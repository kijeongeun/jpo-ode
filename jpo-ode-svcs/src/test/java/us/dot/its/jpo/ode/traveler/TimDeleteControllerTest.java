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
package us.dot.its.jpo.ode.traveler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.springframework.http.HttpStatus;

import mockit.Capturing;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.snmp.SnmpSession;

public class TimDeleteControllerTest {
   
   @Tested
   TimDeleteController testTimDeleteController;
   
   @Injectable
   OdeProperties injectableOdeProperties;
   
   @Capturing
   SnmpSession capturingSnmpSession;
   
   @Mocked
   ResponseEvent mockResponseEvent;
   
   @Test
   public void deleteShouldReturnBadRequestWhenNull() {
      assertEquals(HttpStatus.BAD_REQUEST, testTimDeleteController.deleteTim(null, 42).getStatusCode());
   }

   @Test
   public void deleteShouldCatchSessionIOException() {
      try {
         new Expectations() {
            {
               new SnmpSession((RSU) any);
               result = new IOException("testException123");
            }
         };
      } catch (IOException e) {
         fail("Unexpected Exception in expectations block: " + e);
      }
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }

   @Test
   public void deleteShouldCatchSessionNullPointerException() {
      try {
         new Expectations() {
            {
               new SnmpSession((RSU) any);
               result = new NullPointerException("testException123");
            }
         };
      } catch (IOException e) {
         fail("Unexpected Exception in expectations block: " + e);
      }
      assertEquals(HttpStatus.BAD_REQUEST, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }
   
   @Test
   public void deleteShouldCatchSnmpSetException() throws IOException {
      new Expectations() {{
         capturingSnmpSession.set((PDU) any, (Snmp) any, (UserTarget) any, anyBoolean);
         result = new IOException("testSnmpException123");
      }};
      
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }
   
   @Test
   public void deleteTestTimeout1() throws IOException {
      new Expectations() {{
         capturingSnmpSession.set((PDU) any, (Snmp) any, (UserTarget) any, anyBoolean);
         result = null;
      }};
      
      assertEquals(HttpStatus.REQUEST_TIMEOUT, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }
   
   @Test
   public void deleteTestTimeout2() throws IOException {
      new Expectations() {{
         capturingSnmpSession.set((PDU) any, (Snmp) any, (UserTarget) any, anyBoolean);
         result = mockResponseEvent;
         
         mockResponseEvent.getResponse();
         result = null;
      }};
      
      assertEquals(HttpStatus.REQUEST_TIMEOUT, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }
   
   @Test
   public void deleteTestOK() throws IOException {
      new Expectations() {{
         capturingSnmpSession.set((PDU) any, (Snmp) any, (UserTarget) any, anyBoolean);
         result = mockResponseEvent;
         
         mockResponseEvent.getResponse().getErrorStatus();
         result = 0;
      }};

      //rsuUsername and rsuPassword are null
      assertEquals(HttpStatus.OK, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
      //rsuUsername and rsuPassword are not-null
      assertEquals(HttpStatus.OK, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\", \"rsuUsername\": \"v3user\", \"rsuPassword\": \"password\"}", 42).getStatusCode());
      //rsuUsername and rsuPassword are blank
      assertEquals(HttpStatus.OK, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\", \"rsuUsername\": \"\", \"rsuPassword\": \"\"}", 42).getStatusCode());
   }
   
   @Test
   public void deleteTestMessageAlreadyExists() throws IOException {
      new Expectations() {{
         capturingSnmpSession.set((PDU) any, (Snmp) any, (UserTarget) any, anyBoolean);
         result = mockResponseEvent;
         
         mockResponseEvent.getResponse().getErrorStatus();
         result = 12;
      }};
      
      assertEquals(HttpStatus.BAD_REQUEST, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }
   
   @Test
   public void deleteTestInvalidIndex() throws IOException {
      new Expectations() {{
         capturingSnmpSession.set((PDU) any, (Snmp) any, (UserTarget) any, anyBoolean);
         result = mockResponseEvent;
         
         mockResponseEvent.getResponse().getErrorStatus();
         result = 10;
      }};
      
      assertEquals(HttpStatus.BAD_REQUEST, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }
   
   @Test
   public void deleteTestUnknownErrorCode() throws IOException {
      new Expectations() {{
         capturingSnmpSession.set((PDU) any, (Snmp) any, (UserTarget) any, anyBoolean);
         result = mockResponseEvent;
         
         mockResponseEvent.getResponse().getErrorStatus();
         result = 5;
      }};
      
      assertEquals(HttpStatus.BAD_REQUEST, testTimDeleteController.deleteTim("{\"rsuTarget\":\"127.0.0.1\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}", 42).getStatusCode());
   }

}
