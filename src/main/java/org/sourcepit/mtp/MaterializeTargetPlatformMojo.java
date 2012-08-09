/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.guplex.Guplex;
import org.sourcepit.mtp.ee.ExecutionEnvironmentSelector;
import org.sourcepit.mtp.resolver.TargetPlatformResolver;
import org.sourcepit.mtp.te.TargetEnvironment;
import org.sourcepit.mtp.te.TargetEnvironmentSelector;

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

   /** @component */
   private Guplex guplex;

   @Inject
   private TargetPlatformResolver tpResolver;

   @Inject
   private ExecutionEnvironmentSelector eeSelector;

   @Inject
   private TargetEnvironmentSelector teSelector;

   public final void execute() throws MojoExecutionException, MojoFailureException
   {
      guplex.inject(this, true);
      doExecute();
   }

   private void doExecute() throws MojoExecutionException, MojoFailureException
   {
      final File platformDir = getCleanPlatformDir();

      final CopyTargetPlatformHandler handler = new CopyTargetPlatformHandler(platformDir);

      final List<MavenProject> projects = session.getProjects();
      for (MavenProject project : projects)
      {
         tpResolver.resolveTargetPlatform(session, project, handler);
      }

      final TargetEnvironment selectedTE = teSelector.select(handler.getTargetEnvironments());

      final String selectedEE = eeSelector.select(handler.getExecutionEnvironments());

      final File targetFile = new File(platformDir, name + ".target");
      new TargetPlatformWriter().write(targetFile, name, platformDir, selectedTE, selectedEE);
   }

   private File getCleanPlatformDir()
   {
      final File platformDir = new File(targetDir, "target-platform");
      if (platformDir.exists())
      {
         try
         {
            FileUtils.forceDelete(platformDir);
         }
         catch (IOException e)
         {
            throw Exceptions.pipe(e);
         }
      }
      return platformDir;
   }
}
