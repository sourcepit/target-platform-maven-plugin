/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.change;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public interface TargetPlatformConfigurationChangeDiscoverer
{
   boolean hasTargetPlatformConfigurationChanged(File statusCacheDir, MavenSession session, MavenProject project);

   void clearTargetPlatformConfigurationStausCache(File statusCacheDir, MavenProject project);
}