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

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

public class OsgifyExecutionEnvironmentSelectorTest extends InjectedTest {
   @Inject
   private DefaultExecutionEnvironmentSelector selector;

   @Test
   public void testNull() {
      try {
         selector.select(null);
         fail();
      }
      catch (IllegalArgumentException e) {
         // as expected
      }
   }

   @Test
   public void testEmpty() {
      assertThat(selector.select(new ArrayList<String>()), nullValue());
   }

   @Test
   public void testUnknown() {
      final List<String> ees = Arrays.asList("foo", "bar");
      assertThat(selector.select(ees), equalTo("foo"));
   }

   @Test
   public void testHighestIsSelected() {
      final List<String> ees = Arrays.asList("OSGi/Minimum-1.2", "foo", "CDC-1.1/Foundation-1.1", "JavaSE-1.6",
         "J2SE-1.3", "bar");
      assertThat(selector.select(ees), equalTo("JavaSE-1.6"));
   }
}
