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

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy;
import us.dot.its.jpo.ode.model.OdeMsgPayload;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.model.OdeRequestMsgMetadata;
import us.dot.its.jpo.ode.model.OdeTimData;
import us.dot.its.jpo.ode.model.OdeTravelerInputData;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.plugin.ServiceRequest.OdeInternal;
import us.dot.its.jpo.ode.plugin.ServiceRequest.OdeInternal.RequestVerb;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.util.DateTimeUtils;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.wrapper.MessageProducer;
import us.dot.its.jpo.ode.wrapper.serdes.OdeTimSerializer;

@RestController
public class TimDepositController {
   
   private static final Logger logger = LoggerFactory.getLogger(TimDepositController.class);

   public static class TimDepositControllerException extends Exception {

      private static final long serialVersionUID = 1L;

      public TimDepositControllerException(String errMsg) {
         super(errMsg);
      }

      public TimDepositControllerException(String errMsg, Exception e) {
         super(errMsg, e);
      }
   }
   
   public static final String TIM_BASE_POJO_TOPIC_NAME = "topic.OdeTimBasicPojo";
   public static final String RSUS_STRING = "rsus";
   public static final String REQUEST_STRING = "request";

   private static final String ERRSTR = "error";
   private static final String WARNING = "warning";
   private static final String SUCCESS = "success";

   private MessageProducer<String, OdeObject> timProducer;

   @Autowired
   public TimDepositController(OdeProperties odeProperties) {
      super();
      this.timProducer = new MessageProducer<>(odeProperties.getKafkaBrokers(), odeProperties.getKafkaProducerType(),
            null, OdeTimSerializer.class.getName(), odeProperties.getKafkaTopicsDisabledSet());
   }

   /**
    * Send a TIM with the appropriate deposit type, ODE.PUT or ODE.POST
    *
    * @param jsonString
    * @param verb
    * @return
    */
   public ResponseEntity<String> depositTim(String jsonString, RequestVerb verb) {
         // Check empty
         if (null == jsonString || jsonString.isEmpty()) {
            String errMsg = "Empty request.";
            logger.error(errMsg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
         }

         OdeTravelerInputData odeTID = null;
         ServiceRequest request;
         try {
            // Convert JSON to POJO
            odeTID = (OdeTravelerInputData) JsonUtils.fromJson(jsonString, OdeTravelerInputData.class);
            request = odeTID.getRequest();
            if (request == null) {
               throw new TimDepositControllerException("request element is required as of version 3");
            }
            if (request.getOde() != null) {
               throw new TimDepositControllerException("Request.getOde() == "+ request.getOde().getVersion() + ", verb == " + request.getOde().getVerb());
            } else {
               request.setOde(new OdeInternal());
            }

            request.getOde().setVerb(verb);

            logger.debug("OdeTravelerInputData: {}", jsonString);

         } catch (TimDepositControllerException e) {
            String errMsg = "Missing or invalid argument: " + e.getMessage();
            logger.error(errMsg, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
         } catch (Exception e) {
            String errMsg = "Malformed or non-compliant JSON.";
            logger.error(errMsg, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
         }

         // Add metadata to message and publish to kafka
         OdeTravelerInformationMessage tim = odeTID.getTim();
         OdeMsgPayload timDataPayload = new OdeMsgPayload(tim);
         OdeRequestMsgMetadata timMetadata = new OdeRequestMsgMetadata(timDataPayload, request);

         // Setting the SerialId to OdeBradcastTim serialId to be changed to
         // J2735BroadcastTim serialId after the message has been published to
         // OdeTimBrodcast topic
         timMetadata.setSerialId(serialIdOde);
         timMetadata.setRecordGeneratedBy(GeneratedBy.TMC);

         try {
            timMetadata.setRecordGeneratedAt(DateTimeUtils.isoDateTime(DateTimeUtils.isoDateTime(tim.getTimeStamp())));
         } catch (ParseException e) {
            String errMsg = "Invalid timestamp in tim record: " + tim.getTimeStamp();
            logger.error(errMsg, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
         }

         OdeTimData odeTimData = new OdeTimData(timMetadata, timDataPayload);
         timProducer.send(odeProperties.getKafkaTopicOdeTimBroadcastPojo(), null, odeTimData);

         String obfuscatedTimData = obfuscateRsuPassword(odeTimData.toJson());
         stringMsgProducer.send(odeProperties.getKafkaTopicOdeTimBroadcastJson(), null, obfuscatedTimData);
   }

   /**
    * Update an already-deposited TIM (REST PUT)
    *
    * @param jsonString TIM in JSON
    * @return HTTP 200 on success or error message
    */
   @PutMapping(value = "/tim", produces = "application/json")
   @CrossOrigin
   public ResponseEntity<String> updateTim(@RequestBody String jsonString) {
      return depositTim(jsonString, ServiceRequest.OdeInternal.RequestVerb.PUT);
   }

   /**
    * Deposit a new TIM (REST POST)
    *
    * @param jsonString TIM in JSON
    * @return HTTP 200 on success or error message
    */
   @PostMapping(value = "/tim", produces = "application/json")
   @CrossOrigin
   public ResponseEntity<String> postTim(@RequestBody String jsonString) {
      return depositTim(jsonString, ServiceRequest.OdeInternal.RequestVerb.POST);
   }
}
