package com.itu.snmp.nms;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * The GUI of Pete's Network Management Server. Displays the SNMP GET and TRAP messages.
 * @author Petri Tilli
 *
 */
public class NMSWindow extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private JTextArea messages;
	
	/**
	 * Constructor, creates the components
	 * @param nms The server instance
	 */
	public NMSWindow(final SnmpNms nms) {
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 
		JLabel emptyLabel = new JLabel("NMS started in: " + nms.getTrapAddress());
        emptyLabel.setPreferredSize(new Dimension(600, 50));
        this.getContentPane().add(emptyLabel, BorderLayout.PAGE_END);

        //implementation of a query button. Not used now.
/*        JButton queryButton = new JButton("GET info from agent");
        queryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nms.makeGetToAgent();
			}
		});
		queryButton.setPreferredSize(new Dimension(600,60));
		this.getContentPane().add(queryButton, BorderLayout.PAGE_START);
*/
		messages = new JTextArea();
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
		JScrollPane sp = new JScrollPane(messages); 
        sp.setPreferredSize(new Dimension(600, 200));
        this.getContentPane().add(sp, BorderLayout.CENTER);
 
        this.setTitle("Pete's Super NMS");
        //Display the window.
        this.pack();
        this.setVisible(true);
	}
	
	/**
	 * Adding a message to the JTextArea.
	 * @param message The message to be added.
	 */
	public void addMessage(String message) {
		messages.insert(message + "\n", messages.getText().length());
	}
	
}
