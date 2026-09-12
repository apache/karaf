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
import java.io.InputStream;

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

    @Test
    public void proxyInterface() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        final Class<?> proxyClass = factory.createProxyClass(
                classLoader, Bar.class.getName() + "$$ProxyTestProxy2",
                new Class<?>[]{Bar.class},
                Bar.class.getDeclaredMethods());
        assertNotNull(proxyClass);

        // an interface proxy extends Object, so this also covers the generated no-arg constructor
        final Bar instance = Bar.class.cast(factory.create(proxyClass,
                (method, args) -> method.getName() + "(" + asList(args) + ")"));
        assertEquals("bar([])", instance.bar());
        assertEquals("baz([param])", instance.baz("param"));
    }

    @Test
    public void proxyWithoutNoArgConstructor() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        try {
            factory.createProxyClass(
                    classLoader, Unproxyable.class.getName() + "$$ProxyTestProxy3",
                    new Class<?>[]{Unproxyable.class},
                    Unproxyable.class.getDeclaredMethods());
            fail();
        } catch (final IllegalArgumentException iae) {
            assertEquals("Cannot proxy " + Unproxyable.class.getName() + ", it has no no-arg constructor",
                    iae.getMessage());
        }
    }

    @Test
    public void proxyWithPackagePrivateConstructor() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        try {
            factory.createProxyClass(
                    classLoader, PackagePrivateConstructor.class.getName() + "$$ProxyTestProxy4",
                    new Class<?>[]{PackagePrivateConstructor.class},
                    PackagePrivateConstructor.class.getDeclaredMethods());
            fail();
        } catch (final IllegalArgumentException iae) {
            assertEquals("Cannot proxy " + PackagePrivateConstructor.class.getName()
                            + ", its no-arg constructor is not accessible from the generated proxy",
                    iae.getMessage());
        }
    }

    @Test
    public void proxyWithProtectedConstructor() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        final Class<?> proxyClass = factory.createProxyClass(
                classLoader, ProtectedConstructor.class.getName() + "$$ProxyTestProxy5",
                new Class<?>[]{ProtectedConstructor.class},
                ProtectedConstructor.class.getDeclaredMethods());
        assertNotNull(proxyClass);

        // a protected super constructor is reachable from a subclass, even in another runtime package
        final ProtectedConstructor instance = ProtectedConstructor.class.cast(factory.create(proxyClass,
                (method, args) -> method.getName() + "(" + asList(args) + ")"));
        assertEquals("some([])", instance.some());
    }

    @Test
    public void proxyOfNonPublicClass() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        try {
            factory.createProxyClass(
                    classLoader, NotPublic.class.getName() + "$$ProxyTestProxy6",
                    new Class<?>[]{NotPublic.class},
                    NotPublic.class.getDeclaredMethods());
            fail();
        } catch (final IllegalArgumentException iae) {
            assertEquals("Cannot proxy " + NotPublic.class.getName()
                            + ", it is not public and therefore not visible to the generated proxy",
                    iae.getMessage());
        }
    }

    @Test
    public void proxyOfFinalClass() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        try {
            factory.createProxyClass(
                    classLoader, FinalClass.class.getName() + "$$ProxyTestProxy8",
                    new Class<?>[]{FinalClass.class},
                    FinalClass.class.getDeclaredMethods());
            fail();
        } catch (final IllegalArgumentException iae) {
            assertEquals("Cannot proxy " + FinalClass.class.getName()
                            + ", it is final and the generated proxy cannot extend it",
                    iae.getMessage());
        }
    }

    @Test
    public void proxyOfNonPublicInterface() {
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        try {
            factory.createProxyClass(
                    classLoader, NotPublicInterface.class.getName() + "$$ProxyTestProxy9",
                    new Class<?>[]{NotPublicInterface.class},
                    NotPublicInterface.class.getDeclaredMethods());
            fail();
        } catch (final IllegalArgumentException iae) {
            assertEquals("Cannot proxy " + NotPublicInterface.class.getName()
                            + ", it is not public and therefore not visible to the generated proxy",
                    iae.getMessage());
        }
    }

    @Test
    public void proxyWithNonPublicSecondaryInterface() {
        // classesToProxy[0] (Foo) alone is proxyable; the check must still walk the rest of the
        // array, since every interface in it is added to the proxy's implements clause
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(Thread.currentThread().getContextClassLoader(), null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        try {
            factory.createProxyClass(
                    classLoader, Foo.class.getName() + "$$ProxyTestProxy10",
                    new Class<?>[]{Foo.class, NotPublicInterface.class},
                    Foo.class.getDeclaredMethods());
            fail();
        } catch (final IllegalArgumentException iae) {
            assertEquals("Cannot proxy " + NotPublicInterface.class.getName()
                            + ", it is not public and therefore not visible to the generated proxy",
                    iae.getMessage());
        }
    }

    @Test
    public void proxyWithUnresolvableConstructorParameter() throws Exception {
        // reflecting on the constructors resolves the parameter types of all of them, so a type which
        // is not wired here must not make the proxyability checks reject an otherwise fine class
        final ClassLoader blocking = new BlockingClassLoader(Thread.currentThread().getContextClassLoader(),
                Absent.class.getName(), UnresolvableParameter.class.getName());
        final Class<?> classToProxy = blocking.loadClass(UnresolvableParameter.class.getName());
        final ProxyFactory.ProxyClassLoader classLoader = new ProxyFactory.ProxyClassLoader(blocking, null);
        final AsmProxyFactory factory = new AsmProxyFactory();
        final Class<?> proxyClass = factory.createProxyClass(
                classLoader, UnresolvableParameter.class.getName() + "$$ProxyTestProxy7",
                new Class<?>[]{classToProxy},
                classToProxy.getDeclaredMethods());
        assertNotNull(proxyClass);

        // the proxied class comes from another loader, so the instance is driven reflectively
        final Object instance = factory.create(proxyClass,
                (method, args) -> method.getName() + "(" + asList(args) + ")");
        assertEquals("some([])", proxyClass.getMethod("some").invoke(instance));
    }

    public interface Bar {
        String bar();

        String baz(String some);
    }

    public static class Unproxyable {
        public Unproxyable(final String some) {
            // no no-arg constructor on purpose
        }

        public String some() {
            return "some";
        }
    }

    public static class PackagePrivateConstructor {
        PackagePrivateConstructor() {
            // not accessible from the generated proxy on purpose
        }

        public String some() {
            return "some";
        }
    }

    public static class ProtectedConstructor {
        protected ProtectedConstructor() {
            // no-op
        }

        public String some() {
            return "some";
        }
    }

    static class NotPublic {
        // an accessible constructor is not enough as long as the class itself is not visible
        public NotPublic() {
            // no-op
        }

        public String some() {
            return "some";
        }
    }

    public static final class FinalClass {
        public FinalClass() {
            // no-op
        }

        public String some() {
            return "some";
        }
    }

    interface NotPublicInterface {
        String some();
    }

    public static class Absent {
    }

    public static class UnresolvableParameter {
        public UnresolvableParameter() {
            // the one the proxy invokes
        }

        public UnresolvableParameter(final Absent absent) {
            // its parameter type is hidden by BlockingClassLoader
        }

        public String some() {
            return "some";
        }
    }

    /**
     * Defines {@code reloaded} itself and refuses {@code blocked}, to mimic a type which is not wired
     * in the bundle the proxied class comes from.
     */
    static class BlockingClassLoader extends ClassLoader {
        private final String blocked;
        private final String reloaded;

        BlockingClassLoader(final ClassLoader parent, final String blocked, final String reloaded) {
            super(parent);
            this.blocked = blocked;
            this.reloaded = reloaded;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (blocked.equals(name)) {
                throw new ClassNotFoundException(name);
            }
            if (!reloaded.equals(name)) {
                return super.loadClass(name, resolve);
            }
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                try (final InputStream stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    final byte[] bytes = stream.readAllBytes();
                    clazz = defineClass(name, bytes, 0, bytes.length);
                } catch (final IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
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
