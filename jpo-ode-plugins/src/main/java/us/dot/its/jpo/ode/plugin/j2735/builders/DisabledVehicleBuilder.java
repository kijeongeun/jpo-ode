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

import com.fasterxml.jackson.databind.JsonNode;

import us.dot.its.jpo.ode.plugin.j2735.J2735DisabledVehicle;

public class DisabledVehicleBuilder {

   private static final int STATUS_DETAILS_LOWER_BOUND = 523;
   private static final int STATUS_DETAILS_UPPER_BOUND = 541;

   private DisabledVehicleBuilder() {
      throw new UnsupportedOperationException();
   }

   public static J2735DisabledVehicle genericDisabledVehicle(JsonNode disabledVehicle) {
      J2735DisabledVehicle gstatus = new J2735DisabledVehicle();

      // Required element
      int status = disabledVehicle.get("statusDetails").asInt();
      if (STATUS_DETAILS_LOWER_BOUND <= status && status <= STATUS_DETAILS_UPPER_BOUND) {
         gstatus.setStatusDetails(status);
      } else {
         throw new IllegalArgumentException(String.format("Status Details out of bounds [%d,%d], %d",
               STATUS_DETAILS_LOWER_BOUND, STATUS_DETAILS_UPPER_BOUND, status));
      }

      // Optional element
      JsonNode locationDetails = disabledVehicle.get("locationDetails");
      if (locationDetails != null) {
         gstatus.setLocationDetails(NamedNumberBuilder.genericNamedNumber(locationDetails.asText()));
      }

      return gstatus;
   }

}
