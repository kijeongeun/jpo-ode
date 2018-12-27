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

import us.dot.its.jpo.ode.plugin.j2735.J2735Node_XY;

public class Node_XYBuilder {

    private Node_XYBuilder() {
       throw new UnsupportedOperationException();
    }

    public static J2735Node_XY genericNode_XY(JsonNode node) {
        return new J2735Node_XY(OffsetBuilder.genericOffset_B12(node.get("x")), OffsetBuilder.genericOffset_B12(node.get("y")));
    }

}
