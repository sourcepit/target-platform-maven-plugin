/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp;

import java.io.File;

import org.apache.maven.project.MavenProject;


/**
 * @goal localize
 * @requiresProject true
 * @aggregator
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public class LocalizeTargetPlatformMojo extends AbstractTargetPlatformMojo
{
   @Override
   protected void doExecute()
   {
      final MavenProject project = session.getCurrentProject();

      final File platformDir = downloadTargetPlatformOnDemand(project);

      updateTargetPlatform(project, platformDir);
   }
}
