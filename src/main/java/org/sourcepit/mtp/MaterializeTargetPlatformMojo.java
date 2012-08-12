/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp;

import static org.sourcepit.common.utils.io.IOResources.buffIn;
import static org.sourcepit.common.utils.io.IOResources.fileIn;
import static org.sourcepit.common.utils.io.IOResources.fileOut;
import static org.sourcepit.common.utils.io.IOResources.zipOut;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.sourcepit.common.utils.file.FileVisitor;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.lang.Exceptions;
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

      pack(platformDir, getPlatformZipFile(), getFinalName());
   }

   private void pack(final File platformDir, File platformZipFile, final String pathPrefix)
   {
      new IOOperation<ZipOutputStream>(zipOut(fileOut(platformZipFile, true)))
      {
         @Override
         protected void run(final ZipOutputStream zipOut) throws IOException
         {
            org.sourcepit.common.utils.file.FileUtils.accept(platformDir, new FileVisitor()
            {
               public boolean visit(File file)
               {
                  if (!file.equals(platformDir))
                  {
                     try
                     {
                        String path = PathUtils.getRelativePath(file, platformDir, "/");
                        if (pathPrefix != null)
                        {
                           path = pathPrefix + "/" + path;
                        }
                        pack(zipOut, file, path);
                     }
                     catch (IOException e)
                     {
                        throw Exceptions.pipe(e);
                     }
                  }
                  return true;
               }

               private void pack(final ZipOutputStream zipOut, File file, final String path) throws IOException
               {
                  if (file.isDirectory())
                  {
                     ZipEntry entry = new ZipEntry(path + "/");
                     zipOut.putNextEntry(entry);
                     zipOut.closeEntry();
                  }
                  else
                  {
                     ZipEntry entry = new ZipEntry(path);
                     entry.setSize(file.length());
                     zipOut.putNextEntry(entry);
                     new IOOperation<InputStream>(buffIn(fileIn(file)))
                     {
                        @Override
                        protected void run(InputStream openFile) throws IOException
                        {
                           IOUtils.copy(openFile, zipOut);
                        }
                     }.run();
                     zipOut.closeEntry();
                  }
               }
            });
         }
      }.run();
   }

   private void writeDefinitions(File platformDir, String executionEnvironment,
      Collection<TargetEnvironment> targetEnvironments)
   {
      for (TargetEnvironment targetEnvironment : targetEnvironments)
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

         final String platformName = sb.toString();

         sb.append(".target");

         final File targetFile = new File(platformDir, sb.toString());

         String relativePath = PathUtils.getRelativePath(targetFile.getParentFile(), platformDir, "/");
         if (relativePath == null || relativePath.length() == 0)
         {
            relativePath = ".";
         }

         new TargetPlatformWriter().write(targetFile, platformName, relativePath, targetEnvironment,
            executionEnvironment);
      }
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
