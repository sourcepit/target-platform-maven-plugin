/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.sourcepit.tpmp.change.TargetPlatformConfigurationChangeDiscoverer;

@Named("per-project")
public class PerProjectTargetPlatformResolver implements TargetPlatformResolver
{
   @Inject
   private Logger logger;

   @Inject
   private TargetPlatformConfigurationChangeDiscoverer changeDiscoverer;

   @Inject
   private ProjectTargetPlatformResolver tpResolver;

   @Override
   public boolean isRelyingOnCachedFiles()
   {
      return true;
   }

   public Logger getLogger()
   {
      return logger;
   }

   @Override
   public void resolve(MavenSession session, File platformDir, boolean includeSource, boolean forceUpdate,
      TargetPlatformConfigurationHandler configHandler, TargetPlatformResolutionHandler resolutionHandler)
   {
      resolveTargetPlatformConfiguration(session, configHandler);
      resolveTargetPlatform(session, includeSource, forceUpdate, getMetadataDir(platformDir), resolutionHandler);
   }

   private void resolveTargetPlatformConfiguration(MavenSession session, TargetPlatformConfigurationHandler handler)
   {
      for (MavenProject project : session.getProjects())
      {
         tpResolver.resolveTargetPlatformConfiguration(session, project, handler);
      }
   }

   private void resolveTargetPlatform(MavenSession session, boolean includeSource, boolean forceUpdate,
      File metadataDir, final TargetPlatformResolutionHandler handler)
   {
      for (MavenProject project : session.getProjects())
      {
         resolveTargetPlatform(session, project, includeSource, forceUpdate, metadataDir, handler);
      }
   }

   private void resolveTargetPlatform(MavenSession session, MavenProject project, boolean includeSource,
      boolean forceUpdate, File metadataDir, final TargetPlatformResolutionHandler handler) throws Error
   {
      if (isResolutionRequired(metadataDir, session, project, forceUpdate))
      {
         getLogger().info("Materializing target platform of project " + project.getId());
         try
         {
            tpResolver.resolveTargetPlatform(session, project, includeSource, handler);
         }
         catch (RuntimeException e)
         {
            changeDiscoverer.clearTargetPlatformConfigurationStausCache(metadataDir, project);
            throw e;
         }
         catch (Error e)
         {
            changeDiscoverer.clearTargetPlatformConfigurationStausCache(metadataDir, project);
            throw e;
         }
      }
      else
      {
         getLogger().info("Target platform of project " + project.getId() + " already materialized and up to date");
      }
   }

   private boolean isResolutionRequired(File metadataDir, MavenSession session, MavenProject project,
      boolean forceUpdate)
   {
      if (changeDiscoverer.hasTargetPlatformConfigurationChanged(metadataDir, session, project))
      {
         return true;
      }
      return forceUpdate;
   }

   protected File getMetadataDir(final File platformDir)
   {
      return new File(platformDir, ".tpmp");
   }
}
