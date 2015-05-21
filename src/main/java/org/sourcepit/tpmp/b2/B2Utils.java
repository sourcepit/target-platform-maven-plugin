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

package org.sourcepit.tpmp.b2;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public final class B2Utils {
   private B2Utils() {
      super();
   }

   public static File findModuleXML(MavenSession session, MavenProject project) {
      final File moduleXML = findModuleXML(project.getBasedir());
      if (moduleXML != null && findModuleProject(session, moduleXML) != null) {
         return moduleXML;
      }
      return null;
   }

   public static MavenProject findModuleProject(MavenSession session, final File moduleXML) {
      final File moduleDir = moduleXML.getParentFile();
      for (MavenProject mavenProject : session.getProjects()) {
         if (moduleDir.equals(mavenProject.getBasedir())) {
            return mavenProject;
         }
      }
      return null;
   }

   public static boolean isDerivedProject(MavenProject project) {
      File parentFile = project.getBasedir().getParentFile();
      while (parentFile != null) {
         if (".b2".equals(parentFile.getName())) {
            return true;
         }
         parentFile = parentFile.getParentFile();
      }
      return false;
   }

   private static File findModuleXML(File basedir) {
      final File moduleXML = new File(basedir, "module.xml");
      if (moduleXML.exists()) {
         return moduleXML;
      }
      final File parentDir = basedir.getParentFile();
      return parentDir == null ? null : findModuleXML(parentDir);
   }
}
