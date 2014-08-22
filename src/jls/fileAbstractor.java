package jls;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

/**
 * 
 * @author Josh Marshall
 * 
 * Here's some code that was used to organize my thoughts.  I'm including it because
 * might one day be useful for fully abstracting file read/writes out of the other 
 * logic.
 *
 */
public class fileAbstractor {

	/**
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	static LinkedList<String> read(String filePath) throws IOException{
		LinkedList<String> toReturn = new LinkedList<String>();
		
		Scanner textInput = null;
		
		if(filePath.endsWith(".jls_xz")){
			textInput = new Scanner(new XZInputStream(new FileInputStream(filePath)));
		}else if(filePath.endsWith(".jls_txt")){
			textInput = new Scanner(new FileInputStream(filePath));
		}else if(filePath.endsWith(".jls")){
			textInput = new Scanner(new ZipInputStream(new FileInputStream(filePath)));
		}else{
			throw new IllegalArgumentException();
		}
		
		textInput.useDelimiter("^[ \t\n\r]*");
		
		while(textInput.hasNext()){
			toReturn.add(textInput.next());
		}
		
		textInput.close();
		
		return toReturn;
	}
	
	/**
	 * 
	 * @param filePath
	 * @param toWrite
	 * @throws IOException
	 */
	static void write(String filePath, LinkedList<String> toWrite) throws IOException{

		OutputStream textOutput = null;
		
		if(filePath.endsWith(".jls_xz")){
			textOutput = new XZOutputStream(new FileOutputStream(filePath), new LZMA2Options());
		}else if(filePath.endsWith(".jls_txt")){
			textOutput = new FileOutputStream(filePath);
		}else if(filePath.endsWith(".jls")){
			textOutput = new ZipOutputStream(new FileOutputStream(filePath));
		}else{
			throw new IllegalArgumentException();
		}
		
		while(!toWrite.isEmpty()){
			textOutput.write(toWrite.pop().concat(" ").getBytes());
		}
		
		textOutput.close();
	}
	
	
}
