/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp.it;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;

import org.junit.Test;
import org.sourcepit.common.maven.testing.ExternalMavenTest;
import org.sourcepit.common.testing.Environment;

public class MtpIT extends ExternalMavenTest
{
   @Override
   protected boolean isDebug()
   {
      return false;
   }

   @Override
   protected Environment newEnvironment()
   {
      return Environment.get("it-env.properties");
   }

   @Test
   public void testReactor() throws Exception
   {
      final String projectVersion = getEnvironment().getProperty("project.version");
      final File projectDir = getResource("tycho-reactor");
      build(projectDir, "-e", "-B", "clean", "org.sourcepit:materialize-target-platform-maven-plugin:" + projectVersion
         + ":materialize-target-platform");

      final File platformDir = new File(projectDir, "target/target-platform");
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.mtp.feature
      File[] pdeFeatures = collectFiles(featuresDir, "org.eclipse.pde");
      assertThat(pdeFeatures.length, is(1));

      // org.eclipse.osgi unpacked due to Tycho test mojo configuration
      File[] osgiPlugins = collectFiles(pluginsDir, "org.eclipse.osgi_");
      assertThat(osgiPlugins.length, is(1));
      assertThat(osgiPlugins[0].isDirectory(), is(true));

      // org.junit is not unpacked due to unpack flag in org.eclipse.jdt feature.xml
      File[] junitPlugins = collectFiles(pluginsDir, "org.junit_4");
      assertThat(junitPlugins.length, is(1));
      assertThat(junitPlugins[0].isDirectory(), is(true));
   }

   @Test
   public void testReactorTychoModeMaven() throws Exception
   {
      final String projectVersion = getEnvironment().getProperty("project.version");
      final File projectDir = getResource("tycho-reactor");
      build(projectDir, "-Dtycho.mode=maven", "-e", "-B", "clean",
         "org.sourcepit:materialize-target-platform-maven-plugin:" + projectVersion + ":materialize-target-platform");

      final File platformDir = new File(projectDir, "target/target-platform");
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.mtp.feature
      File[] pdeFeatures = collectFiles(featuresDir, "org.eclipse.pde");
      assertThat(pdeFeatures.length, is(1));

      // org.eclipse.osgi unpacked due to Tycho test mojo configuration
      File[] osgiPlugins = collectFiles(pluginsDir, "org.eclipse.osgi_");
      assertThat(osgiPlugins.length, is(1));
      assertThat(osgiPlugins[0].isDirectory(), is(true));

      // org.junit is not unpacked due to unpack flag in org.eclipse.jdt feature.xml
      File[] junitPlugins = collectFiles(pluginsDir, "org.junit_4");
      assertThat(junitPlugins.length, is(1));
      assertThat(junitPlugins[0].isDirectory(), is(true));
   }

   @Test
   public void testTestPlugin() throws Exception
   {
      final String projectVersion = getEnvironment().getProperty("project.version");

      final File reactorDir = getResource("tycho-reactor");

      final File projectDir = new File(reactorDir, "org.sourcepit.mtp.tests");
      build(projectDir, "-e", "-B", "clean", "org.sourcepit:materialize-target-platform-maven-plugin:" + projectVersion
         + ":materialize-target-platform");

      final File platformDir = new File(projectDir, "target/target-platform");
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.mtp.feature
      File[] pdeFeatures = collectFiles(featuresDir, "org.eclipse.pde");
      assertThat(pdeFeatures.length, is(0));

      // org.eclipse.osgi unpacked due to Tycho test mojo configuration
      File[] osgiPlugins = collectFiles(pluginsDir, "org.eclipse.osgi_");
      assertThat(osgiPlugins.length, is(1));
      assertThat(osgiPlugins[0].isDirectory(), is(true));

      // org.junit is not unpacked because it's not installed via feature.xml
      File[] junitPlugins = collectFiles(pluginsDir, "org.junit_");
      assertThat(junitPlugins.length, is(1));
      assertThat(junitPlugins[0].isFile(), is(true));
   }

   @Test
   public void testPlugin() throws Exception
   {
      final String projectVersion = getEnvironment().getProperty("project.version");

      final File reactorDir = getResource("tycho-reactor");

      final File projectDir = new File(reactorDir, "org.sourcepit.mtp");
      build(projectDir, "-e", "-B", "clean", "org.sourcepit:materialize-target-platform-maven-plugin:" + projectVersion
         + ":materialize-target-platform");

      final File platformDir = new File(projectDir, "target/target-platform");
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.mtp.feature
      File[] pdeFeatures = collectFiles(featuresDir, "org.eclipse.pde");
      assertThat(pdeFeatures.length, is(0));

      // unlike to the test plugin test, there is no configuration which forces the org.eclipse.osgi plugin to be
      // unpacked
      File[] osgiPlugins = collectFiles(pluginsDir, "org.eclipse.osgi_");
      assertThat(osgiPlugins.length, is(1));
      assertThat(osgiPlugins[0].isFile(), is(true));

      // no dependency to org.junit present
      File[] junitPlugins = collectFiles(pluginsDir, "org.junit_");
      assertThat(junitPlugins.length, is(0));
   }

   private File[] collectFiles(final File dir, final String prefix)
   {
      final File[] junitPlugins = dir.listFiles(new FileFilter()
      {
         public boolean accept(File file)
         {
            return file.getName().startsWith(prefix);
         }
      });
      return junitPlugins;
   }
}
