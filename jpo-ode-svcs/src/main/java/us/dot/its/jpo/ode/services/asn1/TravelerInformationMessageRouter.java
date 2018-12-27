package us.dot.its.jpo.ode.services.asn1;

import java.text.ParseException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.coder.OdeTimDataCreatorHelper;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.model.OdeMsgPayload;
import us.dot.its.jpo.ode.model.OdeRequestMsgMetadata;
import us.dot.its.jpo.ode.model.OdeTimData;
import us.dot.its.jpo.ode.model.OdeTravelerInputData;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy;
import us.dot.its.jpo.ode.plugin.j2735.DdsAdvisorySituationData;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.plugin.j2735.builders.TravelerMessageFromHumanToAsnConverter;
import us.dot.its.jpo.ode.traveler.TimMessageManipulator;
import us.dot.its.jpo.ode.util.DateTimeUtils;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.wrapper.AbstractSubscriberProcessor;
import us.dot.its.jpo.ode.wrapper.MessageProducer;

public class TravelerInformationMessageRouter extends AbstractSubscriberProcessor<String, OdeTravelerInputData> {
   
   private static final Logger logger = LoggerFactory.getLogger(TravelerInformationMessageRouter.class);
   
   private Object stringMsgProducer;
   private TimMessageManipulator timMessageManipulator;
   private OdeProperties odeProperties;
   private SerialId serialIdOde;
   private SerialId serialIdJ2735;

   public TravelerInformationMessageRouter(OdeProperties odeProperties, TimMessageManipulator timMessageManipulator) {
      super();
      this.odeProperties = odeProperties;
      this.timMessageManipulator = timMessageManipulator;
      this.serialIdJ2735 = new SerialId();
      this.serialIdOde = new SerialId();
      this.stringMsgProducer = MessageProducer.defaultStringMessageProducer(odeProperties.getKafkaBrokers(),
            odeProperties.getKafkaProducerType(), odeProperties.getKafkaTopicsDisabledSet());
   }

   @Override
   protected Object process(OdeTravelerInputData consumedData) {
      
      // Add metadata to message and publish to kafka
      OdeTravelerInformationMessage tim = consumedData.getTim();
      OdeMsgPayload timDataPayload = new OdeMsgPayload(tim);
      OdeRequestMsgMetadata timMetadata = new OdeRequestMsgMetadata(timDataPayload, consumedData.getRequest());

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
      }

      OdeTimData odeTimData = new OdeTimData(timMetadata, timDataPayload);
      timProducer.send(odeProperties.getKafkaTopicOdeTimBroadcastPojo(), null, odeTimData);

      String obfuscatedTimData = obfuscateRsuPassword(odeTimData.toJson());
      stringMsgProducer.send(odeProperties.getKafkaTopicOdeTimBroadcastJson(), null, obfuscatedTimData);

      // Now that the message gas been published to OdeBradcastTim topic, it should be
      // changed to J2735BroadcastTim serialId
      timMetadata.setSerialId(serialIdJ2735);

      // Short circuit
      // If the TIM has no RSU/SNMP or SDW structures, we are done
      if ((request.getRsus() == null || request.getSnmp() == null) && request.getSdw() == null) {
         String warningMsg = "Warning: TIM contains no RSU, SNMP, or SDW fields. Message only published to POJO broadcast stream.";
         logger.warn(warningMsg);
         return ResponseEntity.status(HttpStatus.OK).body(JsonUtils.jsonKeyValue(WARNING, warningMsg));
      }

      // Craft ASN-encodable TIM
      ObjectNode encodableTid;
      try {
         encodableTid = JsonUtils.toObjectNode(odeTID.toJson());
         TravelerMessageFromHumanToAsnConverter.convertTravelerInputDataToEncodableTim(encodableTid);

         logger.debug("Encodable Traveler Information Data: {}", encodableTid);

      } catch (JsonUtilsException e) {
         String errMsg = "Error converting to encodable TravelerInputData.";
         logger.error(errMsg, e);
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
      }

      try {
         logger.debug("securitySvcsSignatureUri = {}", odeProperties.getSecuritySvcsSignatureUri());
         String xmlMsg;
         DdsAdvisorySituationData asd = null;
         if (!odeProperties.dataSigningEnabled()) {
            // We need to send data UNSECURED, so we should try to build the ASD as well as
            // MessageFrame
            asd = buildASD(odeTID.getRequest());
         }
         xmlMsg = timMessageManipulator.convertToXml(asd, encodableTid, timMetadata, serialIdJ2735);
         JSONObject jsonMsg = XmlUtils.toJSONObject(xmlMsg);

         String j2735Tim = OdeTimDataCreatorHelper.createOdeTimData(jsonMsg.getJSONObject(AppContext.ODE_ASN1_DATA))
               .toString();

         stringMsgProducer.send(odeProperties.getKafkaTopicAsn1EncoderInput(), null, xmlMsg);

         String obfuscatedj2735Tim = obfuscateRsuPassword(j2735Tim);
         // publish Broadcast TIM to a J2735 compliant topic.
         stringMsgProducer.send(odeProperties.getKafkaTopicJ2735TimBroadcastJson(), null, obfuscatedj2735Tim);
         // publish J2735 TIM also to general un-filtered TIM topic
         stringMsgProducer.send(odeProperties.getKafkaTopicOdeTimJson(), null, obfuscatedj2735Tim);

         serialIdOde.increment();
         serialIdJ2735.increment();
      } catch (Exception e) {
         String errMsg = "Error sending data to ASN.1 Encoder module: " + e.getMessage();
         logger.error(errMsg, e);
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
      }
   }
   
   public String obfuscateRsuPassword(String message) {
      return message.replaceAll("\"rsuPassword\": *\".*?\"", "\"rsuPassword\":\"*\"");
   }
}
