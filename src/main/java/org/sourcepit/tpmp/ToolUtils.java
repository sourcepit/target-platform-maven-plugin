/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
