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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.traveler.TimMessageManipulator;
import us.dot.its.jpo.ode.wrapper.MessageConsumer;

/**
 * Launches ToJsonConverter service
 */
@Controller
public class AsnCodecRouterServiceController {

   private static final Logger logger = LoggerFactory.getLogger(AsnCodecRouterServiceController.class);
   org.apache.kafka.common.serialization.Serdes bas;

   @Autowired
   public AsnCodecRouterServiceController(OdeProperties odeProps, TimMessageManipulator timMessageManipulator) {
      super();

      logger.info("Starting {}", this.getClass().getSimpleName());

      // asn1_codec Decoder Routing
      logger.info("Routing DECODED data received ASN.1 Decoder");

      Asn1DecodedDataRouter decoderRouter = new Asn1DecodedDataRouter(odeProps);

      MessageConsumer<String, String> asn1DecoderConsumer = MessageConsumer.defaultStringMessageConsumer(
         odeProps.getKafkaBrokers(), this.getClass().getSimpleName(), decoderRouter);

      asn1DecoderConsumer.setName("Asn1DecoderConsumer");
      decoderRouter.start(asn1DecoderConsumer, odeProps.getKafkaTopicAsn1DecoderOutput());

      // asn1_codec Encoder Routing
      logger.info("Routing ENCODED data received ASN.1 Encoder");

      Asn1EncodedDataRouter enocderRouter = new Asn1EncodedDataRouter(odeProps, timMessageManipulator);

      MessageConsumer<String, String> encoderConsumer = MessageConsumer.defaultStringMessageConsumer(
         odeProps.getKafkaBrokers(), this.getClass().getSimpleName(), enocderRouter);

      encoderConsumer.setName("Asn1EncoderConsumer");
      enocderRouter.start(encoderConsumer, odeProps.getKafkaTopicAsn1EncoderOutput());
   }
}
