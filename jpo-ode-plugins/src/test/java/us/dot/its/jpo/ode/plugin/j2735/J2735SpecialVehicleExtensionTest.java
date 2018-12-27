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
package us.dot.its.jpo.ode.plugin.j2735;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import mockit.Tested;

public class J2735SpecialVehicleExtensionTest {
   @Tested
   J2735SpecialVehicleExtensions sve;
   
   @Test
   public void testGettersAndSetters() {
      J2735TrailerData trailers = new J2735TrailerData();
      sve.setTrailers(trailers);
      assertEquals(trailers,sve.getTrailers());
      
      J2735EmergencyDetails vehicleAlerts = new J2735EmergencyDetails();
      sve.setVehicleAlerts(vehicleAlerts);
      assertEquals(vehicleAlerts,sve.getVehicleAlerts());
      
      J2735EventDescription description = new J2735EventDescription();
      sve.setDescription(description);
      assertEquals(description,sve.getDescription());
   }
}
