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

package org.sourcepit.tpmp.change.tycho;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.EclipseApplicationProject;
import org.eclipse.tycho.core.osgitools.EclipseFeatureProject;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.core.osgitools.OsgiBundleProject;
import org.eclipse.tycho.core.osgitools.UpdateSiteProject;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.UpdateSite;
import org.sourcepit.tpmp.change.TargetPlatformConfigurationFilesDiscoverer;

@Named("Tycho")
public class TychoTargetPlatformConfigurationFilesDiscoverer implements TargetPlatformConfigurationFilesDiscoverer
{
   private final Map<String, TychoProject> projectTypes;

   @Inject
   public TychoTargetPlatformConfigurationFilesDiscoverer(Map<String, TychoProject> projectTypes)
   {
      this.projectTypes = projectTypes;
   }

   @Override
   public List<File> getTargetPlatformConfigurationFiles(MavenSession session, MavenProject project)
   {
      final List<File> files = new ArrayList<File>();
      files.add(project.getFile());

      final TychoProject tychoProject = projectTypes.get(project.getPackaging());
      if (tychoProject != null)
      {
         if (tychoProject instanceof OsgiBundleProject)
         {
            files.add(new File(project.getBasedir(), "META-INF/MANIFEST.MF"));
         }
         else if (tychoProject instanceof EclipseApplicationProject)
         {
            files.add(new File(project.getBasedir(), project.getArtifactId() + ".product"));
         }
         else if (tychoProject instanceof EclipseFeatureProject)
         {
            files.add(new File(project.getBasedir(), Feature.FEATURE_XML));
         }
         else if (tychoProject instanceof EclipseRepositoryProject)
         {
            files.addAll(/* ((EclipseRepositoryProject) tychoProject). */getCategoryFiles(project));
            files.addAll(((EclipseRepositoryProject) tychoProject).getProductFiles(project));
         }
         else if (tychoProject instanceof UpdateSiteProject)
         {
            files.add(new File(project.getBasedir(), UpdateSite.SITE_XML));
         }
      }

      return files;
   }

   private List<File> getCategoryFiles(MavenProject project)
   {
      List<File> res = new ArrayList<File>();
      File categoryFile = new File(project.getBasedir(), "category.xml");
      if (categoryFile.exists())
      {
         res.add(categoryFile);
      }
      return res;
   }

}
