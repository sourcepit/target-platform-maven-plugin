/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sourcepit.common.utils.lang.PipedException;
import org.sourcepit.guplex.Guplex;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public abstract class AbstractGuplexedMojo extends AbstractMojo
{
   /** @component */
   private Guplex guplex;

   public final void execute() throws MojoExecutionException, MojoFailureException
   {
      guplex.inject(this, true);
      try
      {
         doExecute();
      }
      catch (PipedException e)
      {
         e.adaptAndThrow(MojoExecutionException.class);
         e.adaptAndThrow(MojoFailureException.class);
         throw e;
      }
   }

   protected abstract void doExecute();

}
