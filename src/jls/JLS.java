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

		// JLS is GPLv3 (see LICENSE and pop_GPLv3.pdf); the superseded
		// MTU EULA acceptance gate is gone (issue #40)
		JLSStart.parseCommandLine(args);


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
		String[] entries = dirFile.list();
		if (entries == null) {
			// unreadable/nonexistent base directory: no plugins (#46)
			entries = new String[0];
		}
		for (String entry : entries) {
			String fullEntry = base + entry;
			if (entry.endsWith(".xml") && new File(fullEntry).isFile()) {

				// read xml file (by its real path, not CWD-relative, #46)
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				Document doc = null;
				try {
					// manifests need no DTDs or external entities;
					// disallow them (#46)
					dbf.setFeature(
							"http://apache.org/xml/features/disallow-doctype-decl",
							true);
					dbf.setXIncludeAware(false);
					dbf.setExpandEntityReferences(false);
					DocumentBuilder db = dbf.newDocumentBuilder();
					doc = db.parse(new File(fullEntry));
				}
				catch(Exception ex) {
					System.err.println("jls: error: cannot read plugin manifest "
							+ fullEntry + ": " + ex);
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
				url[0] = new File(urlName).toURI().toURL();
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
