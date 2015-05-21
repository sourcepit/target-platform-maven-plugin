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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.sourcepit.tpmp.resolver.ProjectTargetPlatformResolver;
import org.sourcepit.tpmp.resolver.TargetPlatformConfigurationHandler;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;

@Named
public class TychoProjectTargetPlatformResolver extends AbstractTychoTargetPlatformResolver
   implements
      ProjectTargetPlatformResolver {
   @Override
   public void resolveTargetPlatformConfiguration(MavenSession session, MavenProject project,
      TargetPlatformConfigurationHandler handler) {
      if (getTychoProject(project) == null) {
         return;
      }

      final TargetPlatformConfiguration configuration = getTargetPlatformConfiguration(session, project);

      for (TargetEnvironment te : configuration.getEnvironments()) {
         handler.handleTargetEnvironment(te.getOs(), te.getWs(), te.getArch());
      }

      final String ee = configuration.getExecutionEnvironment();
      if (ee != null) {
         handler.handleExecutionEnvironment(ee);
      }
   }

   @Override
   public void resolveTargetPlatform(MavenSession session, MavenProject project, boolean includeSource,
      TargetPlatformResolutionHandler handler) {
      if (getTychoProject(project) == null) {
         return;
      }

      final TargetPlatformConfiguration configuration = getTargetPlatformConfiguration(session, project);

      final List<Dependency> extraRequirements = new ArrayList<Dependency>();
      final Set<String> explodedBundles = new HashSet<String>();
      final List<Dependency> frameworkExtensions = new ArrayList<Dependency>();
      extraRequirements.addAll(configuration.getDependencyResolverConfiguration().getExtraRequirements());

      final TychoSurefirePluginConfiguration surefireConfiguration = new TychoSurefirePluginConfigurationReader().read(project);
      if (surefireConfiguration != null) {
         extraRequirements.addAll(surefireConfiguration.getDependencies());
         explodedBundles.addAll(surefireConfiguration.getExplodedBundles());
         frameworkExtensions.addAll(surefireConfiguration.getFrameworkExtensions());
      }

      final ContentCollector contentCollector = new ContentCollector(handler);

      doResolve(session, project, DefaultReactorProject.adapt(session), includeSource, explodedBundles,
         extraRequirements, frameworkExtensions, contentCollector);
   }
}
