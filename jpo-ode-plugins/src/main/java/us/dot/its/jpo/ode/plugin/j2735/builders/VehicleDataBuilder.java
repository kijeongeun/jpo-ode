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

import us.dot.its.jpo.ode.plugin.j2735.J2735VehicleData;

public class VehicleDataBuilder {
    
    private VehicleDataBuilder() {
       throw new UnsupportedOperationException();
    }

    public static J2735VehicleData genericVehicleData(JsonNode vehicleData) {
        J2735VehicleData vd = new J2735VehicleData();

        if (vehicleData.get("bumpers") != null) {
            vd.setBumpers(BumperHeightsBuilder.genericBumperHeights(vehicleData.get("bumpers")));
        }
        if (vehicleData.get("height") != null) {
            vd.setHeight(HeightBuilder.genericHeight(vehicleData.get("height")));
        }
        if (vehicleData.get("mass") != null) {
            vd.setMass(MassOrWeightBuilder.genericVehicleMass(vehicleData.get("mass")));
        }
        if (vehicleData.get("trailerWeight") != null) {
            vd.setTrailerWeight(MassOrWeightBuilder.genericTrailerWeight(vehicleData.get("trailerWeight")));
        }

        return vd;
    }

}
