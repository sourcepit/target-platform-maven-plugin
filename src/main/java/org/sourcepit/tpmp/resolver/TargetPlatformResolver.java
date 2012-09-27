/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;


public interface TargetPlatformResolver
{
   void resolveTargetPlatformConfiguration(MavenSession session, MavenProject project,
      TargetPlatformConfigurationHandler handler);

   void resolveTargetPlatform(MavenSession session, MavenProject project, boolean includeSource,
      TargetPlatformResolutionHandler handler);
}