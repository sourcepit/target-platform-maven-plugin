/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
