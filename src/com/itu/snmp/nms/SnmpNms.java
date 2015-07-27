package com.itu.snmp.nms;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * The main class of the Network Management Server. This server will listen to
 * agents sending traps. Once traps are received, it will:
 * 1. Make an SNMP GET to the agent sending the trap.
 * 2. Write the resulting message to Database.
 * 
 * @author Petri Tilli
 *
 */
public class SnmpNms {

	private static final Logger logger = Logger.getLogger(SnmpNms.class);
	private String trapAddress = "127.0.0.1/1620";
	private NMSWindow gui;
	
	private String receivedAgentAddress;
	private String receivedAgentId;
	
	/**
	 * Default constructor
	 */
	public SnmpNms() {
		//doing nothing really
	}
	
	/**
	 * The constructor which starts the listening of traps and also starts the GUI.
	 * @param address The address in which this server is listening for traps.
	 * @throws IOException
	 */
	public SnmpNms(String address) throws IOException {
		
		setTrapAddress(address);		

    	BasicConfigurator.configure();

		logger.info("Starting...");
		
		// configure Snmp object
		UdpAddress listenAddress = new UdpAddress(trapAddress);
		DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping(listenAddress);
		Snmp snmp = new Snmp(transport);

		logger.info("Listening to traps in " + trapAddress);

		CommandResponder trapListener = new CommandResponder() {
		  public synchronized void processPdu(CommandResponderEvent e) {
		    PDU trap = e.getPDU();
		    if (trap != null) {
		    	readAndSaveTrap(trap);
				makeGetToAgent(receivedAgentAddress, receivedAgentId);
		    }
		  }
		};
		snmp.addCommandResponder(trapListener);

		transport.listen();		
		
		//start the UI:
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	showGUI();
            }
        });

		logger.info("Started.");
	}
	
	/**
	 * Starts the NMSWindow UI
	 */
	private void showGUI() {
		this.gui = new NMSWindow(this);
	}
	
	/**
	 * Starts the App.
	 * @param args no arguments currently handled
	 * @throws java.io.IOException
	 */
	public static void main(String args[]) throws java.io.IOException {		
		
		//Here we could read for instance trap listening port from arguments.
		//Currently, it is hard-coded.
		new SnmpNms("127.0.0.1/1620");
	}
	
	/**
	 * Makes an SNMP GET to agent
	 * @param agentAddress agent's address in form of url/port, i.e. 127.0.0.1/161
	 */
	public void makeGetToAgent(String agentAddress, String agentId) {
		
		logger.info("Making GET!");

		try {
			UdpAddress targetAddress = new UdpAddress(agentAddress);
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString("public"));
			target.setAddress(targetAddress);
			target.setRetries(2);
			target.setTimeout(10000);
			target.setVersion(SnmpConstants.version2c);
			
			TransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();
	
		    PDU pdu = new PDU();
		    pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.1.1.0")));
		    pdu.setType(PDU.GET); 
		    Snmp snmp = new Snmp(transport);

			ResponseEvent responseEvent = snmp.get(pdu, target);
			if (responseEvent != null) {
				//got a valid response!
				readAndSaveGetResult(responseEvent.getResponse(), agentId);
			}
			else {
				logger.error("Get returned a null response.");
			}
		}
		catch (IOException e) {
			logger.error("Error: " + e.toString());
		}
	}

	/**
	 * Here we are reading the SNMP trap contents and saving it to DB.
	 * Then calling the SNMP GET with this info.
	 * 
	 * @param trap The SNMP trap received from agent
	 */
	public void readAndSaveTrap(PDU trap) {
		String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date());
		Variable alarmCode = trap.getVariable(new OID("1.2.3.4.5.1"));
		Variable agentId = trap.getVariable(new OID("1.2.3.4.5.2"));
		Variable alarmText = trap.getVariable(new OID("1.2.3.4.5.3"));
		Variable agentAddress = trap.getVariable(new OID("1.2.3.4.5.4"));
		String message = alarmCode.toString() + ":" + alarmText.toString();
		if (gui != null) {
			gui.addMessage(date + " TRAP from agent " + agentId.toString() +"("+ agentAddress.toString() +") "+ message);
		}
		saveTrapToFile(date + "," + agentId.toString() + "," + message);
		
		receivedAgentAddress = agentAddress.toString();
		receivedAgentId = agentId.toString();
	}

	/**
	 * Here we are reading the result and finding the correct OID from there.
	 * Once found, saving it to DB.
	 * 
	 * @param responseEvent Response from agent
	 * @param agentId The agent who was queried
	 */
	public void readAndSaveGetResult(PDU respPDU, String agentId) {
		Vector<? extends VariableBinding> varVector = respPDU.getVariableBindings();
		logger.info(varVector.toString());
		String response = "";
		for (VariableBinding binding : varVector) {
			if (binding.getOid().equals(new OID(".1.3.6.1.2.1.1.1.0"))) {
				response = binding.getVariable().toString();
			}
		}
		String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date());
		if (gui != null) {
			gui.addMessage(date + " GET response from agent " + response);
		}
		saveGetToFile(date + ","+agentId+"," + response);
	}

	/**
	 * Saving the GET message to a file.
	 * @param line message to save
	 */
	private void saveGetToFile(String line) {
		PrintWriter out = null;
		try {
		    out = new PrintWriter(new BufferedWriter(new FileWriter("queries.txt", true)));
		    out.print(line);
		}
		catch (Exception e) {
			logger.error("Not able to write queries.txt: " + e.toString());
		}
		finally {
		    if (out != null) out.close();
		}		
	}

	/**
	 * Saving the alarm (trap) to a file.
	 * @param line message to save
	 */
	private void saveTrapToFile(String line) {
		PrintWriter out = null;
		try {
		    out = new PrintWriter(new BufferedWriter(new FileWriter("alarms.txt", true)));
		    out.print(line);
		}
		catch (Exception e) {
			logger.error("Not able to write alarms.txt: " + e.toString());
		}
		finally {
		    if (out != null) out.close();
		}		
	}

	public String getTrapAddress() {
		return trapAddress;
	}
	
	public void setTrapAddress(String address) {
		this.trapAddress = address;
	}
}
