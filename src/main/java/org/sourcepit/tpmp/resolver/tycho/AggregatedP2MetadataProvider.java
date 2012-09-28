/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver.tycho;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

@Component(role = P2MetadataProvider.class)
public class AggregatedP2MetadataProvider implements P2MetadataProvider
{
   @SuppressWarnings("unchecked")
   public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
      List<Map<String, String>> environments, OptionalResolutionAction optionalAction)
   {
      final Map<String, IDependencyMetadata> metadata = (Map<String, IDependencyMetadata>) project.getContextValue("tpmp.aggregatedMetadata");
      return metadata;
   }

}
