/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver.tycho;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentConfigurationImpl;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.AbstractTychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.sourcepit.common.utils.props.LinkedPropertiesMap;
import org.sourcepit.common.utils.props.PropertiesMap;
import org.sourcepit.tpmp.ee.ExecutionEnvironmentSelector;
import org.sourcepit.tpmp.resolver.TargetPlatformConfigurationHandler;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;
import org.sourcepit.tpmp.resolver.TargetPlatformResolver;

@Named("per-session")
public class TychoSessionTargetPlatformResolver extends AbstractTychoTargetPlatformResolver
   implements
      TargetPlatformResolver
{
   @Inject
   private ExecutionEnvironmentSelector eeSelector;
   
   public boolean isRelyingOnCachedFiles()
   {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public void resolve(MavenSession session, File platformDir, boolean includeSource, boolean forceUpdate,
      TargetPlatformConfigurationHandler configHandler, TargetPlatformResolutionHandler resolutionHandler)
   {
      final TargetPlatformConfiguration aggregatedConfiguration = new TargetPlatformConfiguration();
      final LinkedHashSet<String> explodedBundles = new LinkedHashSet<String>();
      final LinkedHashSet<Dependency> frameworkExtensions = new LinkedHashSet<Dependency>();
      aggregateTargetPlatformConfigurations(session, aggregatedConfiguration, frameworkExtensions, explodedBundles);
      handleConfiguration(aggregatedConfiguration, configHandler);

      final MavenProject project = setupAggregatedProject(session, aggregatedConfiguration);

      final ContentCollector contentCollector = new ContentCollector(resolutionHandler);

      final List<ReactorProject> reactorProjects = new ArrayList<ReactorProject>();
      reactorProjects.add(DefaultReactorProject.adapt(project));

      final List<Dependency> extraRequirements = TychoProjectUtils.getTargetPlatformConfiguration(project)
         .getExtraRequirements();

      doResolve(session, project, reactorProjects, includeSource, explodedBundles, extraRequirements,
         frameworkExtensions, contentCollector);
   }

   private void handleConfiguration(final TargetPlatformConfiguration aggregatedConfiguration,
      TargetPlatformConfigurationHandler configHandler)
   {
      for (TargetEnvironment te : aggregatedConfiguration.getEnvironments())
      {
         configHandler.handleTargetEnvironment(te.getOs(), te.getWs(), te.getArch());
      }

      final String ee = aggregatedConfiguration.getExecutionEnvironment();
      if (ee != null)
      {
         configHandler.handleExecutionEnvironment(ee);
      }
   }

   private MavenProject setupAggregatedProject(MavenSession session, TargetPlatformConfiguration aggregatedConfiguration)
   {
      PropertiesMap mvnProperties = new LinkedPropertiesMap();
      mvnProperties.load(getClass().getClassLoader(), "META-INF/tpmp/maven.properties");

      String groupId = mvnProperties.get("groupId");
      String artifactId = mvnProperties.get("artifactId");
      String version = mvnProperties.get("version");

      final String tpmpKey = Plugin.constructKey(groupId, artifactId);

      MavenProject origin = session.getCurrentProject();

      Model model = origin.getModel().clone();
      Build build = model.getBuild();
      if (build.getPluginsAsMap().get(tpmpKey) == null)
      {
         Plugin tpmp = new Plugin();
         tpmp.setGroupId(groupId);
         tpmp.setArtifactId(artifactId);
         tpmp.setVersion(version);

         build.getPlugins().add(tpmp);
         build.flushPluginMap();
      }

      MavenProject fake = new MavenProject(model);
      fake.setClassRealm(origin.getClassRealm());
      fake.setFile(origin.getFile());
      fake.setRemoteArtifactRepositories(origin.getRemoteArtifactRepositories());
      fake.setPluginArtifactRepositories(origin.getPluginArtifactRepositories());
      fake.setManagedVersionMap(origin.getManagedVersionMap());

      if (getTychoProject(fake) == null)
      {
         fake.setPackaging("eclipse-repository");
      }

      fake.getBuildPlugins();

      AbstractTychoProject tychoProject = (AbstractTychoProject) getTychoProject(fake);
      tychoProject.setupProject(session, fake);

      Properties properties = new Properties();
      properties.putAll(fake.getProperties());
      properties.putAll(session.getSystemProperties()); // session wins
      properties.putAll(session.getUserProperties());
      fake.setContextValue(TychoConstants.CTX_MERGED_PROPERTIES, properties);

      fake.setContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION, aggregatedConfiguration);
      
      ExecutionEnvironmentConfiguration eeConfiguration = new ExecutionEnvironmentConfigurationImpl();
      tychoProject.readExecutionEnvironmentConfiguration(fake, eeConfiguration);
      fake.setContextValue(TychoConstants.CTX_EXECUTION_ENVIRONMENT_CONFIGURATION, eeConfiguration);

      Map<String, DependencyMetadata> metadata = new LinkedHashMap<String, DependencyMetadata>();

      Map<String, ReactorProject> vidToProjectMap = new HashMap<String, ReactorProject>();

      for (ReactorProject reactorProject : DefaultReactorProject.adapt(session))
      {
         String vid = reactorProject.getId() + "_" + reactorProject.getVersion();
         vidToProjectMap.put(vid, reactorProject);


         final Map<String, Set<Object>> dependencyMetadata = reactorProject.getDependencyMetadata();
         if (dependencyMetadata != null)
         {
            for (String classifier : dependencyMetadata.keySet())
            {
               DependencyMetadata dm = metadata.get(classifier);
               if (dm == null)
               {
                  dm = new DependencyMetadata();
                  metadata.put(classifier, dm);
               }

               mergeMetadata(dm, reactorProject, classifier, true);
               mergeMetadata(dm, reactorProject, classifier, false);
            }
         }
      }

      fake.setContextValue("tpmp.aggregatedMetadata", metadata);

      return fake;
   }

   private static class DependencyMetadata implements IDependencyMetadata
   {

      private Set<Object> metadata;
      private Set<Object> secondaryMetadata;

      public Set<Object /* IInstallableUnit */> getMetadata(boolean primary)
      {
         return primary ? metadata : secondaryMetadata;
      }

      public Set<Object /* IInstallableUnit */> getMetadata()
      {
         LinkedHashSet<Object> result = new LinkedHashSet<Object>();
         result.addAll(metadata);
         result.addAll(secondaryMetadata);
         return result;
      }

      public void setMetadata(boolean primary, Collection<?> units)
      {
         if (primary)
         {
            metadata = new LinkedHashSet<Object>(units);
         }
         else
         {
            secondaryMetadata = new LinkedHashSet<Object>(units);
         }
      }

   }

   private void mergeMetadata(DependencyMetadata dm, ReactorProject reactorProject, String classifier, boolean primary)
   {
      Set<Object> dependencyMetadata = reactorProject.getDependencyMetadata(classifier, primary);
      if (dependencyMetadata != null && !dependencyMetadata.isEmpty())
      {
         Set<Object> merged = dm.getMetadata(primary);
         if (merged == null)
         {
            merged = new LinkedHashSet<Object>();
         }
         merged.addAll(dependencyMetadata);
         dm.setMetadata(primary, merged);
      }
   }

   private void aggregateTargetPlatformConfigurations(MavenSession session,
      final TargetPlatformConfiguration aggregatedPlatform, final LinkedHashSet<Dependency> frameworkExtensions,
      LinkedHashSet<String> explodedBundles)
   {
      final LinkedHashSet<TargetEnvironment> environments = new LinkedHashSet<TargetEnvironment>();
      final LinkedHashSet<String> executionEnvironments = new LinkedHashSet<String>();
      final LinkedHashSet<ArtifactKey> requirements = new LinkedHashSet<ArtifactKey>();
      final LinkedHashSet<Dependency> dependencies = new LinkedHashSet<Dependency>();
      for (MavenProject project : session.getProjects())
      {
         final TychoProject tychoProject = getTychoProject(project);
         if (tychoProject != null)
         {
            final TargetPlatformConfiguration configuration = getTargetPlatformConfiguration(session, project);
            environments.addAll(configuration.getEnvironments());

            final String executionEnvironment = configuration.getExecutionEnvironment();
            if (executionEnvironment != null)
            {
               executionEnvironments.add(executionEnvironment);
            }

            final Boolean allow = aggregatedPlatform.getAllowConflictingDependencies();
            if (allow == null || allow.booleanValue() == false)
            {
               aggregatedPlatform.setAllowConflictingDependencies(configuration.getAllowConflictingDependencies());
            }
            
            final boolean implicitTargetEnvironment = aggregatedPlatform.isImplicitTargetEnvironment();
            if (!implicitTargetEnvironment)
            {
               aggregatedPlatform.setImplicitTargetEnvironment(configuration.isImplicitTargetEnvironment());
            }

            final boolean includePackedArtifacts = aggregatedPlatform.isIncludePackedArtifacts();
            if (!includePackedArtifacts)
            {
               aggregatedPlatform.setIncludePackedArtifacts(configuration.isIncludePackedArtifacts());
            }

            final String pomDependencies = aggregatedPlatform.getPomDependencies();
            if (pomDependencies == null)
            {
               aggregatedPlatform.setPomDependencies(configuration.getPomDependencies());
            }

            final String targetPlatformResolver = aggregatedPlatform.getTargetPlatformResolver();
            if (targetPlatformResolver == null)
            {
               aggregatedPlatform.setResolver(configuration.getTargetPlatformResolver());
            }

            aggregatedPlatform.getFilters().addAll(configuration.getFilters());

            aggregatedPlatform.getExtraRequirements().addAll(configuration.getExtraRequirements());

            final TychoSurefirePluginConfiguration surefirePluginConfiguration = new TychoSurefirePluginConfigurationReader()
               .read(project);
            if (surefirePluginConfiguration != null)
            {
               dependencies.addAll(surefirePluginConfiguration.getDependencies());
               explodedBundles.addAll(surefirePluginConfiguration.getExplodedBundles());
               frameworkExtensions.addAll(surefirePluginConfiguration.getFrameworkExtensions());
            }
         }
      }

      aggregatedPlatform.getEnvironments().addAll(environments);
      if (!executionEnvironments.isEmpty())
      {
         aggregatedPlatform.setExecutionEnvironment(eeSelector.select(executionEnvironments));
      }

      for (ArtifactKey requirement : requirements)
      {
         Dependency dependency = new Dependency();
         dependency.setArtifactId(requirement.getId());
         dependency.setVersion(requirement.getVersion());
         dependency.setType(requirement.getType());

         aggregatedPlatform.getExtraRequirements().add(dependency);
      }

      aggregatedPlatform.getExtraRequirements().addAll(dependencies);
   }
}
