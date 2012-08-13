/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.slf4j.Logger;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.path.PathUtils;
import org.sourcepit.common.utils.zip.ZipProcessingRequest;
import org.sourcepit.common.utils.zip.ZipProcessor;
import org.sourcepit.guplex.Guplex;
import org.sourcepit.mtp.change.TargetPlatformConfigurationChangeDiscoverer;
import org.sourcepit.mtp.ee.ExecutionEnvironmentSelector;
import org.sourcepit.mtp.resolver.TargetPlatformResolver;

/**
 * @goal materialize-target-platform
 * @requiresProject true
 * @aggregator
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public class MaterializeTargetPlatformMojo extends AbstractMojo
{
   /** @parameter default-value="${project.build.directory}" */
   private File targetDir;

   /** @parameter expression="${name}" default-value="${project.artifactId}" */
   private String name;

   /**
    * @parameter expression="${session}"
    * @readonly
    * @required
    */
   private MavenSession session;

   /** @parameter expression="${mtp.forceUpdate}" default-value="false" */
   private boolean forceUpdate;

   /** @parameter default-value="target" */
   private String classifier;

   /** @component */
   private Guplex guplex;

   @Inject
   private TargetPlatformResolver tpResolver;

   @Inject
   private ExecutionEnvironmentSelector eeSelector;

   @Inject
   private TargetPlatformConfigurationChangeDiscoverer changeDiscoverer;

   @Inject
   private Logger log;

   @Inject
   private RepositorySystem repositorySystem;

   @Inject
   private ArtifactDeployer deployer;

   @Inject
   private ArtifactInstaller installer;

   public final void execute() throws MojoExecutionException, MojoFailureException
   {
      guplex.inject(this, true);
      doExecute();
   }

   private void doExecute() throws MojoExecutionException, MojoFailureException
   {
      final MavenProject project = session.getCurrentProject();

      final File platformDir = getPlatformDir();
      if (!platformDir.exists() && !forceUpdate)
      {
         download(session, project, platformDir);
      }

      final CopyTargetPlatformResolutionHandler resolutionHandler = new CopyTargetPlatformResolutionHandler(platformDir);
      resolve(new File(platformDir, ".mtp"), resolutionHandler);

      final String executionEnvironment = eeSelector.select(resolutionHandler.getExecutionEnvironments());

      writeDefinitions(platformDir, executionEnvironment, resolutionHandler.getTargetEnvironments());

      final File platformZipFile = getPlatformZipFile();
      new SimpleZipper().zip(platformDir, platformZipFile, getClassifiedName());

      installAndDeploy(session, project, platformZipFile);
   }

   private void installAndDeploy(MavenSession session, MavenProject project, File platformZipFile)
   {
      final Artifact platformArtifact = createPlatformArtifact(project);
      platformArtifact.setFile(platformZipFile);

      try
      {
         installer.install(platformZipFile, platformArtifact, session.getLocalRepository());
      }
      catch (ArtifactInstallationException e)
      {
         throw Exceptions.pipe(e);
      }

      try
      {
         deployer.deploy(platformZipFile, platformArtifact, project.getDistributionManagementArtifactRepository(),
            session.getLocalRepository());
      }
      catch (ArtifactDeploymentException e)
      {
         throw Exceptions.pipe(e);
      }
   }

   private void download(MavenSession session, MavenProject project, File platformDir)
   {
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

      if (platformArtifact.getFile().exists())
      {
         final ZipProcessingRequest unzipRequest = ZipProcessingRequest.newUnzipRequest(platformArtifact.getFile(),
            platformDir);
         try
         {
            new ZipProcessor().process(unzipRequest);
         }
         catch (IOException e)
         {
            throw Exceptions.pipe(e);
         }
         try
         {
            final File zipRoot = platformDir.listFiles()[0];
            final File[] files = zipRoot.listFiles();
            for (File file : files)
            {
               if (file.isDirectory())
               {
                  FileUtils.moveDirectory(file, new File(platformDir, file.getName()));
               }
               else
               {
                  FileUtils.moveFile(file, new File(platformDir, file.getName()));
               }
            }
            FileUtils.forceDelete(zipRoot);
         }
         catch (IOException e)
         {
            throw Exceptions.pipe(e);
         }
      }
   }

   private Artifact createPlatformArtifact(MavenProject project)
   {
      final Artifact platformArtifact = repositorySystem.createArtifactWithClassifier(project.getGroupId(),
         project.getArtifactId(), project.getVersion(), "zip", classifier);
      return platformArtifact;
   }

   private void writeDefinitions(File platformDir, String executionEnvironment,
      Collection<TargetEnvironment> targetEnvironments)
   {
      for (TargetEnvironment targetEnvironment : targetEnvironments)
      {
         final String platformName = getTargetPlatformDefinitionName(targetEnvironment);

         final File targetFile = new File(platformDir, platformName + ".target");

         String relativePath = PathUtils.getRelativePath(targetFile.getParentFile(), platformDir, "/");
         if (relativePath == null || relativePath.length() == 0)
         {
            relativePath = ".";
         }

         new TargetPlatformWriter().write(targetFile, platformName, relativePath, targetEnvironment,
            executionEnvironment);
      }
   }

   private String getTargetPlatformDefinitionName(TargetEnvironment targetEnvironment)
   {
      final StringBuilder sb = new StringBuilder();
      sb.append(name);
      sb.append('-');
      sb.append(targetEnvironment.getOs());
      sb.append('-');
      sb.append(targetEnvironment.getWs());
      sb.append('-');
      sb.append(targetEnvironment.getArch());
      if (targetEnvironment.getNl() != null)
      {
         sb.append('-');
         sb.append(targetEnvironment.getNl());
      }
      return sb.toString();
   }

   private void resolve(File metadataDir, final CopyTargetPlatformResolutionHandler handler)
   {
      for (MavenProject project : session.getProjects())
      {
         if (isResolutionRequired(metadataDir, project))
         {
            log.info("Materializing target platform of project " + project.getId());
            try
            {
               tpResolver.resolveTargetPlatform(session, project, handler);
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
            log.info("Target platform of project " + project.getId() + " already materialized and up to date");
         }
      }
   }

   private boolean isResolutionRequired(File metadataDir, MavenProject project)
   {
      if (changeDiscoverer.hasTargetPlatformConfigurationChanged(metadataDir, project))
      {
         return true;
      }
      return forceUpdate;
   }

   private File getPlatformZipFile()
   {
      return new File(targetDir, getClassifiedName() + ".zip");
   }

   private File getPlatformDir()
   {
      return new File(targetDir, getClassifiedName());
   }

   private String getClassifiedName()
   {
      return getFinalName() + "-" + classifier;
   }

   private String getFinalName()
   {
      final MavenProject project = session.getCurrentProject();
      String finalName = project.getBuild().getFinalName();
      if (finalName == null)
      {
         finalName = project.getArtifactId() + "-" + project.getVersion();
      }
      return finalName;
   }
}
