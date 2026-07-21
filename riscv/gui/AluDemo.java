import java.awt.Component;
import java.awt.Frame;
import jls.elem.Element;
import jls.elem.Put;

/** A GUI-built ALU add (the execute stage: rs1 + immediate), all side
 *  ports, Cin left unconnected -- verifies unconnected-input default and
 *  gives a concrete simulated RISC-V-style computation (5 + 3 = 8). */
public final class AluDemo {
	static String scr = GuiDriver.outDir();
	static GuiDriver g;
	public static void main(String[] a) throws Exception {
		try { run(); } catch (Throwable t) { t.printStackTrace(); }
		finally { System.out.flush(); System.exit(0); }
	}
	static Put out(Element e){for(Put p:e.getAllPuts())if(p instanceof jls.elem.Output)return p;return e.getAllPuts().iterator().next();}
	static Put in(Element e,String n){for(Put p:e.getAllPuts())if(n.equals(p.getName()))return p;throw new RuntimeException("no "+n);}
	static void net(String nm,int b,Put d,Put... s)throws Exception{
		g.placeExact("name a wire",new String[]{nm,String.valueOf(b)},null,d.getX(),d.getY());
		for(Put x:s)g.placeJumpEnd(nm,x.getX(),x.getY());
	}
	static void run() throws Exception {
		Frame f = GuiDriver.boot();
		g = new GuiDriver();
		g.placeFrame(f, 1900, 1180);
		g.menu("File", "New");
		Component fld = g.waitFind(GuiDriver.anyTextField(), "name", 6000);
		g.click(fld); g.type("alu");
		g.click(g.waitFind(GuiDriver.buttonText("OK"), "OK", 4000));
		g.robot.delay(600);
		Element a = g.placeExact("constant value", new String[]{"5"}, new String[]{"10"}, 240, 300);
		Element b = g.placeExact("constant value", new String[]{"3"}, new String[]{"10"}, 240, 540);
		Element alu = g.placeExact("adder", new String[]{"8"}, null, 720, 360);
		Element z = g.placeExact("output pin", new String[]{"s","8"}, null, 1200, 360);
		g.watch(z);
		net("opA", 8, out(a), in(alu, "A"));
		net("opB", 8, out(b), in(alu, "B"));
		net("sum", 8, in(alu, "S"), in(z, "input"));
		System.out.println("attached A="+in(alu,"A").isAttached()+" B="+in(alu,"B").isAttached()
			+" S="+in(alu,"S").isAttached()+" z="+in(z,"input").isAttached()
			+" Cin(unconnected)="+in(alu,"Cin").isAttached());
		try (java.io.PrintWriter w = new java.io.PrintWriter(scr + "alu.jls")) { g.currentCircuit().save(w); }
		System.out.println("saved alu.jls");
		g.shot(scr + "alu.png");
	}
}
