/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.sourcepit.tpmp.resolver.ProjectTargetPlatformResolver;
import org.sourcepit.tpmp.resolver.TargetPlatformConfigurationHandler;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;

@Named
public class TychoProjectTargetPlatformResolver extends AbstractTychoTargetPlatformResolver
   implements
      ProjectTargetPlatformResolver
{
   @Override
   public void resolveTargetPlatformConfiguration(MavenSession session, MavenProject project,
      TargetPlatformConfigurationHandler handler)
   {
      if (getTychoProject(project) == null)
      {
         return;
      }

      final TargetPlatformConfiguration configuration = getTargetPlatformConfiguration(session, project);

      for (TargetEnvironment te : configuration.getEnvironments())
      {
         handler.handleTargetEnvironment(te.getOs(), te.getWs(), te.getArch());
      }

      final String ee = configuration.getExecutionEnvironment();
      if (ee != null)
      {
         handler.handleExecutionEnvironment(ee);
      }
   }

   @Override
   public void resolveTargetPlatform(MavenSession session, MavenProject project, boolean includeSource,
      TargetPlatformResolutionHandler handler)
   {
      if (getTychoProject(project) == null)
      {
         return;
      }

      final TargetPlatformConfiguration configuration = getTargetPlatformConfiguration(session, project);

      final List<Dependency> extraRequirements = new ArrayList<Dependency>();
      final Set<String> explodedBundles = new HashSet<String>();
      final List<Dependency> frameworkExtensions = new ArrayList<Dependency>();
      extraRequirements.addAll(configuration.getDependencyResolverConfiguration().getExtraRequirements());

      final TychoSurefirePluginConfiguration surefireConfiguration = new TychoSurefirePluginConfigurationReader()
         .read(project);
      if (surefireConfiguration != null)
      {
         extraRequirements.addAll(surefireConfiguration.getDependencies());
         explodedBundles.addAll(surefireConfiguration.getExplodedBundles());
         frameworkExtensions.addAll(surefireConfiguration.getFrameworkExtensions());
      }

      final ContentCollector contentCollector = new ContentCollector(handler);

      doResolve(session, project, DefaultReactorProject.adapt(session), includeSource, explodedBundles,
         extraRequirements, frameworkExtensions, contentCollector);
   }
}
