/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.ode.inet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import mockit.Capturing;
import mockit.Expectations;

public class InetPointTest {
   
   @Capturing
   DatagramSocket capturingDatagramSocket;
   
   @Capturing
   DatagramPacket capturingDatagramPacket;
   
   @Capturing
   Thread capturingThread;

   @Capturing
   InetAddress capturingInetAddress;

   @Test
   public void testStringConstructorCreatesAddress() {
      try {
         new Expectations() {
            {
               InetAddress.getByName(anyString).getAddress();
            }
         };

         new InetPoint("hostString123", 5, true);
      } catch (UnknownHostException e) {
         fail("Unexpected exception: " + e);
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void testStringConstructorFailsNullAddress() {
      try {
         new Expectations() {
            {
               InetAddress.getByName(anyString).getAddress();
               result = null;
            }
         };
         new InetPoint("hostString123", 5, true);
      } catch (UnknownHostException e) {
         fail("Unexpected exception: " + e);
      }
   }

   @Test
   public void testByteConstructorCreatesAddress() {
      new InetPoint(new byte[] { 1, 2, 3 }, 5, true);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testByteConstructorFailsNullAddress() {
      new InetPoint((byte[]) null, 5, true);
   }

   @Test
   public void testBBytePortConstructorCreatesAddress() {
      new InetPoint(new byte[] { 1, 2, 3 }, 5);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testBytePortConstructorFailsNullAddress() {
      new InetPoint((byte[]) null, 5);
   }

   @Test
   public void getInetAddressCallsGetAddress() {
      try {
         new Expectations() {
            {
               InetAddress.getByAddress((byte[]) any);
            }
         };
         new InetPoint(new byte[] { 1, 2, 3 }, 5).getInetAddress();
      } catch (UnknownHostException e) {
         fail("Unexpected exception: " + e);
      }
   }

   @Test
   public void testAddressLength16IsIpv6() {
      assertTrue(new InetPoint(new byte[16], 5).isIPv6Address());
   }

   @Test
   public void testAddressLength4IsNotIpv6() {
      assertFalse(new InetPoint(new byte[4], 5).isIPv6Address());
   }

   @Test
   public void testToStringMethodIPV6() {
      assertEquals(
            "InetPoint { port = 5 (0x5); address = 00000000000000000000000000000000 (IPv6, null); forward = false }",
            new InetPoint(new byte[16], 5).toString());
   }

   @Test
   public void testToStringException() {
         assertEquals(
               "InetPoint { port = 5 (0x5); address = 00000000000000000000000000000000 (IPv6, null); forward = false }",
               new InetPoint(new byte[16], 5).toString());
   }

   @Test
   public void testToStringMethodIPV4() {
      assertEquals("InetPoint { port = 5 (0x5); address = 00000000 (IPv4, null); forward = false }",
            new InetPoint(new byte[4], 5).toString());
   }

}
