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
package us.dot.its.jpo.ode.plugin.j2735.builders;

import java.math.BigDecimal;

public class VelocityBuilder {

   private VelocityBuilder() {
      throw new UnsupportedOperationException();
   }

   /**
    * Converts velocity from human readable decimal to ASN-compliant int.
    * @param vel
    * @return
    */
   public static int velocity(BigDecimal vel) {

      int convertedValue = 8191;
      if (vel != null) {
         convertedValue = vel.multiply(BigDecimal.valueOf(50)).intValue();
      }

      return convertedValue;
   }
}
