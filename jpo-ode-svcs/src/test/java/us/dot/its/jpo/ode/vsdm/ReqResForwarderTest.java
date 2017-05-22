package us.dot.its.jpo.ode.vsdm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.asn1.j2735.CVSampleMessageBuilder;
import us.dot.its.jpo.ode.asn1.j2735.J2735Util;
import us.dot.its.jpo.ode.j2735.semi.ConnectionPoint;
import us.dot.its.jpo.ode.j2735.semi.IPv4Address;
import us.dot.its.jpo.ode.j2735.semi.IpAddress;
import us.dot.its.jpo.ode.j2735.semi.PortNumber;
import us.dot.its.jpo.ode.j2735.semi.ServiceRequest;

@RunWith(JMockit.class)
public class ReqResForwarderTest {

	@Injectable
	OdeProperties mockOdeProperties;

	@Mocked
	DatagramSocket mockDatagramSocket;
	
	ReqResForwarder forwarder;
	
	@Before
	public void setUp() throws Exception {
		String obuIp = "1.1.1.1";
		int obuPort = 12321;
		int forwarderPort = 5555;

		new Expectations() {
			{	
				mockOdeProperties.getReturnIp();
				result = "3.3.3.3";

				mockOdeProperties.getReturnPort();
				result = forwarderPort;

				mockOdeProperties.getForwarderPort();
				result = forwarderPort;
			}
		};
		
		ServiceRequest req = CVSampleMessageBuilder.buildVehicleSituationDataServiceRequest();
		ConnectionPoint newReturnAddr = new ConnectionPoint();
		newReturnAddr.setPort(new PortNumber(7777));
		req.setDestination(newReturnAddr);
		//ServiceRequest req = CVSampleMessageBuilder.buildVehicleSituationDataServiceRequest("4.4.4.4", 12345);

		forwarder = new ReqResForwarder(mockOdeProperties, req, obuIp, obuPort);
		String expectedHexString = "8000000000002020203018181818ad98";
		byte[] payload = forwarder.getPayload();
		assertEquals(expectedHexString, Hex.encodeHexString(payload));
	}

	@After
	public void tearDown() throws Exception {
		forwarder = null;
		System.out.println("Testing ended");
	}

	@Test @Ignore
	public void testSend() throws IOException {
		new Expectations() {
			{	
				mockOdeProperties.getSdcIp();
				result = "127.0.0.1";

				mockOdeProperties.getSdcPort();
				result = 12345;
				
				mockDatagramSocket.send((DatagramPacket)any);
			}
		};
		forwarder.send();
	}
	
	@Test @Ignore
	public void testReceive() throws IOException {
		new Expectations() {
			{
				mockDatagramSocket.receive((DatagramPacket)any);
			}
		};
		forwarder.receiveVsdServiceResponse();
	}
	
	@Test
	public void testTest() throws IOException {
	}

}
