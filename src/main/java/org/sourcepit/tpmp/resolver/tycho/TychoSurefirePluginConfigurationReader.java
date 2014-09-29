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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class TychoSurefirePluginConfigurationReader
{
   public TychoSurefirePluginConfiguration read(MavenProject project)
   {
      final Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-surefire-plugin");
      if (plugin != null)
      {
         final TychoSurefirePluginConfiguration result = new TychoSurefirePluginConfiguration();
         final Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
         if (configuration != null)
         {
            readConfiguration(result, configuration);
         }
         return result;
      }
      return null;
   }

   private void readConfiguration(TychoSurefirePluginConfiguration result, Xpp3Dom configuration)
   {
      for (Xpp3Dom child : configuration.getChildren())
      {
         final String tagName = child.getName();
         if ("explodedBundles".equals(tagName))
         {
            readExplodedBundles(result, child);
         }
         else if ("frameworkExtensions".equals(tagName))
         {
            readFrameworkExtensions(result, child);
         }
         else if ("dependencies".equals(tagName))
         {
            readDependenciess(result, child);
         }
         else if ("useUIHarness".equals(tagName))
         {
            result.setUseUIHarness(Boolean.valueOf(extractNonEmptyValue(child)).booleanValue());
         }
      }
   }

   private void readDependenciess(TychoSurefirePluginConfiguration result, Xpp3Dom dependencies)
   {
      for (Xpp3Dom dependency : dependencies.getChildren())
      {
         result.getDependencies().add(newDependency(dependency));
      }
   }

   private void readFrameworkExtensions(TychoSurefirePluginConfiguration result, Xpp3Dom frameworkExtensions)
   {
      for (Xpp3Dom frameworkExtension : frameworkExtensions.getChildren())
      {
         result.getFrameworkExtensions().add(newDependency(frameworkExtension));
      }
   }

   private Dependency newDependency(Xpp3Dom dependency)
   {
      final Dependency result = new Dependency();
      result.setGroupId(extractNonEmptyValue(dependency.getChild("groupId")));
      result.setArtifactId(extractNonEmptyValue(dependency.getChild("artifactId")));
      result.setVersion(extractNonEmptyValue(dependency.getChild("version")));
      result.setClassifier(extractNonEmptyValue(dependency.getChild("classifier")));
      final String type = extractNonEmptyValue(dependency.getChild("type"));
      if (type != null)
      {
         result.setType(type);
      }
      result.setSystemPath(extractNonEmptyValue(dependency.getChild("systemPath")));
      result.setScope(extractNonEmptyValue(dependency.getChild("scope")));
      result.setOptional(extractNonEmptyValue(dependency.getChild("optional")));
      return result;
   }

   private void readExplodedBundles(TychoSurefirePluginConfiguration result, Xpp3Dom explodedBundles)
   {
      for (Xpp3Dom explodedBundle : explodedBundles.getChildren())
      {
         final String value = extractNonEmptyValue(explodedBundle);
         if (value != null)
         {
            result.getExplodedBundles().add(value);
         }
      }
   }

   private String extractNonEmptyValue(Xpp3Dom node)
   {
      String value = node == null ? null : node.getValue();
      if (value != null)
      {
         value.trim();
         if (value.length() == 0)
         {
            value = null;
         }
      }
      return value;
   }

}
