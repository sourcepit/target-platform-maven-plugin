/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver.tycho;

import static org.sourcepit.common.utils.io.IOResources.osgiIn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformResolver;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformResolverFactory;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.resolver.TychoDependencyResolver;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.xml.XmlUtils;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Named
public class TychoTargetPlatformResolver implements org.sourcepit.tpmp.resolver.TargetPlatformResolver
{
   @Inject
   private Map<String, TychoProject> projectTypes;

   @Inject
   private DefaultTargetPlatformResolverFactory targetPlatformResolverLocator;

   @Inject
   private RepositorySystem repositorySystem;

   @Inject
   private ResolutionErrorHandler resolutionErrorHandler;

   @Inject
   private BundleReader bundleReader;

   @Inject
   private TychoDependencyResolver resolver;

   public void resolveTargetPlatform(MavenSession session, MavenProject project, TargetPlatformResolutionHandler handler)
   {
      if (!(projectTypes.get(project.getPackaging()) instanceof TychoProject))
      {
         return;
      }

      final List<Dependency> dependencies = new ArrayList<Dependency>();
      final Set<String> explodedBundles = new HashSet<String>();

      final TargetPlatformConfiguration configuration = getTargetPlatformConfiguration(session, project);

      for (TargetEnvironment te : configuration.getEnvironments())
      {
         handler.handleTargetEnvironment(te.getOs(), te.getWs(), te.getArch(), te.getNl());
      }

      final String ee = configuration.getExecutionEnvironment();
      if (ee != null)
      {
         handler.handleExecutionEnvironment(ee);
      }

      dependencies.addAll(configuration.getDependencyResolverConfiguration().getExtraRequirements());

      final TychoSurefirePluginConfiguration surefirePluginConfiguration = new TychoSurefirePluginConfigurationReader()
         .read(project);
      if (surefirePluginConfiguration != null)
      {
         dependencies.addAll(surefirePluginConfiguration.getDependencies());
         explodedBundles.addAll(surefirePluginConfiguration.getExplodedBundles());
      }

      final Map<ReactorProject, MavenProject> projectsMap = new HashMap<ReactorProject, MavenProject>();
      final List<ReactorProject> reactorProjects = new ArrayList<ReactorProject>();
      for (MavenProject mavenProject : session.getProjects())
      {
         final ReactorProject reactorProject = DefaultReactorProject.adapt(mavenProject);
         projectsMap.put(reactorProject, mavenProject);
         reactorProjects.add(reactorProject);

      }
      final TargetPlatformResolver platformResolver = targetPlatformResolverLocator.lookupPlatformResolver(project);

      final TargetPlatform targetPlatform = platformResolver.computeTargetPlatform(session, project, reactorProjects);

      final DependencyResolverConfiguration resolverConfiguration = new DependencyResolverConfiguration()
      {
         public OptionalResolutionAction getOptionalResolutionAction()
         {
            return OptionalResolutionAction.OPTIONAL;
         }

         public List<Dependency> getExtraRequirements()
         {
            return dependencies;
         }
      };

      final DependencyArtifacts platformArtifacts = platformResolver.resolveDependencies(session, project,
         targetPlatform, reactorProjects, resolverConfiguration);

      if (platformArtifacts == null)
      {
         throw new IllegalStateException("Cannot determinate build target platform location -- not executing tests");
      }

      for (ArtifactDescriptor artifact : platformArtifacts.getArtifacts(ArtifactKey.TYPE_ECLIPSE_FEATURE))
      {
         final MavenProject mavenProject = getMavenProject(projectsMap, artifact);

         final File location = artifact.getLocation();

         // due to the feature model of Tycho violates the specified behaviour of the "unpack" attribute, we have to
         // parse the feature.xml on our own. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=386851.
         final Document feature = loadFeature(location);
         for (Node pluginNode : XmlUtils.queryNodes(feature, "/feature/plugin"))
         {
            final Element plugin = (Element) pluginNode;
            if (isUnpack(plugin))
            {
               explodedBundles.add(getId(plugin));
            }
         }

         final ArtifactKey key = artifact.getKey();
         handler.handleFeature(key.getId(), key.getVersion(), location, mavenProject);
      }

      for (ArtifactDescriptor artifact : platformArtifacts.getArtifacts())
      {
         final ArtifactKey key = artifact.getKey();
         final String type = key.getType();
         if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(type) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(type))
         {
            final MavenProject mavenProject = getMavenProject(projectsMap, artifact);
            final boolean explodedBundle = isExplodedBundle(key.getId(), artifact.getLocation(), explodedBundles);
            handler.handlePlugin(key.getId(), key.getVersion(), artifact.getLocation(), explodedBundle, mavenProject);
         }
      }

      if (surefirePluginConfiguration != null)
      {
         dependencies.addAll(surefirePluginConfiguration.getDependencies());
         explodedBundles.addAll(surefirePluginConfiguration.getExplodedBundles());

         final List<File> frameworkExtensions = getFrameworkExtensions(session, project,
            surefirePluginConfiguration.getFrameworkExtensions());
         processFrameworkExtensions(explodedBundles, frameworkExtensions, handler);
      }
   }

   private TargetPlatformConfiguration getTargetPlatformConfiguration(MavenSession session, MavenProject project)
   {
      setupSessionLazy(session);
      return TychoProjectUtils.getTargetPlatformConfiguration(project);
   }

   private void setupSessionLazy(MavenSession session)
   {
      List<MavenProject> projects = session.getProjects();
      for (MavenProject project : projects)
      {
         setupProjectLazy(session, project);
      }
   }

   private void setupProjectLazy(MavenSession session, MavenProject project)
   {
      final TargetPlatformConfiguration targetPlatformConfiguration = (TargetPlatformConfiguration) project
         .getContextValue(TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION);
      if (targetPlatformConfiguration == null)
      {
         // project was not set up by Tycho. Maybe running in -Dtycho.mode=maven
         resolver.setupProject(session, project, DefaultReactorProject.adapt(project));
      }
   }

   private String getId(Element plugin)
   {
      if (plugin.hasAttribute("id"))
      {
         return plugin.getAttribute("id");
      }
      return null;
   }

   private boolean isUnpack(final Element plugin)
   {
      if (!plugin.hasAttribute("unpack"))
      {
         return true;
      }
      return Boolean.parseBoolean(plugin.getAttribute("unpack"));
   }

   private Document loadFeature(File location)
   {
      final Document[] result = new Document[1];
      new IOOperation<InputStream>(osgiIn(location, "feature.xml"))
      {
         @Override
         protected void run(InputStream openResource) throws IOException
         {
            result[0] = XmlUtils.readXml(openResource);
         }
      }.run();
      return result[0];
   }

   private boolean isExplodedBundle(String id, File location, Set<String> explodedBundles)
   {
      if (explodedBundles.contains(id))
      {
         return true;
      }
      return bundleReader.loadManifest(location).isDirectoryShape();
   }

   private MavenProject getMavenProject(final Map<ReactorProject, MavenProject> projectsMap, ArtifactDescriptor artifact)
   {
      ReactorProject otherProject = artifact.getMavenProject();
      MavenProject mavenProject = null;
      if (otherProject != null)
      {
         mavenProject = projectsMap.get(otherProject);
         if (mavenProject == null)
         {
            throw new IllegalStateException();
         }
      }
      return mavenProject;
   }

   private List<File> getFrameworkExtensions(MavenSession session, MavenProject project,
      Collection<Dependency> frameworkExtensions)
   {
      final List<File> files = new ArrayList<File>();

      if (frameworkExtensions != null)
      {
         for (Dependency frameworkExtension : frameworkExtensions)
         {
            Artifact artifact = repositorySystem.createDependencyArtifact(frameworkExtension);
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(artifact);
            request.setResolveRoot(true).setResolveTransitively(false);
            request.setLocalRepository(session.getLocalRepository());
            request.setRemoteRepositories(project.getPluginArtifactRepositories());
            request.setOffline(session.isOffline());
            request.setForceUpdate(session.getRequest().isUpdateSnapshots());
            ArtifactResolutionResult result = repositorySystem.resolve(request);
            try
            {
               resolutionErrorHandler.throwErrors(request, result);
            }
            catch (ArtifactResolutionException e)
            {
               throw new IllegalStateException("Failed to resolve framework extension "
                  + frameworkExtension.getManagementKey(), e);
            }
            files.add(artifact.getFile());
         }
      }

      return files;
   }

   private void processFrameworkExtensions(Set<String> explodedBundles, Collection<File> frameworkExtensions,
      TargetPlatformResolutionHandler handler)
   {
      for (File bundleFile : frameworkExtensions)
      {
         final OsgiManifest mf = bundleReader.loadManifest(bundleFile);

         final String symbolicName = mf.getBundleSymbolicName();
         final String version = mf.getBundleVersion();

         handler.handlePlugin(symbolicName, version, bundleFile,
            isExplodedBundle(symbolicName, bundleFile, explodedBundles), null);
      }
   }
}
