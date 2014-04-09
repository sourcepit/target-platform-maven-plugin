/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
import java.lang.IllegalArgumentException;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

public class OsgifyExecutionEnvironmentSelectorTest extends InjectedTest
{
   @Inject
   private DefaultExecutionEnvironmentSelector selector;

   @Test
   public void testNull()
   {
      try
      {
         selector.select(null);
         fail();
      }
      catch (IllegalArgumentException e)
      {
         // as expected
      }
   }

   @Test
   public void testEmpty()
   {
      assertThat(selector.select(new ArrayList<String>()), nullValue());
   }

   @Test
   public void testUnknown()
   {
      final List<String> ees = Arrays.asList("foo", "bar");
      assertThat(selector.select(ees), equalTo("foo"));
   }

   @Test
   public void testHighestIsSelected()
   {
      final List<String> ees = Arrays.asList("OSGi/Minimum-1.2", "foo", "CDC-1.1/Foundation-1.1", "JavaSE-1.6",
         "J2SE-1.3", "bar");
      assertThat(selector.select(ees), equalTo("JavaSE-1.6"));
   }
}
