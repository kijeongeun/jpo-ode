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

public enum J2735FuelType {
   unknownFuel, // 0 -- Gasoline Powered
   gasoline, // 1
   ethanol, // 2 -- Including blends
   diesel, // 3 -- All types
   electric, // 4
   hybrid, // 5 -- All types
   hydrogen, // 6
   natGasLiquid, // 7 -- Liquefied
   natGasComp, // 8 -- Compressed
   propane // 9
}
