/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp.resolver.tycho;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;

public class TychoSurefirePluginConfiguration
{
   private final List<String> explodedBundles = new ArrayList<String>();
   private final List<Dependency> frameworkExtensions = new ArrayList<Dependency>();
   private final List<Dependency> dependencies = new ArrayList<Dependency>();
   private boolean useUIHarness;

   public List<String> getExplodedBundles()
   {
      return explodedBundles;
   }

   public List<Dependency> getFrameworkExtensions()
   {
      return frameworkExtensions;
   }

   public List<Dependency> getDependencies()
   {
      return dependencies;
   }

   public boolean isUseUIHarness()
   {
      return useUIHarness;
   }
   
   public void setUseUIHarness(boolean useUIHarness)
   {
      this.useUIHarness = useUIHarness;
   }
}
