/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp;

import java.io.File;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.sourcepit.common.utils.path.PathUtils;
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

   /** @parameter expression="${mtp.forceResolution}" default-value="false" */
   private boolean forceResolution;

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

   public final void execute() throws MojoExecutionException, MojoFailureException
   {
      guplex.inject(this, true);
      doExecute();
   }

   private void doExecute() throws MojoExecutionException, MojoFailureException
   {
      final File platformDir = getPlatformDir();

      final CopyTargetPlatformResolutionHandler resolutionHandler = new CopyTargetPlatformResolutionHandler(platformDir);
      resolve(new File(platformDir, ".mtp"), resolutionHandler);

      final String executionEnvironment = eeSelector.select(resolutionHandler.getExecutionEnvironments());

      writeDefinitions(platformDir, executionEnvironment, resolutionHandler.getTargetEnvironments());

      new SimpleZipper().zip(platformDir, getPlatformZipFile(), getFinalName());
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
      return forceResolution;
   }

   private File getPlatformZipFile()
   {
      return new File(targetDir, getFinalName() + ".zip");
   }

   private File getPlatformDir()
   {
      return new File(targetDir, "target-platform");
   }

   private String getFinalName()
   {
      return "target-platform-" + session.getCurrentProject().getVersion() + "-target";
   }
}
