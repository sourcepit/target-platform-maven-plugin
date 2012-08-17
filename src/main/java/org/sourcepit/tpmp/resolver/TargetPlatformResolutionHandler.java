/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver;

import java.io.File;

import org.apache.maven.project.MavenProject;

public interface TargetPlatformResolutionHandler
{
   void handleFeature(String id, String version, File location, MavenProject mavenProject);

   void handlePlugin(String id, String version, File location, boolean unpack, MavenProject mavenProject);
}
