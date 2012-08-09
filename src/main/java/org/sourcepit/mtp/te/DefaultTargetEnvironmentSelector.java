/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp.te;

import java.util.Collection;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;

@Named
public class DefaultTargetEnvironmentSelector implements TargetEnvironmentSelector
{
   private final LegacySupport legacySupport;

   @Inject
   public DefaultTargetEnvironmentSelector(LegacySupport legacySupport)
   {
      this.legacySupport = legacySupport;
   }

   public TargetEnvironment select(Collection<TargetEnvironment> targetEnvironments)
   {
      final TargetEnvironment currentTE = getCurrentTargetEnvironment();
      if (targetEnvironments.contains(currentTE))
      {
         return currentTE;
      }

      String prefix = toString(currentTE.getOs(), currentTE.getWs(), currentTE.getArch());
      for (TargetEnvironment te : targetEnvironments)
      {
         if (toString(te).startsWith(prefix))
         {
            return te;
         }
      }

      prefix = toString(currentTE.getOs(), currentTE.getWs(), null);
      for (TargetEnvironment te : targetEnvironments)
      {
         if (toString(te).startsWith(prefix))
         {
            return te;
         }
      }

      prefix = toString(currentTE.getOs(), null, null);
      for (TargetEnvironment te : targetEnvironments)
      {
         if (toString(te).startsWith(prefix))
         {
            return te;
         }
      }

      return null;
   }

   private String toString(TargetEnvironment te)
   {
      return toString(te.getOs(), te.getWs(), te.getArch());
   }

   private String toString(String os, String ws, String arch)
   {
      final StringBuilder sb = new StringBuilder();
      sb.append(os);
      if (ws != null)
      {
         sb.append('/').append(ws);
         if (arch != null)
         {
            sb.append('/').append(arch);
         }
      }
      return sb.toString();
   }

   private TargetEnvironment getCurrentTargetEnvironment()
   {
      final Properties properties = getProperties();

      final String os = PlatformPropertiesUtils.getOS(properties);
      final String ws = PlatformPropertiesUtils.getWS(properties);
      final String arch = PlatformPropertiesUtils.getArch(properties);

      return new TargetEnvironment(os, ws, arch, null);
   }

   private Properties getProperties()
   {
      MavenSession session = legacySupport.getSession();
      if (session != null)
      {
         MavenProject project = session.getCurrentProject();
         if (project == null)
         {
            final Properties properties = new Properties();
            properties.putAll(session.getSystemProperties()); // session wins
            properties.putAll(session.getUserProperties());
            return properties;
         }
         else
         {
            Properties properties = (Properties) project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);
            if (properties == null)
            {
               properties = new Properties();
               properties.putAll(project.getProperties());
               properties.putAll(session.getSystemProperties()); // session wins
               properties.putAll(session.getUserProperties());
            }
            return properties;
         }
      }
      return System.getProperties();
   }
}
