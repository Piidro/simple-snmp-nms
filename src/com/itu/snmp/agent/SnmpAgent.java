package com.itu.snmp.agent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.AgentConfigManager;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * A very simple SNMP Agent implementation utilizing SNMP4J, which will send 
 * a trap to the NMS and respond to System Description GET with a hard-coded value.
 * 
 * @author Petri Tilli
 *
 */
public class SnmpAgent extends BaseAgent {

	private static final Logger logger = Logger.getLogger(SnmpAgent.class);
	private String address;
	private String agentId;
	private String trapSendingAddress;
	private AgentWindow gui;

	/**
	 * Constructor 
	 * @param address
	 * @param agentId
	 * @throws IOException
	 */
	public SnmpAgent(String address, String agentId) throws IOException {

        super(new File("simplest.boot"), null, new CommandProcessor(new OctetString("simplest")));
        this.address = address;
        this.agentId = agentId;
        this.trapSendingAddress = "127.0.0.1/1620";

        //Some SNMP4J stuff:
        init();
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
        
        //Unregistering the SNMPv2MIB, because we are overriding it:
		this.unregisterManagedObject(this.getSnmpv2MIB());
		
		//Setting the System Description OID with our own value:
		MOScalar<OctetString> scalar1 = new MOScalar<OctetString>(
				new OID(".1.3.6.1.2.1.1.1.0"), 
				MOAccessImpl.ACCESS_READ_CREATE, 
				new OctetString(agentId + " says Hello."));
		
		//Registering the ManagedObject:
		this.registerManagedObject(scalar1);

		//start the UI:
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	showGUI();
            }
        });

		logger.info("Agent started: " + getServer().getRegistry().toString());
    }

	/**
	 * Unregisters a managed object 
	 * @param moGroup
	 */
	public void unregisterManagedObject(MOGroup moGroup) {
		moGroup.unregisterMOs(server, getContext(moGroup));
	}

	/**
	 * Registers a managed object
	 * @param mo
	 */
	public void registerManagedObject(ManagedObject mo) {
		try {
			server.register(mo, null);
		} catch (DuplicateRegistrationException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Sends a trap with OID 1.2.3.4.5 Contains alarm code, alarm text, agent ID and agent's address
	 */
	public void sendTrap() {
		// Create PDU           
		PDU trap = createTrap();
		
		// Specify receiver
		Address targetaddress = new UdpAddress(trapSendingAddress);
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("public"));
		target.setVersion(SnmpConstants.version2c);
		target.setAddress(targetaddress);
		
		try {
			// Send
			logger.info("Sending...");
			Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
			snmp.send(trap, target, null, null);
			String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(new Date());
			if (gui != null) {
				gui.addMessage(date + " a trap sent to " + this.trapSendingAddress);
			}
		} 
		catch (Exception e) {
			logger.error("Could not send trap. " + e.toString());
		}
	}
	
	/**
	 * Creates the trap and its payload.
	 * @return Complete trap ready to send
	 */
	public PDU createTrap() {
		PDU trap = new PDU();
		trap.setType(PDU.TRAP);
		
		OID oid = new OID("1.2.3.4.5");
		trap.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
		trap.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
		trap.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description"))); 
		
		Variable alarmCode = new OctetString("666");
		Variable agentId = new OctetString(this.agentId);
		Variable alarmText = new OctetString("Elevator jammed!");
		Variable agentAddress = new OctetString(this.address);
		trap.add(new VariableBinding(new OID("1.2.3.4.5.1"), alarmCode));          
		trap.add(new VariableBinding(new OID("1.2.3.4.5.2"), agentId));          
		trap.add(new VariableBinding(new OID("1.2.3.4.5.3"), alarmText));          
		trap.add(new VariableBinding(new OID("1.2.3.4.5.4"), agentAddress));
		
		return trap;
	}

	@Override
	protected void registerManagedObjects() {}

	@Override
	protected void unregisterManagedObjects() {}

	@Override
	protected void addUsmUser(USM usm) {}

	@Override
	protected void addNotificationTargets(SnmpTargetMIB targetMIB, SnmpNotificationMIB notificationMIB) {}

	/**
	 * Init Transport mappings, SNMP4J stuff
	 */
    @Override
    protected void initTransportMappings() throws IOException {
        transportMappings = new TransportMapping[1];
		UdpAddress udpAddress = new UdpAddress(address);		
		DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping(udpAddress);
        transportMappings[0] = transport;
    }

    /**
     * Add views, SNMP4J stuff
     */
	@Override
	protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(
		        "cpublic"), new OctetString("v1v2group"),
		        StorageType.nonVolatile);

		vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
		        SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
		        MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
		        new OctetString("fullWriteView"), new OctetString(
		        "fullNotifyView"), StorageType.nonVolatile);
		
		vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
		        new OctetString(), VacmMIB.vacmViewIncluded,
		        StorageType.nonVolatile);
	}

	/**
	 * Add communities, SNMP4J stuff
	 */
	@Override
	protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[]{
            new OctetString("public"), // community name
            new OctetString("cpublic"), // security name
            getAgent().getContextEngineID(), // local engine ID
            new OctetString("public"), // default context name
            new OctetString(), // transport tag
            new Integer32(StorageType.nonVolatile), // storage type
            new Integer32(RowStatus.active) // row status
        };
        MOTableRow row = communityMIB.getSnmpCommunityEntry().createRow(
                new OctetString("public2public").toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow(row);
	}

	/**
	 * @param args 1st: port, 2nd: agentId
	 */
	public static void main(String[] args) throws IOException {

    	BasicConfigurator.configure();
		logger.info("Starting the agent...");
		
		String port = args[0];
		String agentId = args[1];

		new SnmpAgent("127.0.0.1/" + port, agentId);
	}

	/**
	 * Show Agent's GUI
	 */
	private void showGUI() {
		this.gui = new AgentWindow(this);
	}
	
	public String getAddress() {
		return address;
	}

	public String getAgentId() {
		return agentId;
	}

}
