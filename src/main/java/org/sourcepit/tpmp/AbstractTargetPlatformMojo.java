/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.sourcepit.tpmp.ee.ExecutionEnvironmentSelector;
import org.sourcepit.tpmp.resolver.TargetPlatformResolver;

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

   /** @parameter expression="${tpmp.resolutionStrategy}" default-value="per-session" */
   protected String resolutionStrategy;

   @Inject
   protected RepositorySystem repositorySystem;

   @Inject
   private ExecutionEnvironmentSelector eeSelector;

   @Inject
   private Map<String, TargetPlatformResolver> resolverMap;

   protected Artifact createPlatformArtifact(MavenProject project)
   {
      final Artifact platformArtifact = repositorySystem.createArtifactWithClassifier(project.getGroupId(),
         project.getArtifactId(), project.getVersion(), "zip", classifier);
      return platformArtifact;
   }

   protected void updateTargetPlatform(final MavenProject project, final File platformDir)
   {
      final TargetPlatformResolver resolver = getResolver();

      final CopyTargetPlatformResolutionHandler resolutionHandler = new CopyTargetPlatformResolutionHandler(platformDir);
      resolver.resolve(session, platformDir, includeSource, forceUpdate, resolutionHandler, resolutionHandler);

      final String executionEnvironment = selectExecutionEnvironment(resolutionHandler.getExecutionEnvironments());
      writeDefinitions(project, platformDir, executionEnvironment, resolutionHandler.getTargetEnvironments());
   }

   protected TargetPlatformResolver getResolver()
   {
      final TargetPlatformResolver resolver = resolverMap.get(resolutionStrategy);
      if (resolver == null)
      {
         throw new IllegalStateException("No resolver available for strategy '" + resolutionStrategy + "'");
      }
      return resolver;
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

   protected File getPlatformZipFile(MavenProject project)
   {
      return new File(targetDir, getClassifiedName(project) + ".zip");
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
}
