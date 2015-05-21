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

package org.sourcepit.tpmp.resolver.tycho;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;

@Named
public class AggregatedP2MetadataProvider implements P2MetadataProvider {
   @Override
   public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
      List<TargetEnvironment> environments, OptionalResolutionAction optional) {
      @SuppressWarnings("unchecked")
      final Map<String, IDependencyMetadata> metadata = (Map<String, IDependencyMetadata>) project.getContextValue("tpmp.aggregatedMetadata");
      return metadata;
   }

}
