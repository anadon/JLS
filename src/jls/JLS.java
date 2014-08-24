package jls;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Main JLS class.
 * Sets up exception handler and EULA, then finds plugins (if any), 
 * then has JLSStart do the rest of the work.
 * 
 * @author David A. Poplawski, Nick Lanam (test)
 */
public final class JLS  {

	/**
	 * Set up exception handler and EULA agreement, then start up JLS.
	 * 
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {

		// set handler for unexpected exceptions
		DefaultExceptionHandler exHandler = new DefaultExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(exHandler);

		// check for EULA agreement
		JLSStart.parseCommandLine(args);
		if (!Eula.accepted()) {
			System.exit(1);
		}
		
		// look for JLS.jar...
		// first, get all paths in classpath
		String fsep = System.getProperty("file.separator");
		String d = System.getProperty("java.class.path");
		String psep = System.getProperty("path.separator");
		Vector<String> paths = new Vector<String>();
		int from = 0;
		int pos = d.indexOf(psep,from);
		while (pos != -1) {
			paths.add(d.substring(from,pos));
			from = pos + 1;
			pos = d.indexOf(psep,from);
		}
		paths.add(d.substring(from));

		// now look for JLS.jar in some path, putting directory prefix in base
		String base = "."+fsep;
		boolean jlsFound = false;
		for (String p : paths) {
			if (p.equals("JLS.jar")) {
				jlsFound = true;
				break;
			}
			if (p.endsWith("/JLS.jar") || p.endsWith("\\JLS.jar")) {
				// annoying file separator difference between platforms!!!
				base = p.replace("JLS.jar", "");
				if (fsep.equals("/")) {
					base = base.replaceAll("\\\\","/");
				}
				else {
					base = base.replaceAll("/","\\\\");
				}
				jlsFound = true;
				break;
			}
		}

		// if JLS.jar is not found,
		if (!jlsFound) {

			// start JLS in my eclipse development environment
			JLSStart.start(args,exHandler);
			return;
		}

		// look for manifest (.xml) files
		File dirFile = new File(base);
		String urlName = null;
		String cl = null;
		for (String entry : dirFile.list()) {
			String fullEntry = base + entry;
			if (entry.endsWith(".xml") && new File(fullEntry).isFile()) {

				// read xml file
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				Document doc = null;
				try {
					DocumentBuilder db = dbf.newDocumentBuilder();
					doc = db.parse(entry);
				}
				catch(Exception ex) {
					System.out.println(ex);
					System.exit(1);
				}
				Element docEle = doc.getDocumentElement();
				String author = get(docEle, "Author");
				String affil = get(docEle, "Affiliation");
				String contact = get(docEle, "ContactInfo");
				cl = get(docEle, "Class");
				if (author == null || affil == null || contact == null || cl == null) {
					break;
				}
				cl = cl.trim();
				String jar = fullEntry.replaceFirst("\\.xml$", "") + ".jar";
				if (new File(jar).exists()) {
					urlName = new File(jar).getAbsolutePath();
				}
				else {
					urlName = new File(base).getAbsolutePath() + "/";
				}
				break;
			}
		}

		// if no plugins found, start JLS normally
		if (urlName == null)
			JLSStart.start(args,exHandler);

		// otherwise set up new classloader and run specified config
		else {
			try {
				URL [] url = new URL[1];
				url[0] = new URL("file:" + urlName);
				@SuppressWarnings({ "resource", "unchecked" })
				Class<Element> newElement = (Class<Element>) new URLClassLoader(url).loadClass(cl);
				Method[] methods = newElement.getMethods();
				for (Method m : methods) {
					if (m.getName().equals("config")) {
						m.invoke(null, args, exHandler);
						break;
					}
				}
			}
			catch (Exception ex) {
				System.out.println(ex);
				System.exit(1);
			}
		}

	} // end of main method

	/**
	 * Get the value of a given XML field.
	 * 
	 * @param docEle A document element.
	 * @param name The name of the field.
	 * 
	 * @return the value for the given name.
	 */
	private static String get(Element docEle, String name) {
		
		NodeList nl = docEle.getElementsByTagName(name);
		if(nl != null && nl.getLength() == 1) {
			return ((Element)nl.item(0)).getTextContent();
		}
		else {
			return null;
		}
	} // end of get method

} // end of JLS class
