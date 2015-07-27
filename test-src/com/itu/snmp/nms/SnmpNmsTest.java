package com.itu.snmp.nms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import junit.framework.TestCase;

import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

public class SnmpNmsTest extends TestCase {

	private SnmpNms server = null;
	
	public void setUp() {
		server = new SnmpNms();
	}
	
	public void tearDown() {
		server = null;
	}
	
	public void testMakeGet() throws Exception {
		assertTrue(true);
		
		File queriesFile = new File("queries.txt");
		queriesFile.delete();
		
		PDU response = new PDU();
		OID oid = new OID(".1.3.6.1.2.1.1.1.0");
		Variable systemDescription = new OctetString("System");
		VariableBinding binding = new VariableBinding(oid, systemDescription);
		
		Vector<VariableBinding> variableBindings = new Vector<VariableBinding>();
		variableBindings.add(binding);
		response.setVariableBindings(variableBindings);

		server.readAndSaveGetResult(response, "AGENT_ID");
		
		assertTrue(queriesFile.exists());

		String fileContent = SnmpNmsTest.readFileAsString("queries.txt");
		assertTrue(fileContent.indexOf("AGENT_ID") > -1);
		assertTrue(fileContent.indexOf("System") > -1);

	}
	
	public void testTrap() throws Exception {
		
		File alarmsFile = new File("alarms.txt");
		alarmsFile.delete();
		
		PDU trap = new PDU();
		trap.setType(PDU.TRAP);
		
		OID oid = new OID("1.2.3.4.5");
		trap.add(new VariableBinding(SnmpConstants.snmpTrapOID, oid));
		trap.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(5000))); // put your uptime here
		trap.add(new VariableBinding(SnmpConstants.sysDescr, new OctetString("System Description"))); 
		
		Variable alarmCode = new OctetString("777");
		Variable agentId = new OctetString("AGENT_ID");
		Variable alarmText = new OctetString("ALARM");
		Variable agentAddress = new OctetString("127.0.0.1/161");
		trap.add(new VariableBinding(new OID("1.2.3.4.5.1"), alarmCode));          
		trap.add(new VariableBinding(new OID("1.2.3.4.5.2"), agentId));          
		trap.add(new VariableBinding(new OID("1.2.3.4.5.3"), alarmText));          
		trap.add(new VariableBinding(new OID("1.2.3.4.5.4"), agentAddress));

		server.readAndSaveTrap(trap);
		
		assertTrue(alarmsFile.exists());
		
		String fileContent = SnmpNmsTest.readFileAsString("alarms.txt");
		assertTrue(fileContent.indexOf("AGENT_ID") > -1);
		assertTrue(fileContent.indexOf("777") > -1);
		assertTrue(fileContent.indexOf("ALARM") > -1);
	}
	
	public void testStart() {
		try {
			server = new SnmpNms("127.0.0.1/1620");
		}
		catch (Exception e) {
			fail("Exception encountered: " + e.toString());
		}
	}
	
	private static String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(
		new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead=0;
		while((numRead=reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}
}
