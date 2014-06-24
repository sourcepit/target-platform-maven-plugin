/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.tpmp.it;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sourcepit.common.maven.testing.ExternalMavenTest;
import org.sourcepit.common.testing.Environment;

public class TpmpIT extends ExternalMavenTest
{
   @Override
   protected boolean isDebug()
   {
      return true;
   }

   @Override
   protected Environment newEnvironment()
   {
      return Environment.get("it-env.properties");
   }

   @Test
   public void testReactor_PerSession() throws Exception
   {
      testReactor("per-session");
   }

   @Test
   public void testReactor_PerProject() throws Exception
   {
      testReactor("per-project");
   }

   private void testReactor(String strategy) throws IOException
   {
      final String projectVersion = getEnvironment().getProperty("project.version");
      final File projectDir = getResource("tycho-reactor");
      build(projectDir, "-e", "-B", "org.sourcepit:target-platform-maven-plugin:" + projectVersion + ":materialize",
         "-Dtpmp.resolutionStrategy=" + strategy);

      final File platformDir = getPlatformDir(projectDir);
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.tpmp.feature
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

   private File getPlatformDir(final File projectDir)
   {
      return new File(projectDir, "target/" + projectDir.getName() + "-0.1.0-SNAPSHOT-target");
   }

   @Test
   public void testReactorTychoModeMaven_PerSession() throws Exception
   {
      testReactorTychoModeMaven("per-session");
   }

   @Test
   public void testReactorTychoModeMaven_PerProject() throws Exception
   {
      testReactorTychoModeMaven("per-project");
   }

   private void testReactorTychoModeMaven(String strategy) throws IOException
   {
      final String projectVersion = getEnvironment().getProperty("project.version");
      final File projectDir = getResource("tycho-reactor");
      build(projectDir, "-Dtycho.mode=maven", "-e", "-B", "clean", "org.sourcepit:target-platform-maven-plugin:"
         + projectVersion + ":materialize", "-Dtpmp.resolutionStrategy=" + strategy);

      final File platformDir = getPlatformDir(projectDir);
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.tpmp.feature
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
   public void testTargetPlatformConfigurationNotChanged() throws Exception
   {
      testReactorTychoModeMaven("per-project");

      final String projectVersion = getEnvironment().getProperty("project.version");
      final File projectDir = new File(getWs().getRoot(), "tycho-reactor");
      assertTrue(projectDir.exists());

      final File platformDir = getPlatformDir(projectDir);
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      final File metadataDir = new File(platformDir, ".tpmp");
      assertTrue(metadataDir.exists());

      FileUtils.forceDelete(featuresDir);
      FileUtils.forceDelete(pluginsDir);

      build(projectDir, "-Dtycho.mode=maven", "-e", "-B", "org.sourcepit:target-platform-maven-plugin:"
         + projectVersion + ":materialize", "-Dtpmp.resolutionStrategy=per-project");

      assertFalse(featuresDir.exists());
      assertFalse(pluginsDir.exists());
   }

   @Test
   public void testTestPlugin_PerSession() throws Exception
   {
      testTestPlugin("per-session");
   }

   @Test
   public void testTestPlugin_PerProject() throws Exception
   {
      testTestPlugin("per-project");
   }

   private void testTestPlugin(String strategy) throws IOException
   {
      final String projectVersion = getEnvironment().getProperty("project.version");

      final File reactorDir = getResource("tycho-reactor");

      build(reactorDir, "--projects", "org.sourcepit.tpmp.tests,org.sourcepit.tpmp", "-e", "-B", "clean",
         "org.sourcepit:target-platform-maven-plugin:" + projectVersion + ":materialize", "-Dtpmp.resolutionStrategy="
            + strategy);

      final File platformDir = getPlatformDir(new File(reactorDir, "org.sourcepit.tpmp"));
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.tpmp.feature
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
   public void testPlugin_PerSession() throws Exception
   {
      testPlugin("per-session");
   }

   @Test
   public void testPlugin_PerProject() throws Exception
   {
      testPlugin("per-project");
   }

   private void testPlugin(String strategy) throws IOException
   {
      final String projectVersion = getEnvironment().getProperty("project.version");

      final File reactorDir = getResource("tycho-reactor");

      final File projectDir = new File(reactorDir, "org.sourcepit.tpmp");
      build(projectDir, "-e", "-B", "clean", "org.sourcepit:target-platform-maven-plugin:" + projectVersion
         + ":materialize", "-Dtpmp.resolutionStrategy=" + strategy);

      final File platformDir = getPlatformDir(projectDir);
      assertTrue(platformDir.exists());

      final File featuresDir = new File(platformDir, "features");
      assertTrue(featuresDir.exists());

      final File pluginsDir = new File(platformDir, "plugins");
      assertTrue(pluginsDir.exists());

      // org.eclipse.pde is contributed via the org.sourcepit.tpmp.feature
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
         @Override
         public boolean accept(File file)
         {
            return file.getName().startsWith(prefix);
         }
      });
      return junitPlugins;
   }
}
