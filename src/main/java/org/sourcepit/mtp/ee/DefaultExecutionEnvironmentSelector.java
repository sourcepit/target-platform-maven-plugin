/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp.ee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcepit.osgify.core.ee.ExecutionEnvironment;
import org.sourcepit.osgify.core.ee.ExecutionEnvironmentService;

@Named
public class DefaultExecutionEnvironmentSelector implements ExecutionEnvironmentSelector
{
   private final ExecutionEnvironmentService eeService;

   private final Logger log = LoggerFactory.getLogger(getClass());

   @Inject
   public DefaultExecutionEnvironmentSelector(ExecutionEnvironmentService eeService)
   {
      this.eeService = eeService;
   }

   public String select(@NotNull Collection<String> executionEnvironments)
   {
      if (executionEnvironments.isEmpty())
      {
         return null;
      }

      final List<String> knownAndSortedIds = getKnownAndSortedIds();
      
      // check if we know each ee
      for (String eeId : executionEnvironments)
      {
         if (!knownAndSortedIds.contains(eeId))
         {
            log.warn("Unknown execution environment: " + eeId);
         }
      }
      
      // return highest known ee
      for (String eeId : knownAndSortedIds)
      {
         if (executionEnvironments.contains(eeId))
         {
            return eeId;
         }
      }

      // fallback... return first
      log.warn("Unable to determine a known execution environment from " + executionEnvironments
         + ". Selecting the first one");

      return executionEnvironments.iterator().next();
   }

   private List<String> getKnownAndSortedIds()
   {
      final List<String> knownAndSortedIds = new ArrayList<String>();

      final List<ExecutionEnvironment> ees = eeService.getExecutionEnvironments();
      for (int i = ees.size() - 1; i > -1; i--)
      {
         final ExecutionEnvironment ee = ees.get(i);
         final String eeId = ee.getId();
         knownAndSortedIds.add(eeId);
      }
      return knownAndSortedIds;
   }
}
