/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp.change;

import static org.sourcepit.common.utils.io.IOResources.buffIn;
import static org.sourcepit.common.utils.io.IOResources.byteIn;
import static org.sourcepit.common.utils.io.IOResources.fileIn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.maven.project.MavenProject;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.io.IOResource;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.props.LinkedPropertiesMap;
import org.sourcepit.common.utils.props.PropertiesMap;

@Named
public class ChecksumTargetPlatformConfigurationChangeDiscoverer implements TargetPlatformConfigurationChangeDiscoverer
{
   private final TargetPlatformConfigurationFilesDiscoverer configFilesDiscoverer;

   @Inject
   public ChecksumTargetPlatformConfigurationChangeDiscoverer(
      TargetPlatformConfigurationFilesDiscoverer configFilesDiscoverer)
   {
      this.configFilesDiscoverer = configFilesDiscoverer;
   }

   public boolean hasTargetPlatformConfigurationChanged(File statusCacheDir, MavenProject project)
   {
      // always update project checksums
      // get last project checksum
      final String lastChecksum = getProjectChecksum(statusCacheDir, project);
      // compute new project checksum
      final String newChecksum = computeProjectChecksum(project);
      // compare
      if (lastChecksum == null || !newChecksum.equals(lastChecksum))
      {
         // persist new checksum if changed
         setProjectChecksum(statusCacheDir, project, newChecksum);
         return true;
      }

      return false;
   }

   public void clearTargetPlatformConfigurationStausCache(File statusCacheDir, MavenProject project)
   {

   }

   private String computeProjectChecksum(MavenProject project)
   {
      final List<MavenProject> projects = new ArrayList<MavenProject>();
      projects.add(project);

      MavenProject parent = project.getParent();
      while (parent != null)
      {
         projects.add(parent);
         parent = parent.getParent();
      }

      return computeProjectsChecksum(projects);
   }

   private String computeProjectsChecksum(List<MavenProject> projects)
   {
      final StringBuilder sb = new StringBuilder();
      for (MavenProject project : projects)
      {
         final List<File> files = configFilesDiscoverer.getTargetPlatformConfigurationFiles(project);
         for (File file : files)
         {
            sb.append(calculateHash(file));
         }
      }

      try
      {
         return calculateHash(sb.toString().getBytes("ASCII"));
      }
      catch (UnsupportedEncodingException e)
      {
         throw Exceptions.pipe(e);
      }
   }

   private static String calculateHash(File file)
   {
      final MessageDigest sha1;
      try
      {
         sha1 = MessageDigest.getInstance("SHA1");
      }
      catch (NoSuchAlgorithmException e)
      {
         throw Exceptions.pipe(e);
      }
      return calculateHash(sha1, buffIn(fileIn(file)));
   }

   private static String calculateHash(byte[] bytes)
   {
      final MessageDigest sha1;
      try
      {
         sha1 = MessageDigest.getInstance("SHA1");
      }
      catch (NoSuchAlgorithmException e)
      {
         throw Exceptions.pipe(e);
      }
      return calculateHash(sha1, buffIn(byteIn(bytes)));
   }

   private static String calculateHash(final MessageDigest algorithm, final IOResource<? extends InputStream> ioResource)
   {
      new IOOperation<InputStream>(ioResource)
      {
         @Override
         protected void run(InputStream openResource) throws IOException
         {
            final DigestInputStream dis = new DigestInputStream(openResource, algorithm);
            // read the file and update the hash calculation
            while (dis.read() != -1)
               ;
         }
      }.run();

      // get the hash value as byte array
      byte[] hash = algorithm.digest();

      return byteArray2Hex(hash);
   }

   private static String byteArray2Hex(byte[] hash)
   {
      Formatter formatter = null;
      try
      {
         formatter = new Formatter();
         for (byte b : hash)
         {
            formatter.format("%02x", b);
         }
         return formatter.toString();
      }
      finally
      {
         IOUtils.closeQuietly(formatter);
      }
   }

   private String getProjectChecksum(File statusCacheDir, MavenProject project)
   {
      final File checksumFile = new File(statusCacheDir, "project-status.properties");
      if (checksumFile.exists())
      {
         final PropertiesMap properties = new LinkedPropertiesMap();
         properties.load(checksumFile);
         return properties.get(project.getId());
      }
      return null;
   }

   private void setProjectChecksum(File statusCacheDir, MavenProject project, String checksum)
   {
      final File checksumFile = new File(statusCacheDir, "project-status.properties");
      final PropertiesMap properties = new LinkedPropertiesMap();
      if (checksumFile.exists())
      {
         properties.load(checksumFile);
      }
      else
      {
         checksumFile.getParentFile().mkdirs();
         try
         {
            checksumFile.createNewFile();
         }
         catch (IOException e)
         {
            throw Exceptions.pipe(e);
         }
      }
      properties.put(project.getId(), checksum);
      properties.store(checksumFile);
   }
}
