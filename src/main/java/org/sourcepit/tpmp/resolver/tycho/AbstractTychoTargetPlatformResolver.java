/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver.tycho;

import static org.sourcepit.common.utils.io.IO.osgiIn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
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
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.DependencyResolver;
import org.eclipse.tycho.core.DependencyResolverConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.resolver.DefaultDependencyResolverFactory;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.sourcepit.common.utils.io.IOOperation;
import org.sourcepit.common.utils.xml.XmlUtils;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.Optional;

@Named
public class AbstractTychoTargetPlatformResolver
{
   @Inject
   private MavenProjectFacade projectFacade;

   @Inject
   private DefaultDependencyResolverFactory targetPlatformResolverLocator;

   @Inject
   private RepositorySystem repositorySystem;

   @Inject
   private ResolutionErrorHandler resolutionErrorHandler;

   @Inject
   private BundleReader bundleReader;

   @Inject
   private TychoSourceIUResolver sourceResolver;

   protected void doResolve(MavenSession session, MavenProject project, List<ReactorProject> reactorProjects,
      boolean includeSource, Set<String> explodedBundles, List<Dependency> extraRequirements,
      Collection<Dependency> frameworkExtensions, ContentCollector contentCollector)
   {
      final TargetPlatform targetPlatform = resolve(session, project, reactorProjects, contentCollector,
         explodedBundles, extraRequirements);

      processFrameworkExtensions(explodedBundles, getFrameworkExtensions(session, project, frameworkExtensions),
         contentCollector);

      if (includeSource)
      {
         sourceResolver.resolveSources(session, targetPlatform, contentCollector.getPlugins(), contentCollector);
      }
   }


   protected TargetPlatform resolve(MavenSession session, final MavenProject project,
      List<ReactorProject> reactorProjects, TargetPlatformResolutionHandler resolutionHandler,
      final Set<String> explodedBundles, final List<Dependency> extraRequirements)
   {
      final DependencyResolver platformResolver = targetPlatformResolverLocator.lookupDependencyResolver(project);

      final TargetPlatform targetPlatform = platformResolver.computePreliminaryTargetPlatform(session, project,
         reactorProjects);

      final DependencyResolverConfiguration resolverConfiguration = new DependencyResolverConfiguration()
      {
         @Override
         public OptionalResolutionAction getOptionalResolutionAction()
         {
            return OptionalResolutionAction.OPTIONAL;
         }

         @Override
         public List<Dependency> getExtraRequirements()
         {
            return extraRequirements;
         }
      };

      final DependencyArtifacts platformArtifacts = platformResolver.resolveDependencies(session, project,
         targetPlatform, reactorProjects, resolverConfiguration);

      handlePluginsAndFeatures(session, project, platformArtifacts, explodedBundles, resolutionHandler);

      return targetPlatform;
   }

   private void handlePluginsAndFeatures(MavenSession session, MavenProject project,
      final DependencyArtifacts platformArtifacts, final Set<String> explodedBundles,
      TargetPlatformResolutionHandler resolutionHandler)
   {
      // map original rector projects to their versioned id. needed to recognize and re-map reactor artifacts later
      final Map<String, MavenProject> vidToProjectMap = projectFacade.createVidToProjectMap(session);

      for (ArtifactDescriptor artifact : platformArtifacts.getArtifacts(PackagingType.TYPE_ECLIPSE_FEATURE))
      {
         final Optional<MavenProject> mavenProject = projectFacade.getMavenProject(vidToProjectMap, artifact);

         final File location = projectFacade.getLocation(artifact, mavenProject);

         if (project.getBasedir().equals(location))
         {
            continue;
         }

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

         final ArtifactKey key = projectFacade.getArtifactKey(artifact, mavenProject);
         resolutionHandler.handleFeature(key.getId(), key.getVersion(), location, mavenProject.orNull());
      }

      for (ArtifactDescriptor artifact : platformArtifacts.getArtifacts())
      {
         // Pre Tycho 0.20.0 source artifacts was typed as "eclipse-repository"... Now source artifacts are
         // eclipse-plugins. Because of we will resolve sources later, ignore it here.
         if (!"sources".equals(artifact.getClassifier()))
         {
            final Optional<MavenProject> mavenProject = projectFacade.getMavenProject(vidToProjectMap, artifact);
            final File location = projectFacade.getLocation(artifact, mavenProject);
            if (project.getBasedir().equals(location))
            {
                continue;
            }

            final ArtifactKey key = projectFacade.getArtifactKey(artifact, mavenProject);
            final String type = key.getType();
            if (PackagingType.TYPE_ECLIPSE_PLUGIN.equals(type) || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(type))
            {
               final boolean explodedBundle = isExplodedBundle(key.getId(), location, explodedBundles);
               resolutionHandler.handlePlugin(key.getId(), key.getVersion(), location, explodedBundle,
                  mavenProject.orNull());
            }
         }
      }
   }

   protected void processFrameworkExtensions(Set<String> explodedBundles, Collection<File> frameworkExtensions,
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

   private boolean isExplodedBundle(String id, File location, Set<String> explodedBundles)
   {
      if (explodedBundles.contains(id))
      {
         return true;
      }
      return bundleReader.loadManifest(location).isDirectoryShape();
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

   private String getId(Element plugin)
   {
      if (plugin.hasAttribute("id"))
      {
         return plugin.getAttribute("id");
      }
      return null;
   }

   protected TychoProject getTychoProject(MavenProject project)
   {
      return projectFacade.getTychoProject(project);
   }

   protected TargetPlatformConfiguration getTargetPlatformConfiguration(MavenSession session, MavenProject project)
   {
      return projectFacade.getTargetPlatformConfiguration(session, project);
   }

   protected static class ContentCollector implements TargetPlatformResolutionHandler
   {
      private final TargetPlatformResolutionHandler delegate;

      private final Set<String> plugins = new LinkedHashSet<String>();

      public ContentCollector(TargetPlatformResolutionHandler delegate)
      {
         this.delegate = delegate;
      }

      @Override
      public void handleFeature(String id, String version, File location, MavenProject mavenProject)
      {
         delegate.handleFeature(id, version, location, mavenProject);
      }

      @Override
      public void handlePlugin(String id, String version, File location, boolean unpack, MavenProject mavenProject)
      {
         plugins.add(id + "_" + version);
         delegate.handlePlugin(id, version, location, unpack, mavenProject);
      }

      public Set<String> getPlugins()
      {
         return plugins;
      }
   }
}
