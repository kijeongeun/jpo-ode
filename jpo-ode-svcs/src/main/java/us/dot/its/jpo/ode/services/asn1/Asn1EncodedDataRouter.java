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
package us.dot.its.jpo.ode.services.asn1;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.eventlog.EventLogger;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.services.asn1.Asn1CommandManager.Asn1CommandManagerException;
import us.dot.its.jpo.ode.traveler.TimDepositController;
import us.dot.its.jpo.ode.traveler.TimMessageManipulator;
import us.dot.its.jpo.ode.util.CodecUtils;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.wrapper.AbstractSubscriberProcessor;
import us.dot.its.jpo.ode.wrapper.MessageProducer;

public class Asn1EncodedDataRouter extends AbstractSubscriberProcessor<String, String> {

   private static final String BYTES = "bytes";

  private static final String MESSAGE_FRAME = "MessageFrame";

  private static final String ERROR_ON_DDS_DEPOSIT = "Error on DDS deposit.";

  public static class Asn1EncodedDataRouterException extends Exception {

      private static final long serialVersionUID = 1L;

      public Asn1EncodedDataRouterException(String string) {
         super(string);
      }

   }

   private static final Logger logger = LoggerFactory.getLogger(Asn1EncodedDataRouter.class);

   private OdeProperties odeProperties;
   private MessageProducer<String, String> stringMsgProducer;
   private Asn1CommandManager asn1CommandManager;

   private TimMessageManipulator timMessageManipulator;

   public Asn1EncodedDataRouter(OdeProperties odeProperties, TimMessageManipulator timMessageManipulator) {
      super();

      this.odeProperties = odeProperties;
      this.timMessageManipulator = timMessageManipulator;

      this.stringMsgProducer = MessageProducer.defaultStringMessageProducer(odeProperties.getKafkaBrokers(),
            odeProperties.getKafkaProducerType(), this.odeProperties.getKafkaTopicsDisabledSet());

      this.asn1CommandManager = new Asn1CommandManager(odeProperties);

   }

   @Override
   public Object process(String consumedData) {
      try {
         logger.debug("Consumed: {}", consumedData);
         JSONObject consumedObj = XmlUtils.toJSONObject(consumedData).getJSONObject(OdeAsn1Data.class.getSimpleName());

         /*
          * When receiving the 'rsus' in xml, since there is only one 'rsu' and
          * there is no construct for array in xml, the rsus does not translate
          * to an array of 1 element. The following workaround, resolves this
          * issue.
          */
         JSONObject metadata = consumedObj.getJSONObject(AppContext.METADATA_STRING);

         if (metadata.has(TimDepositController.REQUEST_STRING)) {
            JSONObject request = metadata.getJSONObject(TimDepositController.REQUEST_STRING);

            if (request.has(TimDepositController.RSUS_STRING)) {
               JSONObject rsusIn = (JSONObject) request.get(TimDepositController.RSUS_STRING);
               if (rsusIn.has(TimDepositController.RSUS_STRING)) {
                 Object rsu = rsusIn.get(TimDepositController.RSUS_STRING);
                 JSONArray rsusOut = new JSONArray();
                 if (rsu instanceof JSONArray) {
                   logger.debug("Multiple RSUs exist in the request: {}", request);
                   JSONArray rsusInArray = (JSONArray) rsu;
                   for (int i = 0; i < rsusInArray.length(); i++) {
                     rsusOut.put(rsusInArray.get(i));
                   }
                   request.put(TimDepositController.RSUS_STRING, rsusOut);
                 } else if (rsu instanceof JSONObject) {
                   logger.debug("Single RSU exists in the request: {}", request);
                   rsusOut.put(rsu);
                   request.put(TimDepositController.RSUS_STRING, rsusOut);
                 } else {
                   logger.debug("No RSUs exist in the request: {}", request);
                   request.remove(TimDepositController.RSUS_STRING);
                 }
               }
            }

            // Convert JSON to POJO
            ServiceRequest servicerequest = getServicerequest(consumedObj);

            processEncodedTim(servicerequest, consumedObj);
         } else {
            throw new Asn1EncodedDataRouterException("Invalid or missing '"
                + TimDepositController.REQUEST_STRING + "' object in the encoder response");
         }
      } catch (Exception e) {
         String msg = "Error in processing received message from ASN.1 Encoder module: " + consumedData;
         EventLogger.logger.error(msg, e);
         logger.error(msg, e);
      }
      return null;
   }

   public ServiceRequest getServicerequest(JSONObject consumedObj) {
      String sr = consumedObj.getJSONObject(AppContext.METADATA_STRING).getJSONObject(TimDepositController.REQUEST_STRING).toString();
      logger.debug("ServiceRequest: {}", sr);

      // Convert JSON to POJO
      ServiceRequest serviceRequest = null;
      try {
         serviceRequest = (ServiceRequest) JsonUtils.fromJson(sr, ServiceRequest.class);

      } catch (Exception e) {
         String errMsg = "Malformed JSON.";
         EventLogger.logger.error(errMsg, e);
         logger.error(errMsg, e);
      }

      return serviceRequest;
   }

   public void processEncodedTim(ServiceRequest request, JSONObject consumedObj) {

      JSONObject dataObj = consumedObj.getJSONObject(AppContext.PAYLOAD_STRING).getJSONObject(AppContext.DATA_STRING);

      // CASE 1: no SDW in metadata (SNMP deposit only)
      // - sign MF
      // - send to RSU
      // CASE 2: SDW in metadata but no ASD in body (send back for another
      // encoding)
      // - sign MF
      // - send to RSU
      // - craft ASD object
      // - publish back to encoder stream
      // CASE 3: If SDW in metadata and ASD in body (double encoding complete)
      // - send to DDS

      if (!dataObj.has(Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING)) {
         logger.debug("Unsigned message received");
         // We don't have ASD, therefore it must be just a MessageFrame that needs to be signed
         // No support for unsecured MessageFrame only payload.
         // Cases 1 & 2
         // Sign and send to RSUs

         JSONObject mfObj = dataObj.getJSONObject(MESSAGE_FRAME);

         String hexEncodedTim = mfObj.getString(BYTES);
         logger.debug("Encoded message - phase 1: {}", hexEncodedTim);

         if (odeProperties.dataSigningEnabled()) {
            logger.debug("Sending message for signature!");
            String base64EncodedTim = CodecUtils.toBase64(
               CodecUtils.fromHex(hexEncodedTim));
            String signedResponse = asn1CommandManager.sendForSignature(base64EncodedTim );

            try {
               hexEncodedTim = CodecUtils.toHex(
                  CodecUtils.fromBase64(
                     JsonUtils.toJSONObject(signedResponse).getString("result")));
            } catch (JsonUtilsException e1) {
               logger.error("Unable to parse signed message response {}", e1);
            }
         }

         logger.debug("Sending message to RSUs...");
         if (null != request.getSnmp() && null != request.getRsus() && null != hexEncodedTim) {
            asn1CommandManager.sendToRsus(request, hexEncodedTim);
         }

         if (request.getSdw() != null) {
            // Case 2 only

            logger.debug("Publishing message for round 2 encoding!");
            String xmlizedMessage = asn1CommandManager.packageSignedTimIntoAsd(request, hexEncodedTim);

            stringMsgProducer.send(odeProperties.getKafkaTopicAsn1EncoderInput(), null, xmlizedMessage);
         }

      } else {
         //We have encoded ASD. It could be either UNSECURED or secured.
         logger.debug("securitySvcsSignatureUri = {}", odeProperties.getSecuritySvcsSignatureUri());

         if (odeProperties.dataSigningEnabled()) {
            logger.debug("Signed message received. Depositing it to SDW.");
            // We have a ASD with signed MessageFrame
            // Case 3
            JSONObject asdObj = dataObj.getJSONObject(Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING);
            try {
              asn1CommandManager.depositToDDS(asdObj.getString(BYTES));
            } catch (JSONException | Asn1CommandManagerException e) {
              String msg = ERROR_ON_DDS_DEPOSIT;
              logger.error(msg, e);
              EventLogger.logger.error(msg, e);
            }
         } else {
            logger.debug("Unsigned ASD received. Depositing it to SDW.");
            //We have ASD with UNSECURED MessageFrame
            processEncodedTimUnsecured(request, consumedObj);
         }
      }
   }

   public void processEncodedTimUnsecured(ServiceRequest request, JSONObject consumedObj) {
      // Send TIMs and record results
      HashMap<String, String> responseList = new HashMap<>();

      JSONObject dataObj = consumedObj
            .getJSONObject(AppContext.PAYLOAD_STRING)
            .getJSONObject(AppContext.DATA_STRING);

      if (null != request.getSdw()) {
         JSONObject asdObj = null;
         if (dataObj.has(Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING)) {
            asdObj = dataObj.getJSONObject(Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING);
         } else {
            logger.error("ASD structure present in metadata but not in JSONObject!");
         }

        if (null != asdObj) {
           String asdBytes = asdObj.getString(BYTES);

           // Deposit to DDS
           String ddsMessage = "";
           try {
              asn1CommandManager.depositToDDS(asdBytes);
              ddsMessage = "\"dds_deposit\":{\"success\":\"true\"}";
              logger.info("DDS deposit successful.");
           } catch (Exception e) {
              ddsMessage = "\"dds_deposit\":{\"success\":\"false\"}";
              String msg = ERROR_ON_DDS_DEPOSIT;
              logger.error(msg, e);
              EventLogger.logger.error(msg, e);
           }

           responseList.put("ddsMessage", ddsMessage);
        } else if (logger.isErrorEnabled()) { // Added to avoid Sonar's "Invoke method(s) only conditionally." code smell
          String msg = "ASN.1 Encoder did not return ASD encoding {}";
          EventLogger.logger.error(msg, consumedObj.toString());
          logger.error(msg, consumedObj.toString());
        }
      }

      if (dataObj.has(MESSAGE_FRAME)) {
         JSONObject mfObj = dataObj.getJSONObject(MESSAGE_FRAME);
         String encodedTim = mfObj.getString(BYTES);
         logger.debug("Encoded message - phase 2: {}", encodedTim);

        // only send message to rsu if snmp, rsus, and message frame fields are present
        if (null != request.getSnmp() && null != request.getRsus() && null != encodedTim) {
           logger.debug("Encoded message phase 3: {}", encodedTim);
           Map<String, String> rsuResponseList =
                 asn1CommandManager.sendToRsus(request, encodedTim);
           responseList.putAll(rsuResponseList);
         }
      }

      logger.info("TIM deposit response {}", responseList);
   }
}
