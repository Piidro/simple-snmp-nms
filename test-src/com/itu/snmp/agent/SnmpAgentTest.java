package com.itu.snmp.agent;

import junit.framework.TestCase;

import org.snmp4j.PDU;
import org.snmp4j.agent.AgentConfigManager;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class SnmpAgentTest extends TestCase {

	private SnmpAgent agent;
	
	public void setUp() throws Exception {
		agent = new SnmpAgent("127.0.0.1/1610", "AGENT_ID");
	}
	
	public void tearDown() {
		agent = null;
	}
	
	public void testCreateTrap() throws Exception {
		PDU trap = agent.createTrap();
		
		assertTrue(trap.getVariable(SnmpConstants.snmpTrapOID).equals(new OID("1.2.3.4.5")));
		assertTrue(trap.getVariable(new OID("1.2.3.4.5.1")).equals(new OctetString("666")));
		assertTrue(trap.getVariable(new OID("1.2.3.4.5.2")).equals(new OctetString("AGENT_ID")));
		assertTrue(trap.getVariable(new OID("1.2.3.4.5.3")).equals(new OctetString("Elevator jammed!")));
		assertTrue(trap.getVariable(new OID("1.2.3.4.5.4")).equals(new OctetString("127.0.0.1/1610")));
	}
	
}
