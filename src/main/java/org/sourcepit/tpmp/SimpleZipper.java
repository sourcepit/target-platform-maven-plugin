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

package org.sourcepit.tpmp;

import static org.sourcepit.common.utils.io.IO.buffIn;
import static org.sourcepit.common.utils.io.IO.fileIn;
import static org.sourcepit.common.utils.io.IO.fileOut;
import static org.sourcepit.common.utils.io.IO.zipOut;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.sourcepit.common.utils.file.FileVisitor;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.lang.Exceptions;
import org.sourcepit.common.utils.path.PathUtils;

public class SimpleZipper
{
   public void zip(final File platformDir, File platformZipFile, final String pathPrefix)
   {
      new IOOperation<ZipOutputStream>(zipOut(fileOut(platformZipFile, true)))
      {
         @Override
         protected void run(final ZipOutputStream zipOut) throws IOException
         {
            org.sourcepit.common.utils.file.FileUtils.accept(platformDir, new FileVisitor()
            {
               @Override
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
}
