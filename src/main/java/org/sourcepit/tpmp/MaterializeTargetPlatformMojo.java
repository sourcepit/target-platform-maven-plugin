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

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.sourcepit.common.utils.lang.Exceptions;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
@Mojo(name = "materialize", requiresProject = true, aggregator = true)
public class MaterializeTargetPlatformMojo extends AbstractTargetPlatformMojo
{
   @Parameter(property = "tpmp.attach", defaultValue = "true")
   private boolean attach;

   @Parameter(property = "tpmp.install", defaultValue = "false")
   private boolean install;

   @Parameter(property = "tpmp.deploy", defaultValue = "false")
   private boolean deploy;
   @Inject
   private ArtifactDeployer deployer;

   @Inject
   private ArtifactInstaller installer;

   @Inject
   private MavenProjectHelper projectHelper;

   @Override
   protected void doExecute()
   {
      final MavenProject project = getSession().getCurrentProject();

      final File platformDir = getPlatformDir(project);
      updateTargetPlatform(project, platformDir);

      final File platformZipFile = zip(project, platformDir);

      final Artifact platformArtifact = createPlatformArtifact(project);
      platformArtifact.setFile(platformZipFile);

      attach(project, platformArtifact);

      install(getSession(), platformArtifact);

      deploy(getSession(), project, platformArtifact);
   }

   private File zip(final MavenProject project, final File platformDir)
   {
      final File platformZipFile = getPlatformZipFile(project);
      new SimpleZipper().zip(platformDir, platformZipFile, getClassifiedName(project));
      return platformZipFile;
   }

   private void attach(MavenProject project, final Artifact platformArtifact)
   {
      if (attach)
      {
         projectHelper.attachArtifact(project, platformArtifact.getType(), platformArtifact.getClassifier(),
            platformArtifact.getFile());
      }
   }

   private void install(MavenSession session, final Artifact platformArtifact)
   {
      if (install)
      {
         try
         {
            installer.install(platformArtifact.getFile(), platformArtifact, session.getLocalRepository());
         }
         catch (ArtifactInstallationException e)
         {
            throw Exceptions.pipe(e);
         }
      }
   }

   private void deploy(MavenSession session, MavenProject project, final Artifact platformArtifact)
   {
      if (deploy)
      {
         try
         {
            deployer.deploy(platformArtifact.getFile(), platformArtifact,
               project.getDistributionManagementArtifactRepository(), session.getLocalRepository());
         }
         catch (ArtifactDeploymentException e)
         {
            throw Exceptions.pipe(e);
         }
      }
   }
}
