package com.itu.snmp.agent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * The GUI Pete's Secret Agent. User can send a trap with the GUI and GUI displays a log
 * of sent TRAP messages.
 * @author Petri Tilli
 *
 */
public class AgentWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JTextArea messages;
	
	/**
	 * Constructor, creates the components
	 * @param agent The agent instance
	 */
	public AgentWindow(final SnmpAgent agent) {
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 
		JLabel statusLabel = new JLabel("Agent started: " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " ID: " + agent.getAgentId() + " in " + agent.getAddress());
        statusLabel.setPreferredSize(new Dimension(500, 50));
        this.getContentPane().add(statusLabel, BorderLayout.PAGE_END);

        JButton queryButton = new JButton("Send SNMP trap to NMS");
        queryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				agent.sendTrap();
			}
		});
		queryButton.setPreferredSize(new Dimension(500,60));
		this.getContentPane().add(queryButton, BorderLayout.PAGE_START);

		messages = new JTextArea();
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
		JScrollPane sp = new JScrollPane(messages); 
        sp.setPreferredSize(new Dimension(500, 200));
        this.getContentPane().add(sp, BorderLayout.CENTER);
 
        this.setTitle("Pete's Secret Agent");
        //Display the window.
        this.pack();
        this.setVisible(true);
	}

	/**
	 * Adds a message to JTextArea.
	 * @param message The message to be added.
	 */
	public void addMessage(String message) {
		messages.insert(message + "\n", messages.getText().length());
	}

}
