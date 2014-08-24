package jls;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Handle end user license agreement approval.
 * 
 * @author David A. Poplawski
 */
public final class Eula extends JDialog implements ActionListener {
	
	// properties
	private static boolean ok = false;
	private JButton accept;
	private JButton deny;

	/**
	 * Check for existing agreement file in user.home.
	 * If none there, then show frame, get ok, and save agreement file.
	 */
	public static boolean accepted() {
		
		String home = System.getProperty("user.home");
		File f = new File(home + "/.jls" + JLSInfo.vers);
		try {
			Scanner input = new Scanner(new FileInputStream(f));
			if (input.hasNextLong()) {
				long code = input.nextLong();
				if (code%17==0 && code%97==0)
					return true;
			}
		}
		catch (FileNotFoundException ex) {
			// nothing to do but continue
		}
		presentEula();
		if (ok) {
			try {
				PrintWriter output = new PrintWriter(new FileOutputStream(f));
				int start = 1000000000;
				int stop = Integer.MAX_VALUE;
				long code = System.currentTimeMillis() % (stop-start) + start;
				while (code%17!=0 || code%97!=0)
					code += 1;
				output.printf("%d %s\n", code, new Date().toString());
				output.close();
			}
			catch (IOException ex) {
				if (GraphicsEnvironment.isHeadless()) {
					System.out.println("Can't create EULA file");
					System.exit(1);
				}
				else {
					JOptionPane.showMessageDialog(null,"Can't create EULA file");
					System.exit(1);
				}
			}
		}
		return ok;
	} // end of check method
	
	private static void presentEula() {
		if(!JLSInfo.batch) {
			try {new Eula((Frame)null,"License Agreement",true);}
			catch (HeadlessException e) {
				presentOnTerminal();
			}
		}
		else presentOnTerminal();
	}
	
	private static void presentOnTerminal() {
		System.out.println("Please read and accept the license agreement below.\n");
		for(String line : lines) {
			System.out.println(line);
		}
		System.out.print("\nDo you accept the terms of the license agreement [Y/n]? ");
		Scanner s = new Scanner(System.in);
		String in = s.nextLine();
		if(in.equalsIgnoreCase("y") || in.length() == 0) ok = true;
	}
	
	/**
	 * Show frame with text in a scrollpane and accept/reject buttons.
	 */
	public Eula(Frame f, String n, boolean m) {
		
		super(f,n,m);
		
		// set up GUI
		Container window = getContentPane();
		window.setLayout(new BorderLayout());
		JTextArea text = new JTextArea(lines.length,60);
		Dimension d = text.getPreferredSize();
		text.setEditable(false);
		text.setFont(new Font("Courier",Font.PLAIN,12));
		for (int i=0; i<lines.length; i+=1) {
			text.append(" "+lines[i].replace("JLSInfo.year",JLSInfo.year+"")+"\n");
		}
		JScrollPane pane = new JScrollPane(text);
		window.add(pane,BorderLayout.CENTER);
		JPanel buttons = new JPanel();
		buttons.setBackground(Color.BLACK);
		buttons.setLayout(new FlowLayout());
		accept = new JButton("AGREE");
		accept.setBackground(Color.GREEN);
		deny = new JButton("DO NOT AGREE");
		deny.setBackground(Color.RED);
		deny.setForeground(Color.WHITE);
		buttons.add(accept);
		buttons.add(deny);
		window.add(buttons,BorderLayout.SOUTH);
		
		// set up listeners
		accept.addActionListener(this);
		deny.addActionListener(this);
		
		// finish up
		setLocation(100,100);
		setSize(d.width,500);
		setVisible(true);
	} // end of constructor
	
	/**
	 * React to button pushes.
	 */
	public void actionPerformed(ActionEvent event) {
		
		ok = false;
		if (event.getSource() == accept) {
			ok = true;
		}
		dispose();
	} // end of actionPerformed method
	
	// agreement text
	private static String [] lines = {
			"LICENSE AGREEMENT",
			"",
			"IMPORTANT NOTICE: YOUR USE OF THE SOFTWARE IS CONDITIONED ON YOUR REVIEW AND",
			"AGREEMENT TO THE TERMS AND CONDITIONS OF THIS LICENSE AGREEMENT. BY CLICKING",
			"\"AGREE\" YOU AGREE TO BE BOUND TO THE TERMS OF THIS AGREEMENT.",
			"",
			"1. DEFINITIONS:",
			"\"Licensor\" means Michigan Technological University. ",
			"\"Licensee\" means You, if you accept the terms of this Agreement. ",
			"\"Authors\" means David A. Poplawski ",
			"\"Software\" means the executable version of the JLS (Java Logic Simulator)",
			"software package, including any subsequent upgrades, updates, or",
			"modifications to JLS provided by Authors or Licensor.",
			"",
			"2. LICENSE GRANT.",
			"Licensor grants to Licensee a non-exclusive, worldwide, royalty-free,",
			"perpetual, non-transferable, single-user license to copy and use the Software",
			"and related documentation for any academic or educational use. This license",
			"does not entitle Licensee to any installation support, technical support,",
			"telephone assistance, or maintenance. This license entitles Licensee to",
			"upgrades, updates, and modifications within the Software only to the extent",
			"they are made available by Licensor from time-to-time at Licensor�s sole",
			"discretion.",
			"",
			"3. RESTRICTIONS.",
			"Except as otherwise expressly permitted in this Agreement, Licensee may not",
			"(i) sell, rent, lease, or sublicense rights in Software; (ii) remove or alter",
			"any trademark, logo, copyright, or other proprietary notices, legends,",
			"symbols, or labels in Software or related documentation; (iii) except as",
			"provided in Article 6, use the name of Licensor or Authors in any manner",
			"related to the Software without their prior written permission; (iv) make any",
			"derivative works based on or incorporating the Software.",
			"",
			"4. TERMINATION.",
			"License and the rights granted hereunder shall terminate automatically if",
			"Licensee breaches any of the terms or conditions of this Agreement, unless",
			"Licensee receives a prior written waiver of such breach from Licensor. Upon",
			"termination, Licensee shall destroy all copies of the Software and related",
			"documentation, including copies made for backup purposes.",
			"",
			"5. PROPRIETARY RIGHTS.",
			"The Software and related documentation constitute published works and are",
			"protected by copyright and other intellectual property laws and by",
			"international treaties. All rights, title to, and ownership interest in the",
			"Software, including all intellectual property rights therein, belong to and",
			"shall remain with Licensor. Licensee acknowledges such ownership and",
			"intellectual property rights and agrees not to take any action that",
			"jeopardizes, limits, undermines, or in any manner interferes with Licensor's",
			"ownership and intellectual property rights with respect to the Software.",
			"",
			"6. MANDATORY NOTICE.",
			"Both the notice below and the full terms of this Agreement shall be embedded",
			"in any location or medium in which the Software or related documentation is",
			"stored, copied, or reproduced, and shall be loaded into computer memory for",
			"use, display, or reproduction in any copy of Software or related",
			"documentation. The notice to accompany the full terms of this Agreement shall",
			"state: ",
			"\"JLS  is licensed from Michigan Technological University.",
			"Copyright � JLSInfo.year by David A. Poplawski.",
			"All rights reserved.\" ",
			"",
			"7. DISCLAIMER OF WARRANTY.",
			"THE SOFTWARE IS PROVIDED IN ACCORDANCE WITH THE TERMS AND CONDITIONS OF THIS",
			"AGREEMENT ON AN \"AS IS\" BASIS. LICENSOR MAKES NO REPRESENTATIONS OF AND",
			"SPECIFICALLY DISCLAIMS WARRANTIES OF ANY KIND, EXPRESS, IMPLIED, STATUTORY,",
			"OR OTHERWISE, INCLUDING BUT NOT LIMITED TO WARRANTIES THAT THE SOFTWARE IS",
			"MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE, NON-INFRINGING, ACCURATE, OR FREE",
			"FROM DEFECTS, WHETHER DISCOVERABLE OR NOT. LICENSEE BEARS THE ENTIRE RISK AS",
			"TO THE QUALITY AND PERFORMANCE OF THE SOFTWARE. SHOULD THE SOFTWARE PROVE",
			"DEFECTIVE IN ANY RESPECT, LICENSEE ASSUMES SOLE RESPONSIBILITY AND LIABILITY",
			"FOR THE ENTIRE COST OF ANY SERVICE AND REPAIR IN CONNECTION THEREWITH. THIS",
			"DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS AGREEMENT. NO",
			"USE OF THE SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER. ",
			"",
			"8. LIMITATION OF LIABILITY.",
			"IN NO EVENT AND UNDER NO CIRCUMSTANCES WILL LICENSOR BE LIABLE TO ANY PARTY",
			"FOR DIRECT, INDIRECT, CONSEQUENTIAL, SPECIAL, INCIDENTAL, PUNITIVE, OR",
			"EXEMPLARY DAMAGES OF ANY KIND WHATSOEVER ARISING OUT OF THE USE OF OR",
			"INABILITY TO USE ANY PORTION OF THE SOFTWARE, INCLUDING BUT NOT LIMITED TO",
			"DAMAGES FOR LOSS OF GOODWILL, WORK STOPPAGE, COMPUTER FAILURE OR MALFUNCTION,",
			"OR ANY AND ALL OTHER DAMAGES OR LOSSES, EVEN IF ADVISED OF THE POSSIBILITY",
			"THEREOF, AND WITHOUT REGARD TO WHETHER SUCH CLAIM OR ALLEGATION IS BASED IN",
			"CONTRACT, TORT, OR ANY OTHER LEGAL OR EQUITABLE THEORY. TO THE EXTENT",
			"PERMITTED UNDER APPLICABLE LAW, LICENSOR'S ENTIRE LIABILITY UNDER ANY",
			"PROVISION OF THIS AGREEMENT SHALL NOT EXCEED IN THE AGGREGATE THE SUM OF THE",
			"FEES LICENSEE PAID FOR THIS LICENSE (IF ANY). ",
			"",
			"9. MISCELLANEOUS.",
			"(a) This Agreement constitutes the entire Agreement between the parties",
			"concerning the subject matter hereof. (b) This Agreement may be amended only",
			"by mutual written agreement, signed by both parties. (c) Except to the extent",
			"applicable law, if any, provides otherwise, this Agreement shall be governed",
			"by the laws of the State of Michigan, U.S.A., excluding its conflict of law",
			"provisions, and the parties choose the state and federal courts within the",
			"State of Michigan as the sole venue for disputes involving this Agreement.",
			"(d) This Agreement shall not be governed by the United Nations Convention on",
			"Contracts for the International Sale of Goods. (e) If any provision in this",
			"Agreement should be held invalid or unenforceable by a court having",
			"jurisdiction, such provision shall be modified to the minimum extent",
			"necessary to render it enforceable without losing its intent, or severed from",
			"this Agreement if no such modification is possible, and other provisions of",
			"this Agreement shall remain in full force and effect. (f) A waiver by either",
			"party of any term or condition of this Agreement or any breach thereof, in",
			"any one instance, shall not waive such term or condition or any subsequent",
			"breach thereof. (g) The provisions of this Agreement which require or",
			"contemplate performance after the termination of this Agreement shall be",
			"enforceable notwithstanding said termination. (h) Licensee may not assign or",
			"otherwise transfer by operation of law or otherwise this Agreement or any",
			"rights or obligations herein. (i) This Agreement shall be binding upon and",
			"shall inure to the benefit of the parties, their successors, and permitted",
			"assigns. (j) The relationship between Licensor and Licensee is that of",
			"independent contractors and neither Licensee nor its agents shall have any",
			"authority to bind Licensor in any way. (k) The headings to the sections of",
			"this Agreement are used for convenience only and shall have no substantive",
			"meaning."
	};
	
} // end of Eula class
