/*
 * Copyright 2014 Bernd Vogt and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sourcepit.tpmp;

import org.sourcepit.common.constraints.NotNull;

public class TargetEnvironment {
   private final String os;

   private final String ws;

   private final String arch;

   public TargetEnvironment(@NotNull String os, @NotNull String ws, @NotNull String arch) {
      super();
      this.os = os;
      this.ws = ws;
      this.arch = arch;
   }

   public String getOs() {
      return os;
   }

   public String getWs() {
      return ws;
   }

   public String getArch() {
      return arch;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((arch == null) ? 0 : arch.hashCode());
      result = prime * result + ((os == null) ? 0 : os.hashCode());
      result = prime * result + ((ws == null) ? 0 : ws.hashCode());
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
      TargetEnvironment other = (TargetEnvironment) obj;
      if (arch == null) {
         if (other.arch != null)
            return false;
      }
      else if (!arch.equals(other.arch))
         return false;
      if (os == null) {
         if (other.os != null)
            return false;
      }
      else if (!os.equals(other.os))
         return false;
      if (ws == null) {
         if (other.ws != null)
            return false;
      }
      else if (!ws.equals(other.ws))
         return false;
      return true;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(os).append('/').append(ws).append('/').append(arch);
      return sb.toString();
   }
}
