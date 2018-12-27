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
package us.dot.its.jpo.ode.plugin;

import us.dot.its.jpo.ode.model.OdeObject;

public class RoadSideUnit {
   public static class RSU extends OdeObject {

      private static final long serialVersionUID = 3149576493038209597L;

      private String rsuTarget;
      private String rsuUsername;
      private String rsuPassword;
      private int rsuRetries;
      private int rsuTimeout;
      private int rsuIndex;

      public RSU() {
         super();
      }

      public RSU(String rsuTarget, String rsuUsername, String rsuPassword, int rsuRetries, int rsuTimeout) {
         super();
         this.rsuTarget = rsuTarget;
         this.rsuUsername = rsuUsername;
         this.rsuPassword = rsuPassword;
         this.rsuRetries = rsuRetries;
         this.rsuTimeout = rsuTimeout;
      }

      public String getRsuTarget() {
         return rsuTarget;
      }

      public void setRsuTarget(String rsuTarget) {
         this.rsuTarget = rsuTarget;
      }

      public String getRsuUsername() {
         return rsuUsername;
      }

      public void setRsuUsername(String rsuUsername) {
         this.rsuUsername = rsuUsername;
      }

      public String getRsuPassword() {
         return rsuPassword;
      }

      public void setRsuPassword(String rsuPassword) {
         this.rsuPassword = rsuPassword;
      }

      public int getRsuRetries() {
         return rsuRetries;
      }

      public void setRsuRetries(int rsuRetries) {
         this.rsuRetries = rsuRetries;
      }

      public int getRsuTimeout() {
         return rsuTimeout;
      }

      public void setRsuTimeout(int rsuTimeout) {
         this.rsuTimeout = rsuTimeout;
      }

      public int getRsuIndex() {
        return rsuIndex;
      }

      public void setRsuIndex(int rsuIndex) {
        this.rsuIndex = rsuIndex;
      }

      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + rsuIndex;
        result = prime * result + ((rsuPassword == null) ? 0 : rsuPassword.hashCode());
        result = prime * result + rsuRetries;
        result = prime * result + ((rsuTarget == null) ? 0 : rsuTarget.hashCode());
        result = prime * result + rsuTimeout;
        result = prime * result + ((rsuUsername == null) ? 0 : rsuUsername.hashCode());
        return result;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj)
          return true;
        if (obj == null)
          return false;
        if (getClass() != obj.getClass())
          return false;
        RSU other = (RSU) obj;
        if (rsuIndex != other.rsuIndex)
          return false;
        if (rsuPassword == null) {
          if (other.rsuPassword != null)
            return false;
        } else if (!rsuPassword.equals(other.rsuPassword))
          return false;
        if (rsuRetries != other.rsuRetries)
          return false;
        if (rsuTarget == null) {
          if (other.rsuTarget != null)
            return false;
        } else if (!rsuTarget.equals(other.rsuTarget))
          return false;
        if (rsuTimeout != other.rsuTimeout)
          return false;
        if (rsuUsername == null) {
          if (other.rsuUsername != null)
            return false;
        } else if (!rsuUsername.equals(other.rsuUsername))
          return false;
        return true;
      }
   }

   private RoadSideUnit() {
      throw new UnsupportedOperationException();
   }
}
