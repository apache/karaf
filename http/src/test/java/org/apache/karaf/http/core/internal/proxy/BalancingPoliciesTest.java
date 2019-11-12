/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.http.core.internal.proxy;

import org.junit.Assert;
import org.junit.Test;

public class BalancingPoliciesTest {

    private static final String proxyTo = "A,B,C";

    @Test
    public void testSingleHost() throws Exception {
        String[] proxyTos = "A".split(",");
        Assert.assertEquals(1, proxyTos.length);
        Assert.assertEquals("A", proxyTos[0]);
    }

    @Test
    public void testRandomBalancingPolicy() throws Exception {
        RandomBalancingPolicy balancingPolicy = new RandomBalancingPolicy();
        String selected = balancingPolicy.selectHost(proxyTo.split(","));
        System.out.println("First call: " + selected);
        Assert.assertTrue(selected.equals("A") || selected.equals("B") || selected.equals("C"));
        selected = balancingPolicy.selectHost(proxyTo.split(","));
        System.out.println("Second call: " + selected);
        Assert.assertTrue(selected.equals("A") || selected.equals("B") || selected.equals("C"));
        selected = balancingPolicy.selectHost(proxyTo.split(","));
        System.out.println("Third call: " + selected);
        Assert.assertTrue(selected.equals("A") || selected.equals("B") || selected.equals("C"));
        selected = balancingPolicy.selectHost("A".split(","));
        System.out.println("Single host: " + selected);
        Assert.assertEquals("A", selected);
    }

    @Test
    public void testRoundRobinBalancingPolicy() throws Exception {
        RoundRobinBalancingPolicy balancingPolicy = new RoundRobinBalancingPolicy();
        String selected = balancingPolicy.selectHost(proxyTo.split(","));
        System.out.println("First call: " + selected);
        Assert.assertEquals("A", selected);
        selected = balancingPolicy.selectHost(proxyTo.split(","));
        System.out.println("Second call: " + selected);
        Assert.assertEquals("B", selected);
        selected = balancingPolicy.selectHost(proxyTo.split(","));
        System.out.println("Third call: " + selected);
        Assert.assertEquals("C", selected);
        selected = balancingPolicy.selectHost(proxyTo.split(","));
        System.out.println("Fourth call: " + selected);
        Assert.assertEquals("A", selected);
        selected = balancingPolicy.selectHost("A".split(","));
        System.out.println("Single host: " + selected);
        Assert.assertEquals("A", selected);
    }

}
