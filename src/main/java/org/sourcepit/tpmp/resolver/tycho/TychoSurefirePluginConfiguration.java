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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;

public class TychoSurefirePluginConfiguration {
   private final List<String> explodedBundles = new ArrayList<String>();
   private final List<Dependency> frameworkExtensions = new ArrayList<Dependency>();
   private final List<Dependency> dependencies = new ArrayList<Dependency>();
   private boolean useUIHarness;

   public List<String> getExplodedBundles() {
      return explodedBundles;
   }

   public List<Dependency> getFrameworkExtensions() {
      return frameworkExtensions;
   }

   public List<Dependency> getDependencies() {
      return dependencies;
   }

   public boolean isUseUIHarness() {
      return useUIHarness;
   }

   public void setUseUIHarness(boolean useUIHarness) {
      this.useUIHarness = useUIHarness;
   }
}
