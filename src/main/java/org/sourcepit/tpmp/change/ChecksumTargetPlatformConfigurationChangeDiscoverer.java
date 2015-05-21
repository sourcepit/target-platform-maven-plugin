/*
 * Copyright 2014 Bernd Vogt and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sourcepit.tpmp.change;

import static org.sourcepit.common.utils.io.IO.buffIn;
import static org.sourcepit.common.utils.io.IO.byteIn;
import static org.sourcepit.common.utils.io.IO.fileIn;

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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.sourcepit.common.utils.charset.CharsetDetectionResult;
import org.sourcepit.common.utils.charset.CharsetDetector;
import org.sourcepit.common.utils.io.IOHandle;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.path.PathUtils;
import org.sourcepit.common.utils.props.LinkedPropertiesMap;
import org.sourcepit.common.utils.props.PropertiesMap;
import org.sourcepit.tpmp.ToolUtils;

@Named
public class ChecksumTargetPlatformConfigurationChangeDiscoverer implements TargetPlatformConfigurationChangeDiscoverer {
   private final CharsetDetector charsetDetector;

   private final Map<String, TargetPlatformConfigurationFilesDiscoverer> configFilesDiscovererMap;

   @Inject
   public ChecksumTargetPlatformConfigurationChangeDiscoverer(CharsetDetector charsetDetector,
      Map<String, TargetPlatformConfigurationFilesDiscoverer> configFilesDiscovererMap) {
      this.charsetDetector = charsetDetector;
      this.configFilesDiscovererMap = configFilesDiscovererMap;
   }

   @Override
   public boolean hasTargetPlatformConfigurationChanged(File statusCacheDir, MavenSession session, MavenProject project) {
      // always update project checksums
      // get last project checksum
      final String lastChecksum = getProjectChecksum(statusCacheDir, project);
      // compute new project checksum
      final String newChecksum = computeProjectChecksum(statusCacheDir, session, project);
      // compare
      if (lastChecksum == null || !newChecksum.equals(lastChecksum)) {
         // persist new checksum if changed
         setProjectChecksum(statusCacheDir, project, newChecksum);
         return true;
      }

      return false;
   }

   @Override
   public void clearTargetPlatformConfigurationStausCache(File statusCacheDir, MavenProject project) {
      final File checksumFile = new File(statusCacheDir, "project-status.properties");
      final PropertiesMap properties = new LinkedPropertiesMap();
      if (checksumFile.exists()) {
         properties.load(checksumFile);
         properties.remove(project.getId());
         properties.store(checksumFile);
      }
   }

   private String computeProjectChecksum(File statusCacheDir, MavenSession session, MavenProject project) {
      final List<MavenProject> projects = new ArrayList<MavenProject>();
      projects.add(project);

      MavenProject parent = project.getParent();
      while (parent != null) {
         projects.add(parent);
         parent = parent.getParent();
      }

      return computeProjectsChecksum(statusCacheDir, session, projects);
   }

   private String getDefaultEncoding(MavenProject project) {
      return project.getProperties().getProperty("project.build.sourceEncoding", Charset.defaultCharset().name());
   }

   private String computeProjectsChecksum(File statusCacheDir, MavenSession session, List<MavenProject> projects) {
      final StringBuilder sb = new StringBuilder();
      for (final MavenProject project : projects) {
         final List<File> files = getTPFilesDiscoverer(session, project).getTargetPlatformConfigurationFiles(session,
            project);
         for (final File file : files) {
            final String encoding = detectEncoding(project, file);
            final String hash = calculateHash(file, encoding);
            final String path = PathUtils.getRelativePath(file, new File("").getAbsoluteFile(), "/");

            dump(statusCacheDir, path, encoding, hash);

            sb.append(hash);
         }
      }

      try {
         return calculateHash(sb.toString().getBytes("ASCII"), "ASCII");
      }
      catch (UnsupportedEncodingException e) {
         throw Exceptions.pipe(e);
      }
   }

   private TargetPlatformConfigurationFilesDiscoverer getTPFilesDiscoverer(MavenSession session, MavenProject project) {
      final String tool = ToolUtils.getTool(session, project);
      if (tool == null) {
         throw new IllegalStateException("Property tpmp.tool is not set");
      }
      return configFilesDiscovererMap.get(tool);
   }

   private void dump(File statusCacheDir, String path, String encoding, String hash) {
      final File checksumFile = new File(statusCacheDir, "dump.properties");
      final PropertiesMap properties = new LinkedPropertiesMap();
      if (checksumFile.exists()) {
         properties.load(checksumFile);
      }
      else {
         checksumFile.getParentFile().mkdirs();
         try {
            checksumFile.createNewFile();
         }
         catch (IOException e) {
            throw Exceptions.pipe(e);
         }
      }
      properties.put(path + "@" + encoding, hash);
      properties.store(checksumFile);
   }

   private String detectEncoding(final MavenProject project, final File file) {
      final CharsetDetectionResult[] result = new CharsetDetectionResult[1];

      new IOOperation<InputStream>(fileIn(file)) {
         @Override
         protected void run(InputStream openResource) throws IOException {
            result[0] = charsetDetector.detect(file.getName(), openResource, getDefaultEncoding(project));
         }
      }.run();

      return result[0].getRecommendedCharset().name();
   }

   private static String calculateHash(File file, String encoding) {
      final MessageDigest sha1;
      try {
         sha1 = MessageDigest.getInstance("SHA1");
      }
      catch (NoSuchAlgorithmException e) {
         throw Exceptions.pipe(e);
      }

      return calculateHash(sha1, buffIn(fileIn(file)), encoding);
   }

   private static String calculateHash(byte[] bytes, String encoding) {
      final MessageDigest sha1;
      try {
         sha1 = MessageDigest.getInstance("SHA1");
      }
      catch (NoSuchAlgorithmException e) {
         throw Exceptions.pipe(e);
      }
      return calculateHash(sha1, buffIn(byteIn(bytes)), encoding);
   }

   private static String calculateHash(final MessageDigest algorithm, final IOHandle<? extends InputStream> ioResource,
      final String encoding) {
      Reader reader = null;
      InputStream inputStream = null;
      try {
         inputStream = ioResource.open();
         reader = new WhitespaceFilterReader(new InputStreamReader(inputStream, encoding));

         int ch = reader.read();
         while (ch > -1) {
            algorithm.update((byte) ((ch & 0xFF00) >> 8));
            algorithm.update((byte) (ch & 0x00FF));
            ch = reader.read();
         }
      }
      catch (IOException e) {
         throw Exceptions.pipe(e);
      }
      finally {
         IOUtils.closeQuietly(reader);
      }

      // get the hash value as byte array
      byte[] hash = algorithm.digest();

      return byteArray2Hex(hash);
   }

   private static final class WhitespaceFilterReader extends FilterReader {
      private WhitespaceFilterReader(Reader in) {
         super(in);
      }

      @Override
      public long skip(long n) throws IOException {
         if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
         }

         for (long i = 0; i < n; i++) {
            if (read() == -1) {
               return i;
            }
         }
         return n;
      }

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
         for (int i = 0; i < len; i++) {
            int ch = read();
            if (ch == -1) {
               if (i == 0) {
                  return -1;
               }
               else {
                  return i;
               }
            }
            cbuf[off + i] = (char) ch;
         }
         return len;
      }

      @Override
      public int read() throws IOException {
         int ch = super.read();
         while (Character.isWhitespace(ch)) {
            ch = super.read();
         }
         return ch;
      }
   }

   private static String byteArray2Hex(byte[] hash) {
      Formatter formatter = null;
      try {
         formatter = new Formatter();
         for (byte b : hash) {
            formatter.format("%02x", Byte.valueOf(b));
         }
         return formatter.toString();
      }
      finally {
         IOUtils.closeQuietly(formatter);
      }
   }

   private String getProjectChecksum(File statusCacheDir, MavenProject project) {
      final File checksumFile = new File(statusCacheDir, "project-status.properties");
      if (checksumFile.exists()) {
         final PropertiesMap properties = new LinkedPropertiesMap();
         properties.load(checksumFile);
         return properties.get(project.getId());
      }
      return null;
   }

   private void setProjectChecksum(File statusCacheDir, MavenProject project, String checksum) {
      final File checksumFile = new File(statusCacheDir, "project-status.properties");
      final PropertiesMap properties = new LinkedPropertiesMap();
      if (checksumFile.exists()) {
         properties.load(checksumFile);
      }
      else {
         checksumFile.getParentFile().mkdirs();
         try {
            checksumFile.createNewFile();
         }
         catch (IOException e) {
            throw Exceptions.pipe(e);
         }
      }
      properties.put(project.getId(), checksum);
      properties.store(checksumFile);
   }
}
