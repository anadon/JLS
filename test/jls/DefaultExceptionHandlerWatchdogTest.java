package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * DefaultExceptionHandler bounded-exit guarantee (issue #208). The
 * interactive branch used to show a modal dialog and only then call
 * System.exit; on an unattended display (the Xvfb UI-CI lane) the modal
 * blocks forever and the process hangs until a wall-clock timeout. These
 * tests drive the handler's termination seams - a blocking dialog stand-in
 * and recording exiters - so the watchdog behaviour is exercised without a
 * real display: a hung dialog must still terminate within a bounded time.
 */
class DefaultExceptionHandlerWatchdogTest {

	@AfterEach
	void removeTraceFile() {
		// saveTrace writes JLSerror in the working directory; don't leave it
		new File("JLSerror").delete();
	}

	/**
	 * P2: a display-present-unattended run - modelled by a dialog that
	 * never returns - must still exit within a bounded time via the
	 * watchdog, instead of hanging (P1's exit-124 failure at 76ebb20).
	 */
	@Test
	void watchdogTerminatesWhenDialogHangs() throws Exception {
		DefaultExceptionHandler handler = new DefaultExceptionHandler();
		handler.interactiveOverride = Boolean.TRUE;
		handler.watchdogMillis = 200L;

		AtomicInteger code = new AtomicInteger(-999);
		AtomicBoolean viaHardExit = new AtomicBoolean(false);
		CountDownLatch exited = new CountDownLatch(1);
		handler.exiter = c -> { code.set(c); exited.countDown(); };
		handler.hardExiter = c -> {
			viaHardExit.set(true);
			code.set(c);
			exited.countDown();
		};

		// a dialog that never returns, exactly as a modal blocks an
		// unattended display; released after the assertions
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean dialogEntered = new AtomicBoolean(false);
		handler.dialogRunner = show -> {
			dialogEntered.set(true);
			try {
				release.await();
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		};

		Thread worker = new Thread(
				() -> handler.uncaughtException(Thread.currentThread(),
						new RuntimeException("boom")),
				"hang-worker");
		worker.setDaemon(true);
		worker.start();

		assertTrue(exited.await(3, TimeUnit.SECONDS),
				"the watchdog must terminate a hung dialog within bounds");
		assertTrue(dialogEntered.get(), "the dialog was shown");
		assertTrue(viaHardExit.get(), "the watchdog uses the hard exiter");
		assertEquals(1, code.get(), "exit is non-zero");
		release.countDown();
	}

	/**
	 * P3: the attended path is unchanged - a dialog that returns (the user
	 * dismissed it) exits immediately through the orderly exiter, well
	 * before the watchdog would fire, and never uses the hard path.
	 */
	@Test
	void dismissedDialogExitsImmediatelyWithoutWatchdog() throws Exception {
		DefaultExceptionHandler handler = new DefaultExceptionHandler();
		handler.interactiveOverride = Boolean.TRUE;
		handler.watchdogMillis = 10_000L;

		AtomicInteger code = new AtomicInteger(-999);
		AtomicBoolean viaHardExit = new AtomicBoolean(false);
		CountDownLatch exited = new CountDownLatch(1);
		handler.exiter = c -> { code.set(c); exited.countDown(); };
		handler.hardExiter = c -> { viaHardExit.set(true); exited.countDown(); };

		AtomicBoolean dialogEntered = new AtomicBoolean(false);
		handler.dialogRunner = show -> dialogEntered.set(true); // returns at once

		handler.uncaughtException(Thread.currentThread(),
				new RuntimeException("boom"));

		assertTrue(exited.await(2, TimeUnit.SECONDS), "exited promptly");
		assertTrue(dialogEntered.get(), "the dialog was shown to the user");
		assertFalse(viaHardExit.get(),
				"a dismissed dialog exits orderly, not via the watchdog");
		assertEquals(1, code.get(), "exit is non-zero");
	}

	/**
	 * P4 guard: the batch/headless path is untouched - it prints, writes
	 * the trace, and exits non-zero with no dialog at all.
	 */
	@Test
	void batchPathExitsWithoutDialog() throws Exception {
		DefaultExceptionHandler handler = new DefaultExceptionHandler();
		handler.interactiveOverride = Boolean.FALSE;

		AtomicInteger code = new AtomicInteger(-999);
		CountDownLatch exited = new CountDownLatch(1);
		handler.exiter = c -> { code.set(c); exited.countDown(); };
		AtomicBoolean dialogEntered = new AtomicBoolean(false);
		handler.dialogRunner = show -> dialogEntered.set(true);

		handler.uncaughtException(Thread.currentThread(),
				new RuntimeException("boom"));

		assertTrue(exited.await(2, TimeUnit.SECONDS), "batch exits");
		assertEquals(1, code.get(), "exit is non-zero");
		assertFalse(dialogEntered.get(), "batch shows no dialog");
		assertTrue(new File("JLSerror").isFile(), "the trace file is written");
	}
}
