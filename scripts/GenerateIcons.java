// Generates the JLS application icons checked in under resources/packaging/
// (issue #82).  The project has no standalone logo artwork (the only images
// are tiny per-element toolbar GIFs), so the icon is a simple letterform
// rendered programmatically: "JLS" in DejaVu Sans Bold on a slate-blue
// rounded square, with a signal-trace accent line.
//
// Run from the repository root (any JDK 17+, no build step needed):
//
//     java -Djava.awt.headless=true scripts/GenerateIcons.java
//
// Outputs:
//     resources/packaging/jls.png   256x256 PNG   (Linux jpackage / freedesktop)
//     resources/packaging/jls.ico   ICO container (Windows) with PNG-compressed
//                                   16/32/48/256 entries (valid since Vista)
//     resources/packaging/jls.icns  ICNS container (macOS) with PNG-compressed
//                                   ic07/ic08/ic09 (128/256/512) entries
//
// The ICO and ICNS files are written directly as PNG-bearing containers so no
// external conversion tool (ImageMagick, icotool, iconutil) is required.

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class GenerateIcons {

	private static final int MASTER = 512;

	public static void main(String[] args) throws IOException {
		Path outDir = Path.of(args.length > 0 ? args[0] : "resources/packaging");
		Files.createDirectories(outDir);

		BufferedImage master = render(MASTER);

		// Linux / generic PNG
		ImageIO.write(scale(master, 256), "png", outDir.resolve("jls.png").toFile());

		// Windows ICO (PNG-compressed entries)
		writeIco(outDir.resolve("jls.ico"),
				pngBytes(scale(master, 16)), pngBytes(scale(master, 32)),
				pngBytes(scale(master, 48)), pngBytes(scale(master, 256)));

		// macOS ICNS (PNG-compressed entries: ic07=128, ic08=256, ic09=512)
		writeIcns(outDir.resolve("jls.icns"),
				new String[] { "ic07", "ic08", "ic09" },
				new byte[][] { pngBytes(scale(master, 128)),
						pngBytes(scale(master, 256)), pngBytes(master) });

		System.out.println("wrote jls.png, jls.ico, jls.icns to " + outDir);
	}

	/** Draw the icon at the given square size. */
	private static BufferedImage render(int s) {
		BufferedImage im = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = im.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);

		// slate-blue rounded square
		float arc = s * 0.20f;
		RoundRectangle2D plate =
				new RoundRectangle2D.Float(0, 0, s, s, arc, arc);
		g.setPaint(new GradientPaint(0, 0, new Color(0x2E4A6B),
				0, s, new Color(0x1A2C44)));
		g.fill(plate);

		// subtle inner border
		g.setColor(new Color(255, 255, 255, 28));
		g.setStroke(new BasicStroke(Math.max(1f, s / 128f)));
		float inset = s / 96f;
		g.draw(new RoundRectangle2D.Float(inset, inset,
				s - 2 * inset, s - 2 * inset, arc - inset, arc - inset));

		// "JLS" letterform
		Font font = new Font("DejaVu Sans", Font.BOLD, Math.round(s * 0.34f));
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		String text = "JLS";
		int tx = (s - fm.stringWidth(text)) / 2;
		int ty = Math.round(s * 0.52f) + fm.getAscent() / 2 - fm.getDescent() / 2;
		g.setColor(new Color(0xF2F4F8));
		g.drawString(text, tx, ty);

		// signal-trace accent: a horizontal wire with three connection nodes
		float wy = s * 0.735f;
		float x0 = s * 0.22f;
		float x1 = s * 0.78f;
		g.setColor(new Color(0xD9A441));
		g.setStroke(new BasicStroke(Math.max(1.5f, s / 64f),
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(Math.round(x0), Math.round(wy), Math.round(x1), Math.round(wy));
		float r = Math.max(2f, s / 36f);
		for (float nx : new float[] { x0, (x0 + x1) / 2, x1 }) {
			g.fillOval(Math.round(nx - r), Math.round(wy - r),
					Math.round(2 * r), Math.round(2 * r));
		}

		g.dispose();
		return im;
	}

	/** High-quality downscale by progressive halving. */
	private static BufferedImage scale(BufferedImage src, int size) {
		BufferedImage cur = src;
		while (cur.getWidth() / 2 >= size) {
			cur = resize(cur, cur.getWidth() / 2);
		}
		if (cur.getWidth() != size) {
			cur = resize(cur, size);
		}
		return cur;
	}

	private static BufferedImage resize(BufferedImage src, int size) {
		BufferedImage out = new BufferedImage(size, size,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(src, 0, 0, size, size, null);
		g.dispose();
		return out;
	}

	private static byte[] pngBytes(BufferedImage im) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(im, "png", bos);
		return bos.toByteArray();
	}

	/**
	 * Write a Windows .ico container whose entries are PNG-compressed
	 * (supported since Windows Vista).  Entry sizes are read back from
	 * the PNG IHDR (bytes 16-23, big-endian width/height).
	 */
	private static void writeIco(Path path, byte[]... pngs) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);
		out.writeShort(Short.reverseBytes((short) 0));          // reserved
		out.writeShort(Short.reverseBytes((short) 1));          // type: icon
		out.writeShort(Short.reverseBytes((short) pngs.length)); // count
		int offset = 6 + 16 * pngs.length;
		for (byte[] png : pngs) {
			int w = pngDimension(png, 16);
			int h = pngDimension(png, 20);
			out.writeByte(w >= 256 ? 0 : w); // 0 means 256
			out.writeByte(h >= 256 ? 0 : h);
			out.writeByte(0);                                    // palette
			out.writeByte(0);                                    // reserved
			out.writeShort(Short.reverseBytes((short) 1));       // planes
			out.writeShort(Short.reverseBytes((short) 32));      // bpp
			out.writeInt(Integer.reverseBytes(png.length));
			out.writeInt(Integer.reverseBytes(offset));
			offset += png.length;
		}
		for (byte[] png : pngs) {
			out.write(png);
		}
		Files.write(path, bos.toByteArray());
	}

	private static int pngDimension(byte[] png, int at) {
		return ((png[at] & 0xFF) << 24) | ((png[at + 1] & 0xFF) << 16)
				| ((png[at + 2] & 0xFF) << 8) | (png[at + 3] & 0xFF);
	}

	/** Write a macOS .icns container with PNG-compressed entries. */
	private static void writeIcns(Path path, String[] types, byte[][] pngs)
			throws IOException {
		int total = 8;
		for (byte[] png : pngs) {
			total += 8 + png.length;
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);
		out.writeBytes("icns");
		out.writeInt(total);
		for (int i = 0; i < types.length; i++) {
			out.writeBytes(types[i]);
			out.writeInt(8 + pngs[i].length);
			out.write(pngs[i]);
		}
		Files.write(path, bos.toByteArray());
	}

	private GenerateIcons() {
	}
}
