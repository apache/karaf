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
package org.apache.karaf.service.interceptor.impl.runtime.proxy;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

public class AsmProxyFactoryTest {
    @Test
    public void proxy() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        final Class<?> proxyClass = factory.createProxyClass(
                classLoader, Foo.class.getName() + "$$ProxyTestProxy1",
                new Class<?>[]{Foo.class},
                Foo.class.getDeclaredMethods());
        assertNotNull(proxyClass);

        final Foo instance = Foo.class.cast(factory.create(proxyClass, (method, args) -> {
            switch (method.getName()) {
                case "fail":
                    throw new IOException("it must be a checked exception to ensure it is well propagated");
                default:
                    return method.getName() + "(" + asList(args) + ")";
            }
        }));
        assertEquals("foo1([])", instance.foo1());
        assertEquals("foo2([param])", instance.foo2("param"));
        assertTrue(instance.toString().startsWith(Foo.class.getName() + "$$ProxyTestProxy1@"));
        try {
            instance.fail();
            fail();
        } catch (final IOException e) {
            assertEquals("it must be a checked exception to ensure it is well propagated", e.getMessage());
        }
    }

    public static class Foo {
        public String foo1() {
            return "first";
        }

        public String foo2(final String some) {
            return "second<" + some + ">";
        }

        public String fail() throws IOException {
            return "ok";
        }
    }
}
