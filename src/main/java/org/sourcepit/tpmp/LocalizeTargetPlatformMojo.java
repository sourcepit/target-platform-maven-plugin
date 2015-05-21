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

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.zip.ZipProcessingRequest;
import org.sourcepit.common.utils.zip.ZipProcessor;


/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
@Mojo(name = "localize", requiresProject = true, aggregator = true)
public class LocalizeTargetPlatformMojo extends AbstractTargetPlatformMojo {
   @Override
   protected void doExecute() {
      final MavenProject project = getSession().getCurrentProject();

      final File platformDir = downloadTargetPlatformOnDemand(project);

      updateTargetPlatform(project, platformDir);
   }

   protected File downloadTargetPlatformOnDemand(MavenProject project) {
      final File platformDir = getPlatformDir(project);
      if (!platformDir.exists() && getResolver().isRelyingOnCachedFiles()) {
         download(getSession(), project, platformDir.getParentFile());
      }
      return platformDir;
   }

   private void download(MavenSession session, MavenProject project, File parentDir) {
      final Artifact platformArtifact = createPlatformArtifact(project);

      final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
      request.setArtifact(platformArtifact);
      request.setResolveRoot(true);
      request.setResolveTransitively(false);
      request.setLocalRepository(session.getLocalRepository());
      request.setRemoteRepositories(project.getRemoteArtifactRepositories());
      request.setManagedVersionMap(project.getManagedVersionMap());
      request.setOffline(session.isOffline());

      repositorySystem.resolve(request);

      if (platformArtifact.getFile().exists()) {
         final ZipProcessingRequest unzipRequest = ZipProcessingRequest.newUnzipRequest(platformArtifact.getFile(),
            parentDir);
         try {
            new ZipProcessor().process(unzipRequest);
         }
         catch (IOException e) {
            throw Exceptions.pipe(e);
         }
      }
   }
}
