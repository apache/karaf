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
package org.apache.karaf.management.boot;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KarafMBeanServerBuilder extends MBeanServerBuilder {

    private static volatile InvocationHandler guard;

    public static InvocationHandler getGuard() {
        return guard;
    }

    public static void setGuard(InvocationHandler guardHandler) {
        guard = guardHandler;
    }

    @Override
    public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate) {
        InvocationHandler handler = new MBeanInvocationHandler(super.newMBeanServer(defaultDomain, outer, delegate));
        return (MBeanServer) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ MBeanServer.class }, handler);
    }

    private static final class MBeanInvocationHandler implements InvocationHandler {

        private final MBeanServer wrapped;
        private final List<String> guarded = Collections.unmodifiableList(Arrays.asList("invoke", "getAttribute", "getAttributes", "setAttribute", "setAttributes"));

        MBeanInvocationHandler(MBeanServer mbeanServer) {
            wrapped = mbeanServer;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (guarded.contains(method.getName())) {
                if (KarafMBeanServerBuilder.guard == null) {
                    throw new IllegalStateException("KarafMBeanServerBuilder not initialized");
                }
                guard.invoke(proxy, method, args);
            }
            return method.invoke(wrapped, args);
        }

    }

}
