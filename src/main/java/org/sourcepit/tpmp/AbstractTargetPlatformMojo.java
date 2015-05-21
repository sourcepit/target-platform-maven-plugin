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

package org.sourcepit.tpmp;

import static org.sourcepit.common.utils.io.IO.cpIn;
import static org.sourcepit.common.utils.io.IO.read;
import static org.sourcepit.common.utils.xml.XmlUtils.queryNode;
import static org.sourcepit.common.utils.xml.XmlUtils.writeXml;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.sourcepit.common.utils.io.Read.FromStream;
import org.sourcepit.common.utils.lang.PipedException;
import org.sourcepit.common.utils.xml.XmlUtils;
import org.sourcepit.tpmp.ee.ExecutionEnvironmentSelector;
import org.sourcepit.tpmp.resolver.TargetPlatformResolver;
import org.w3c.dom.Document;

public abstract class AbstractTargetPlatformMojo extends AbstractMojo {
   @Inject
   protected LegacySupport buildContext;

   @Parameter(property = "tpmp.targetDir", defaultValue = "${project.build.directory}")
   protected File targetDir;

   @Parameter(property = "tpmp.forceUpdate", defaultValue = "false")
   private boolean forceUpdate;

   @Parameter(property = "tpmp.includeSource", defaultValue = "true")
   private boolean includeSource;

   @Parameter(property = "tpmp.classifier", defaultValue = "target")
   protected String classifier;

   @Parameter(property = "tpmp.eclipseProjectName", defaultValue = "Target Platforms")
   protected String eclipseProjectName;

   @Parameter(property = "tpmp.resolutionStrategy", defaultValue = "per-session")
   protected String resolutionStrategy;

   @Inject
   protected RepositorySystem repositorySystem;

   @Inject
   private ExecutionEnvironmentSelector eeSelector;

   @Inject
   private Map<String, TargetPlatformResolver> resolverMap;

   @Override
   public final void execute() throws MojoExecutionException, MojoFailureException {
      try {
         doExecute();
      }
      catch (PipedException e) {
         e.adaptAndThrow(MojoExecutionException.class);
         e.adaptAndThrow(MojoFailureException.class);
         throw e;
      }
   }

   protected abstract void doExecute();

   protected Artifact createPlatformArtifact(MavenProject project) {
      final Artifact platformArtifact = repositorySystem.createArtifactWithClassifier(project.getGroupId(),
         project.getArtifactId(), project.getVersion(), "zip", classifier);
      return platformArtifact;
   }

   protected void updateTargetPlatform(final MavenProject project, final File platformDir) {
      final TargetPlatformResolver resolver = getResolver();

      final CopyTargetPlatformResolutionHandler resolutionHandler = new CopyTargetPlatformResolutionHandler(platformDir);
      resolver.resolve(getSession(), platformDir, includeSource, forceUpdate, resolutionHandler, resolutionHandler);

      final String executionEnvironment = selectExecutionEnvironment(resolutionHandler.getExecutionEnvironments());
      writeDefinitions(project, platformDir, executionEnvironment, resolutionHandler.getTargetEnvironments());

      writeDotProject(platformDir);
   }

   protected MavenSession getSession() {
      return buildContext.getSession();
   }

   private void writeDotProject(final File platformDir) {
      final File dotProject = new File(platformDir.getParentFile(), ".project");
      if (dotProject.exists()) {
         getLog().info("Eclipse project description (.project) already exists, skipping creation of a new one.");
      }
      else {
         getLog().info(
            "Writing Eclipse project description (.project) under " + dotProject.getParent()
               + ". You can import the project into your workspace afterwards.");

         final Document doc = read(new FromStream<Document>() {
            @Override
            public Document read(InputStream inputStream) throws Exception {
               return XmlUtils.readXml(inputStream);
            }
         }, cpIn(getClass().getClassLoader(), "META-INF/tpmp/.project"));

         queryNode(doc, "/projectDescription/name").setTextContent(eclipseProjectName);
         writeXml(doc, dotProject);
      }
   }

   protected TargetPlatformResolver getResolver() {
      final TargetPlatformResolver resolver = resolverMap.get(resolutionStrategy);
      if (resolver == null) {
         throw new IllegalStateException("No resolver available for strategy '" + resolutionStrategy + "'");
      }
      return resolver;
   }

   protected void writeDefinitions(MavenProject project, File parentDir, String executionEnvironment,
      Collection<TargetEnvironment> targetEnvironments) {
      for (TargetEnvironment targetEnvironment : targetEnvironments) {
         final String platformName = getTargetPlatformDefinitionName(project, targetEnvironment);

         final File targetFile = new File(parentDir, platformName + ".target");

         new TargetPlatformWriter().write(targetFile, platformName, parentDir.getAbsolutePath(), targetEnvironment,
            executionEnvironment);
      }
   }

   private String getTargetPlatformDefinitionName(MavenProject project, TargetEnvironment targetEnvironment) {
      final StringBuilder sb = new StringBuilder();
      sb.append(getClassifiedName(project));
      sb.append('-');
      sb.append(targetEnvironment.getOs());
      sb.append('-');
      sb.append(targetEnvironment.getWs());
      sb.append('-');
      sb.append(targetEnvironment.getArch());
      return sb.toString();
   }

   protected String selectExecutionEnvironment(Collection<String> executionEnvironments) {
      return eeSelector.select(executionEnvironments);
   }

   protected File getPlatformZipFile(MavenProject project) {
      return new File(targetDir, getClassifiedName(project) + ".zip");
   }

   protected File getPlatformDir(MavenProject project) {
      return new File(targetDir, getClassifiedName(project));
   }

   protected String getClassifiedName(MavenProject project) {
      return getFinalName(project) + "-" + classifier;
   }

   protected String getFinalName(MavenProject project) {
      String finalName = project.getBuild().getFinalName();
      if (finalName == null) {
         finalName = project.getArtifactId() + "-" + project.getVersion();
      }
      return finalName;
   }
}
