/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.b2;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public final class B2Utils
{
   private B2Utils()
   {
      super();
   }

   public static File findModuleXML(MavenSession session, MavenProject project)
   {
      final File moduleXML = findModuleXML(project.getBasedir());
      if (moduleXML != null && findModuleProject(session, moduleXML) != null)
      {
         return moduleXML;
      }
      return null;
   }

   public static MavenProject findModuleProject(MavenSession session, final File moduleXML)
   {
      final File moduleDir = moduleXML.getParentFile();
      for (MavenProject mavenProject : session.getProjects())
      {
         if (moduleDir.equals(mavenProject.getBasedir()))
         {
            return mavenProject;
         }
      }
      return null;
   }

   public static boolean isDerivedProject(MavenProject project)
   {
      File parentFile = project.getBasedir().getParentFile();
      while (parentFile != null)
      {
         if (".b2".equals(parentFile.getName()))
         {
            return true;
         }
         parentFile = parentFile.getParentFile();
      }
      return false;
   }

   private static File findModuleXML(File basedir)
   {
      final File moduleXML = new File(basedir, "module.xml");
      if (moduleXML.exists())
      {
         return moduleXML;
      }
      final File parentDir = basedir.getParentFile();
      return parentDir == null ? null : findModuleXML(parentDir);
   }
}
