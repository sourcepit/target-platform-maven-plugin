/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.resolver.tycho;

import static org.sourcepit.common.utils.lang.Exceptions.pipe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.sourcepit.common.manifest.osgi.BundleManifest;
import org.sourcepit.common.manifest.osgi.BundleSymbolicName;
import org.sourcepit.common.manifest.osgi.Version;
import org.sourcepit.common.manifest.osgi.resource.BundleManifestResourceImpl;
import org.sourcepit.tpmp.resolver.TargetPlatformResolutionHandler;

@Named
public class TychoSourceIUResolver
{
   @Inject
   private EquinoxServiceFactory equinox;

   @Inject
   private Logger logger;

   public void resolveSources(MavenSession session, final TargetPlatform targetPlatform,
      Collection<String> sourceTargetBundles, TargetPlatformResolutionHandler handler)
   {
      final Map<File, MavenProject> projectsMap = new HashMap<File, MavenProject>();
      for (MavenProject mavenProject : session.getProjects())
      {
         projectsMap.put(mavenProject.getBasedir(), mavenProject);
      }

      final P2ResolverFactory factory = equinox.getService(P2ResolverFactory.class);
      final P2Resolver resolver = factory.createResolver(new MavenLoggerAdapter(logger, false));

      final Class<? extends TargetPlatform> clazz = targetPlatform.getClass();
      final Method method = getMethod(clazz, "getInstallableUnits");
      final Collection<?> units = invoke(method, targetPlatform);

      for (final Object unit : units)
      {
         if (hasSourceCapability(unit))
         {
            final String symbolicName = getSymbolicName(unit);
            final String version = getVersion(unit);

            final BundleManifest manifest = getManifest(unit);

            String[] targetIdAndVersion = getTargetIdAndVersion(manifest);
            if (targetIdAndVersion == null)
            {
               targetIdAndVersion = getTargetIdAndVersion(symbolicName, version);
            }

            if (targetIdAndVersion != null)
            {
               final String targetKey = targetIdAndVersion[0] + "_" + targetIdAndVersion[1];
               if (sourceTargetBundles.contains(targetKey))
               {
                  final P2ResolutionResult result = resolver.resolveInstallableUnit(targetPlatform, symbolicName,
                     version);

                  for (Entry entry : result.getArtifacts())
                  {
                     final File location = entry.getLocation();
                     if (location != null && location.exists())
                     {
                        handler.handlePlugin(entry.getId(), entry.getVersion(), location, false,
                           projectsMap.get(location));
                     }
                  }
               }
            }
         }
      }
   }

   private static String getSymbolicName(Object unit)
   {
      final Method method = getMethod(unit.getClass(), "getId");
      return invoke(method, unit);
   }

   private static String getVersion(Object unit)
   {
      final Method method = getMethod(unit.getClass(), "getVersion");
      final Object version = invoke(method, unit);
      return version.toString();
   }

   private String[] getTargetIdAndVersion(String symbolicName, String version)
   {
      String targetId = null;
      if (symbolicName.endsWith(".source"))
      {
         targetId = symbolicName.substring(0, symbolicName.length() - ".source".length());
      }
      if (targetId == null || version == null)
      {
         return null;
      }
      return new String[] { targetId, version };
   }

   private String[] getTargetIdAndVersion(BundleManifest manifest)
   {
      String targetId = null;
      String version = null;

      final BundleSymbolicName bundleSymbolicName = manifest.getBundleSymbolicName();
      final Version bundleVersion = manifest.getBundleVersion();
      if (bundleSymbolicName != null && bundleVersion != null)
      {
         String symbolicName = bundleSymbolicName.getSymbolicName();
         String value = manifest.getHeaderValue("Eclipse-SourceBundle");
         if (value == null)
         {
            if (symbolicName.endsWith(".source"))
            {
               targetId = symbolicName.substring(0, symbolicName.length() - ".source".length());
            }
            version = bundleVersion.toFullString();
         }
         else
         {
            String[] segments = value.split(";");
            targetId = segments[0];
            for (int i = 1; i < segments.length; i++)
            {
               String segment = segments[i];
               if (segment.startsWith("version="))
               {
                  segment = segment.substring("version=".length());
                  if (segment.startsWith("\""))
                  {
                     segment = segment.substring(1);
                  }
                  if (segment.endsWith("\""))
                  {
                     segment = segment.substring(0, segment.length() - 1);
                  }
                  version = segment;
                  break;
               }
            }
         }
      }

      if (targetId == null || version == null)
      {
         return null;
      }
      return new String[] { targetId, version };
   }

   private static BundleManifest getManifest(Object unit)
   {
      Method method = getMethod(unit.getClass(), "getTouchpointData");
      final Collection<?> points = invoke(method, unit);
      for (Object point : points)
      {
         // getInstruction(String instructionKey)
         method = getMethod(point.getClass(), "getInstruction", String.class);
         Object instruction = invoke(method, point, "manifest");
         if (instruction != null)
         {
            method = getMethod(instruction.getClass(), "getBody");

            String manifest = invoke(method, instruction);

            Resource resource = new BundleManifestResourceImpl();
            try
            {
               resource.load(new ByteArrayInputStream(manifest.getBytes("UTF-8")), null);
            }
            catch (IOException e)
            {
               throw pipe(e);
            }
            return (BundleManifest) resource.getContents().get(0);
         }
      }
      return null;
   }

   private static boolean hasSourceCapability(Object unit)
   {
      final Method method = getMethod(unit.getClass(), "getProvidedCapabilities");
      final Collection<?> capabilities = invoke(method, unit);
      for (Object capabilty : capabilities)
      {
         if (capabilty.toString().startsWith("org.eclipse.equinox.p2.eclipse.type/source/"))
         {
            return true;
         }
      }
      return false;
   }

   private static Method getMethod(Class<?> clazz, String methodName, Class<?>... argTypes)
   {
      try
      {
         return clazz.getDeclaredMethod(methodName, argTypes);
      }
      catch (NoSuchMethodException e)
      {
         for (Class<?> interfaze : clazz.getInterfaces())
         {
            final Method method = getMethod(interfaze, methodName, argTypes);
            if (method != null)
            {
               return method;
            }
         }
         final Class<?> superclass = clazz.getSuperclass();
         if (superclass != null)
         {
            final Method method = getMethod(superclass, methodName, argTypes);
            if (method != null)
            {
               return method;
            }
         }
         return null;
      }
   }

   @SuppressWarnings("unchecked")
   private static <T> T invoke(Method method, Object target, Object... args)
   {
      try
      {
         return (T) method.invoke(target, args);
      }
      catch (IllegalAccessException e)
      {
         throw pipe(e);
      }
      catch (InvocationTargetException e)
      {
         final Throwable t = e.getTargetException();
         if (t instanceof RuntimeException)
         {
            throw (RuntimeException) t;
         }
         if (t instanceof Error)
         {
            throw (Error) t;
         }
         if (t instanceof Exception)
         {
            throw pipe((Exception) t);
         }
         throw new IllegalStateException(t);
      }
   }
}
