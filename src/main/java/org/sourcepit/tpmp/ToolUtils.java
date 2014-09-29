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

import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.sourcepit.tpmp.b2.B2Utils;

public final class ToolUtils
{
   private ToolUtils()
   {
      super();
   }

   public static String getTool(MavenSession session, MavenProject project)
   {
      final Properties properties = new Properties();
      properties.putAll(project.getProperties());
      properties.putAll(session.getSystemProperties()); // session wins
      properties.putAll(session.getUserProperties());

      String tool = properties.getProperty("tpmp.tool");
      if (tool == null)
      {
         tool = B2Utils.findModuleXML(session, project) == null ? "Tycho" : "b2";
      }

      return tool;
   }
}
