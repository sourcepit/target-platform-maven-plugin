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

package org.sourcepit.tpmp.resolver;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcepit.tpmp.change.TargetPlatformConfigurationChangeDiscoverer;

@Named("per-project")
public class PerProjectTargetPlatformResolver implements TargetPlatformResolver {

   private static final Logger LOGGER = LoggerFactory.getLogger(PerProjectTargetPlatformResolver.class);

   @Inject
   private TargetPlatformConfigurationChangeDiscoverer changeDiscoverer;

   @Inject
   private ProjectTargetPlatformResolver tpResolver;

   @Override
   public boolean isRelyingOnCachedFiles() {
      return true;
   }

   public Logger getLogger() {
      return LOGGER;
   }

   @Override
   public void resolve(MavenSession session, File platformDir, boolean includeSource, boolean forceUpdate,
      TargetPlatformConfigurationHandler configHandler, TargetPlatformResolutionHandler resolutionHandler) {
      resolveTargetPlatformConfiguration(session, configHandler);
      resolveTargetPlatform(session, includeSource, forceUpdate, getMetadataDir(platformDir), resolutionHandler);
   }

   private void resolveTargetPlatformConfiguration(MavenSession session, TargetPlatformConfigurationHandler handler) {
      for (MavenProject project : session.getProjects()) {
         tpResolver.resolveTargetPlatformConfiguration(session, project, handler);
      }
   }

   private void resolveTargetPlatform(MavenSession session, boolean includeSource, boolean forceUpdate,
      File metadataDir, final TargetPlatformResolutionHandler handler) {
      for (MavenProject project : session.getProjects()) {
         resolveTargetPlatform(session, project, includeSource, forceUpdate, metadataDir, handler);
      }
   }

   private void resolveTargetPlatform(MavenSession session, MavenProject project, boolean includeSource,
      boolean forceUpdate, File metadataDir, final TargetPlatformResolutionHandler handler) throws Error {
      if (isResolutionRequired(metadataDir, session, project, forceUpdate)) {
         getLogger().info("Materializing target platform of project " + project.getId());
         try {
            tpResolver.resolveTargetPlatform(session, project, includeSource, handler);
         }
         catch (RuntimeException e) {
            changeDiscoverer.clearTargetPlatformConfigurationStausCache(metadataDir, project);
            throw e;
         }
         catch (Error e) {
            changeDiscoverer.clearTargetPlatformConfigurationStausCache(metadataDir, project);
            throw e;
         }
      }
      else {
         getLogger().info("Target platform of project " + project.getId() + " already materialized and up to date");
      }
   }

   private boolean isResolutionRequired(File metadataDir, MavenSession session, MavenProject project,
      boolean forceUpdate) {
      if (changeDiscoverer.hasTargetPlatformConfigurationChanged(metadataDir, session, project)) {
         return true;
      }
      return forceUpdate;
   }

   protected File getMetadataDir(final File platformDir) {
      return new File(platformDir, ".tpmp");
   }
}
