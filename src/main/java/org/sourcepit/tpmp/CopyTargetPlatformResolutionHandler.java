/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.zip.ZipProcessingRequest;
import org.sourcepit.common.utils.zip.ZipProcessor;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;

public class CopyTargetPlatformResolutionHandler implements TargetPlatformResolutionHandler
{
   private final Collection<String> executionEnvironments = new LinkedHashSet<String>();
   private final Collection<TargetEnvironment> targetEnvironments = new LinkedHashSet<TargetEnvironment>();

   private final File platformDir;
   private final File featuresDir;
   private final File pluginsDir;

   public CopyTargetPlatformResolutionHandler(File targetDir)
   {
      this.platformDir = targetDir;
      featuresDir = new File(targetDir, "features");
      pluginsDir = new File(targetDir, "plugins");
   }

   public File getPlatformDir()
   {
      return platformDir;
   }

   public File getFeaturesDir()
   {
      return featuresDir;
   }

   public File getPluginsDir()
   {
      return pluginsDir;
   }

   public Collection<TargetEnvironment> getTargetEnvironments()
   {
      return targetEnvironments;
   }

   public Collection<String> getExecutionEnvironments()
   {
      return executionEnvironments;
   }

   public void handleTargetEnvironment(@NotNull String os, @NotNull String ws, @NotNull String arch, String nl)
   {
      targetEnvironments.add(new TargetEnvironment(os, ws, arch, nl));
   }

   public void handleExecutionEnvironment(@NotNull String ee)
   {
      executionEnvironments.add(ee);
   }

   public void handleFeature(@NotNull String id, @NotNull String version, @NotNull File location,
      MavenProject mavenProject)
   {
      if (mavenProject == null)
      {
         processFeature(id, version, location);
      }
   }

   public void handlePlugin(@NotNull String id, @NotNull String version, @NotNull File location, boolean unpack,
      MavenProject mavenProject)
   {
      if (mavenProject == null)
      {
         processPlugin(id, version, location, unpack);
      }
   }

   private void processPlugin(String id, String version, File location, boolean unpack)
   {
      final File pluginDir = newPluginDir(id, version);
      final File pluginJar = newPluginJar(id, version);

      try
      {
         if (location.isFile())
         {
            if (unpack)
            {
               if (pluginJar.exists())
               {
                  FileUtils.forceDelete(pluginJar);
               }
               if (!pluginDir.exists())
               {
                  unpack(location, pluginDir);
               }
            }
            else
            {
               if (!pluginJar.exists() && !pluginDir.exists())
               {
                  FileUtils.copyFile(location, pluginJar);
               }
            }
         }
         else
         {
            if (pluginJar.exists())
            {
               FileUtils.forceDelete(pluginJar);
            }
            if (!pluginDir.exists())
            {
               FileUtils.copyDirectory(location, pluginDir);
            }
         }
      }
      catch (IOException e)
      {
         throw Exceptions.pipe(e);
      }
   }

   private void processFeature(String id, String version, File location)
   {
      final File featureDir = newFeatureDir(id, version);
      if (featureDir.exists())
      {
         return;
      }

      try
      {
         unpack(location, featureDir);
      }
      catch (IOException e)
      {
         throw Exceptions.pipe(e);
      }

   }

   private void unpack(File srcFile, final File destDir) throws IOException
   {
      final ZipProcessingRequest request = ZipProcessingRequest.newUnzipRequest(srcFile, destDir);
      new ZipProcessor().process(request);
   }

   private File newFeatureDir(String id, String version)
   {
      return new File(featuresDir, getVersionedid(id, version));
   }

   private File newPluginDir(String id, String version)
   {
      return new File(pluginsDir, getVersionedid(id, version));
   }

   private File newPluginJar(String id, String version)
   {
      return new File(pluginsDir, getVersionedid(id, version) + ".jar");
   }

   private String getVersionedid(String id, String version)
   {
      return id + "_" + version;
   }
}
