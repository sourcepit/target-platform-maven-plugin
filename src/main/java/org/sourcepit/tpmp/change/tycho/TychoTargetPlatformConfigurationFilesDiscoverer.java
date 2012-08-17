/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
