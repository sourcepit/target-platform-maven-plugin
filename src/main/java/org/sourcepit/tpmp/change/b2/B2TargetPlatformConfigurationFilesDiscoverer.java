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

package org.sourcepit.tpmp.change.b2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TychoProject;
import org.sourcepit.tpmp.b2.B2Utils;
import org.sourcepit.tpmp.change.TargetPlatformConfigurationFilesDiscoverer;
import org.sourcepit.tpmp.change.tycho.TychoTargetPlatformConfigurationFilesDiscoverer;

@Named("b2")
public class B2TargetPlatformConfigurationFilesDiscoverer extends TychoTargetPlatformConfigurationFilesDiscoverer
   implements
      TargetPlatformConfigurationFilesDiscoverer
{
   @Inject
   public B2TargetPlatformConfigurationFilesDiscoverer(Map<String, TychoProject> projectTypes)
   {
      super(projectTypes);
   }

   @Override
   public List<File> getTargetPlatformConfigurationFiles(MavenSession session, MavenProject project)
   {
      final List<File> files = new ArrayList<File>();

      final File moduleXML = B2Utils.findModuleXML(session, project);
      if (moduleXML != null)
      {
         final File modulePom = new File(moduleXML.getParentFile(), ".b2/module-pom-template.xml");
         if (modulePom.exists())
         {
            files.add(modulePom);

            if (!B2Utils.isDerivedProject(project))
            {
               final File b2ExtFile = new File(project.getBasedir(), "b2-extension.xml");
               if (b2ExtFile.exists())
               {
                  files.add(b2ExtFile);
               }

               files.addAll(super.getTargetPlatformConfigurationFiles(session, project));
               files.remove(project.getFile());
            }
            return files;
         }
      }

      throw new IllegalStateException();
   }

}
