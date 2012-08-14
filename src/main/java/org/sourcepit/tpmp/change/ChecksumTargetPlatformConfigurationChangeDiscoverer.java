/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.change;

import static org.sourcepit.common.utils.io.IOResources.buffIn;
import static org.sourcepit.common.utils.io.IOResources.byteIn;
import static org.sourcepit.common.utils.io.IOResources.fileIn;

import java.io.File;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.maven.project.MavenProject;
import org.sourcepit.common.utils.charset.CharsetDetectionResult;
import org.sourcepit.common.utils.charset.CharsetDetector;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.io.IOResource;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.props.LinkedPropertiesMap;
import org.sourcepit.common.utils.props.PropertiesMap;

@Named
public class ChecksumTargetPlatformConfigurationChangeDiscoverer implements TargetPlatformConfigurationChangeDiscoverer
{
   private final CharsetDetector charsetDetector;

   private final TargetPlatformConfigurationFilesDiscoverer configFilesDiscoverer;

   @Inject
   public ChecksumTargetPlatformConfigurationChangeDiscoverer(CharsetDetector charsetDetector,
      TargetPlatformConfigurationFilesDiscoverer configFilesDiscoverer)
   {
      this.charsetDetector = charsetDetector;
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
      final File checksumFile = new File(statusCacheDir, "project-status.properties");
      final PropertiesMap properties = new LinkedPropertiesMap();
      if (checksumFile.exists())
      {
         properties.load(checksumFile);
         properties.remove(project.getId());
         properties.store(checksumFile);
      }
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

   private String getDefaultEncoding(MavenProject project)
   {
      return project.getProperties().getProperty("project.build.sourceEncoding", Charset.defaultCharset().name());
   }

   private String computeProjectsChecksum(List<MavenProject> projects)
   {
      final StringBuilder sb = new StringBuilder();
      for (final MavenProject project : projects)
      {
         final List<File> files = configFilesDiscoverer.getTargetPlatformConfigurationFiles(project);
         for (final File file : files)
         {
            sb.append(calculateHash(file, detectEncoding(project, file)));
         }
      }

      try
      {
         return calculateHash(sb.toString().getBytes("ASCII"), "ASCII");
      }
      catch (UnsupportedEncodingException e)
      {
         throw Exceptions.pipe(e);
      }
   }

   private String detectEncoding(final MavenProject project, final File file)
   {
      final CharsetDetectionResult[] result = new CharsetDetectionResult[1];

      new IOOperation<InputStream>(fileIn(file))
      {
         @Override
         protected void run(InputStream openResource) throws IOException
         {
            result[0] = charsetDetector.detect(file.getName(), openResource, getDefaultEncoding(project));
         }
      }.run();

      return result[0].getRecommendedCharset().name();
   }

   private static String calculateHash(File file, String encoding)
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

      return calculateHash(sha1, buffIn(fileIn(file)), encoding);
   }

   private static String calculateHash(byte[] bytes, String encoding)
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
      return calculateHash(sha1, buffIn(byteIn(bytes)), encoding);
   }

   private static String calculateHash(final MessageDigest algorithm,
      final IOResource<? extends InputStream> ioResource, final String encoding)
   {
      Reader reader = null;
      InputStream inputStream = null;
      try
      {
         inputStream = ioResource.open();
         reader = new WhitespaceFilterReader(new InputStreamReader(inputStream, encoding));

         int ch = reader.read();
         while (ch > -1)
         {
            algorithm.update((byte) ((ch & 0xFF00) >> 8));
            algorithm.update((byte) (ch & 0x00FF));
            ch = reader.read();
         }
      }
      catch (IOException e)
      {
         throw Exceptions.pipe(e);
      }
      finally
      {
         IOUtils.closeQuietly(reader);
      }

      // get the hash value as byte array
      byte[] hash = algorithm.digest();

      return byteArray2Hex(hash);
   }

   private static final class WhitespaceFilterReader extends FilterReader
   {
      private WhitespaceFilterReader(Reader in)
      {
         super(in);
      }

      @Override
      public long skip(long n) throws IOException
      {
         if (n < 0L)
         {
            throw new IllegalArgumentException("skip value is negative");
         }

         for (long i = 0; i < n; i++)
         {
            if (read() == -1)
            {
               return i;
            }
         }
         return n;
      }

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException
      {
         for (int i = 0; i < len; i++)
         {
            int ch = read();
            if (ch == -1)
            {
               if (i == 0)
               {
                  return -1;
               }
               else
               {
                  return i;
               }
            }
            cbuf[off + i] = (char) ch;
         }
         return len;
      }

      @Override
      public int read() throws IOException
      {
         int ch = super.read();
         while (Character.isWhitespace(ch))
         {
            ch = super.read();
         }
         return ch;
      }
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
