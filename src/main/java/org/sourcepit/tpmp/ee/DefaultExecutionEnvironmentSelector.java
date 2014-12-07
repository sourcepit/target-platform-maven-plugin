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

package org.sourcepit.tpmp.ee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourcepit.common.constraints.NotNull;
import org.sourcepit.osgifier.core.ee.ExecutionEnvironment;
import org.sourcepit.osgifier.core.ee.ExecutionEnvironmentService;

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

   @Override
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
