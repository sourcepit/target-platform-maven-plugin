/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp.resolver.tycho;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sourcepit.common.maven.testing.EmbeddedMavenEnvironmentTest;
import org.sourcepit.common.maven.testing.MavenExecutionResult2;
import org.sourcepit.common.testing.Environment;
import org.sourcepit.mtp.resolver.tycho.TychoSurefirePluginConfiguration;
import org.sourcepit.mtp.resolver.tycho.TychoSurefirePluginConfigurationReader;

public class TychoSurefirePluginConfigurationReaderTest extends EmbeddedMavenEnvironmentTest
{
   @Override
   protected Environment newEnvironment()
   {
      return Environment.get("test-env.properties");
   }

   @Test
   public void test() throws Exception
   {
      final File pom = getResource("TychoSurefirePluginConfigurationReaderTest/pom.xml");

      final MavenExecutionResult2 result = buildProject(pom);

      final MavenProject project = result.getProject();

      final TychoSurefirePluginConfiguration configuration = new TychoSurefirePluginConfigurationReader().read(project);
      assertNotNull(configuration);
      
      assertThat(configuration.isUseUIHarness(),is(true));

      final List<String> explodedBundles = configuration.getExplodedBundles();
      assertThat(explodedBundles.size(), is(2));
      assertThat(explodedBundles.get(0), equalTo("org.apache.ant"));
      assertThat(explodedBundles.get(1), equalTo("org.junit"));

      final List<Dependency> frameworkExtensions = configuration.getFrameworkExtensions();
      assertThat(frameworkExtensions.size(), is(1));

      final Dependency frameworkExtension = frameworkExtensions.get(0);
      assertDependency(frameworkExtension, "org.eclipse.tycho.its.tycho353", "tycho353.fwk.ext", "1.0.0", null);

      final List<Dependency> dependencies = configuration.getDependencies();
      assertThat(dependencies.size(), is(2));
      assertDependency(dependencies.get(0), null, "eclipse.feature", "0.0.0", "eclipse-feature");
      assertDependency(dependencies.get(1), null, "eclipse.plugin", "0.0.0", "eclipse-plugin");
   }

   private void assertDependency(Dependency actual, String groupId, String artifactId, String version, String type)
   {
      if (groupId == null)
      {
         assertThat(actual.getGroupId(), nullValue());
      }
      else
      {
         assertThat(actual.getGroupId(), equalTo(groupId));
      }

      if (artifactId == null)
      {
         assertThat(actual.getArtifactId(), nullValue());
      }
      else
      {
         assertThat(actual.getArtifactId(), equalTo(artifactId));
      }

      if (version == null)
      {
         assertThat(actual.getVersion(), nullValue());
      }
      else
      {
         assertThat(actual.getVersion(), equalTo(version));
      }

      if (type == null)
      {
         assertThat(actual.getType(), equalTo("jar"));
      }
      else
      {
         assertThat(actual.getType(), equalTo(type));
      }

      assertThat(actual.getClassifier(), nullValue());
      assertThat(actual.getScope(), nullValue());
      assertThat(actual.getSystemPath(), nullValue());
      assertThat(actual.isOptional(), is(false));
   }
}
