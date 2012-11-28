/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp;

import javax.validation.constraints.NotNull;

public class TargetEnvironment
{
   private final String os;

   private final String ws;

   private final String arch;

   public TargetEnvironment(@NotNull String os, @NotNull String ws, @NotNull String arch)
   {
      super();
      this.os = os;
      this.ws = ws;
      this.arch = arch;
   }

   public String getOs()
   {
      return os;
   }

   public String getWs()
   {
      return ws;
   }

   public String getArch()
   {
      return arch;
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((arch == null) ? 0 : arch.hashCode());
      result = prime * result + ((os == null) ? 0 : os.hashCode());
      result = prime * result + ((ws == null) ? 0 : ws.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      TargetEnvironment other = (TargetEnvironment) obj;
      if (arch == null)
      {
         if (other.arch != null)
            return false;
      }
      else if (!arch.equals(other.arch))
         return false;
      if (os == null)
      {
         if (other.os != null)
            return false;
      }
      else if (!os.equals(other.os))
         return false;
      if (ws == null)
      {
         if (other.ws != null)
            return false;
      }
      else if (!ws.equals(other.ws))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      final StringBuilder sb = new StringBuilder();
      sb.append(os).append('/').append(ws).append('/').append(arch);
      return sb.toString();
   }
}
