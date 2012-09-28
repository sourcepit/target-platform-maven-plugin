/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.slf4j.Logger;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.zip.ZipProcessingRequest;
import org.sourcepit.common.utils.zip.ZipProcessor;
import org.sourcepit.tpmp.change.TargetPlatformConfigurationChangeDiscoverer;
import org.sourcepit.tpmp.ee.ExecutionEnvironmentSelector;
import org.sourcepit.tpmp.resolver.TargetPlatformConfigurationHandler;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;
import org.sourcepit.tpmp.resolver.TargetPlatformResolver;
import org.sourcepit.tpmp.resolver.tycho.TychoSessionTargetPlatformResolver;

public abstract class AbstractTargetPlatformMojo extends AbstractGuplexedMojo
{
   /** @parameter expression="${session}" */
   protected MavenSession session;

   /** @parameter default-value="${project.build.directory}" */
   protected File targetDir;

   /** @parameter expression="${tpmp.forceUpdate}" default-value="false" */
   private boolean forceUpdate;

   /** @parameter expression="${tpmp.includeSource}" default-value="true" */
   private boolean includeSource;

   /** @parameter expression="${tpmp.classifier}" default-value="target" */
   protected String classifier;

   @Inject
   private Logger logger;

   @Inject
   private RepositorySystem repositorySystem;

   @Inject
   private TargetPlatformConfigurationChangeDiscoverer changeDiscoverer;

   @Inject
   private TargetPlatformResolver tpResolver;

   @Inject
   private ExecutionEnvironmentSelector eeSelector;

   private void download(MavenSession session, MavenProject project, File parentDir)
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
            parentDir);
         try
         {
            new ZipProcessor().process(unzipRequest);
         }
         catch (IOException e)
         {
            throw Exceptions.pipe(e);
         }
      }
   }

   protected Artifact createPlatformArtifact(MavenProject project)
   {
      final Artifact platformArtifact = repositorySystem.createArtifactWithClassifier(project.getGroupId(),
         project.getArtifactId(), project.getVersion(), "zip", classifier);
      return platformArtifact;
   }

   @Inject
   TychoSessionTargetPlatformResolver sessionResolver;

   protected void updateTargetPlatform(final MavenProject project, final File platformDir)
   {
      final CopyTargetPlatformResolutionHandler resolutionHandler = new CopyTargetPlatformResolutionHandler(platformDir);
      sessionResolver.resolve(session, true, resolutionHandler, resolutionHandler);

      // resolveTargetPlatformConfiguration(session, resolutionHandler);
      // resolveTargetPlatform(session, getMetadataDir(platformDir), resolutionHandler);

      final String executionEnvironment = selectExecutionEnvironment(resolutionHandler.getExecutionEnvironments());
      writeDefinitions(project, platformDir, executionEnvironment, resolutionHandler.getTargetEnvironments());
   }

   private void resolveTargetPlatformConfiguration(MavenSession session, TargetPlatformConfigurationHandler handler)
   {
      for (MavenProject project : session.getProjects())
      {
         tpResolver.resolveTargetPlatformConfiguration(session, project, handler);
      }
   }

   private void resolveTargetPlatform(MavenSession session, File metadataDir,
      final TargetPlatformResolutionHandler handler)
   {
      for (MavenProject project : session.getProjects())
      {
         resolveTargetPlatform(session, project, metadataDir, handler);
      }
   }

   private void resolveTargetPlatform(MavenSession session, MavenProject project, File metadataDir,
      final TargetPlatformResolutionHandler handler) throws Error
   {
      if (isResolutionRequired(metadataDir, session, project))
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

   private boolean isResolutionRequired(File metadataDir, MavenSession session, MavenProject project)
   {
      if (changeDiscoverer.hasTargetPlatformConfigurationChanged(metadataDir, session, project))
      {
         return true;
      }
      return forceUpdate;
   }

   protected void writeDefinitions(MavenProject project, File parentDir, String executionEnvironment,
      Collection<TargetEnvironment> targetEnvironments)
   {
      for (TargetEnvironment targetEnvironment : targetEnvironments)
      {
         final String platformName = getTargetPlatformDefinitionName(project, targetEnvironment);

         final File targetFile = new File(parentDir, platformName + ".target");

         new TargetPlatformWriter().write(targetFile, platformName, parentDir.getAbsolutePath(), targetEnvironment,
            executionEnvironment);
      }
   }

   private String getTargetPlatformDefinitionName(MavenProject project, TargetEnvironment targetEnvironment)
   {
      final StringBuilder sb = new StringBuilder();
      sb.append(getClassifiedName(project));
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

   protected String selectExecutionEnvironment(Collection<String> executionEnvironments)
   {
      return eeSelector.select(executionEnvironments);
   }

   public Logger getLogger()
   {
      return logger;
   }

   public boolean isForceUpdate()
   {
      return forceUpdate;
   }

   protected File getPlatformZipFile(MavenProject project)
   {
      return new File(targetDir, getClassifiedName(project) + ".zip");
   }

   protected File downloadTargetPlatformOnDemand(MavenProject project)
   {
      final File platformDir = getPlatformDir(project);
      if (!platformDir.exists())
      {
         download(session, project, platformDir.getParentFile());
      }
      return platformDir;
   }

   protected File getPlatformDir(MavenProject project)
   {
      return new File(targetDir, getClassifiedName(project));
   }

   protected String getClassifiedName(MavenProject project)
   {
      return getFinalName(project) + "-" + classifier;
   }

   protected String getFinalName(MavenProject project)
   {
      String finalName = project.getBuild().getFinalName();
      if (finalName == null)
      {
         finalName = project.getArtifactId() + "-" + project.getVersion();
      }
      return finalName;
   }


   protected File getMetadataDir(final File platformDir)
   {
      return new File(platformDir, ".tpmp");
   }
}
