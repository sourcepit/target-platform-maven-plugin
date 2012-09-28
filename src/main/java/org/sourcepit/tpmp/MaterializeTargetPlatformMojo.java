/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.sourcepit.common.utils.lang.Exceptions;

/**
 * @goal materialize
 * @requiresProject true
 * @aggregator
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public class MaterializeTargetPlatformMojo extends AbstractTargetPlatformMojo
{
   /** @parameter expression="${tpmp.attach}" default-value="true" */
   private boolean attach;

   /** @parameter expression="${tpmp.install}" default-value="false" */
   private boolean install;

   /** @parameter expression="${tpmp.deploy}" default-value="false" */
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
      final MavenProject project = session.getCurrentProject();
      
      
      
      final File platformDir = getPlatformDir(project);
      updateTargetPlatform(project, platformDir);

      final File platformZipFile = zip(project, platformDir);

      final Artifact platformArtifact = createPlatformArtifact(project);
      platformArtifact.setFile(platformZipFile);
      
      attach(project, platformArtifact);
      
      install(session, platformArtifact);
      
      deploy(session, project, platformArtifact);
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
