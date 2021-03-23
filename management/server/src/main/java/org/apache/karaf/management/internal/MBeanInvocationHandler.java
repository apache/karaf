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
package org.apache.karaf.management.internal;

import javax.management.MBeanServer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MBeanInvocationHandler implements InvocationHandler {

    private final MBeanServer wrapped;

    private final InvocationHandler guard;

    private final List<String> guarded = Collections.unmodifiableList(Arrays.asList("invoke", "getAttribute", "getAttributes", "setAttribute", "setAttributes"));

    public MBeanInvocationHandler(MBeanServer mBeanServer, InvocationHandler guard) {
        wrapped = mBeanServer;
        this.guard = guard;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (guarded.contains(method.getName())) {
            guard.invoke(proxy, method, args);
        }

        if (method.getName().equals("equals") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class) {
            Object target = args[0];
            if (target != null && Proxy.isProxyClass(target.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(target);
                if (handler instanceof MBeanInvocationHandler) {
                    args[0] = ((MBeanInvocationHandler) handler).wrapped;
                }
            }
        } else if (method.getName().equals("finalize") && method.getParameterTypes().length == 0) {
            // special case finalize, don't route through to delegate because that will get its own call
            return null;
        }

        
            
        try {
            return AccessController.doPrivilegedWithCombiner(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    try {
                        return method.invoke(wrapped, args);
                    } catch (InvocationTargetException e) {
                        return null;
                    }
                }
            });
        } catch (Exception pae) {
            Throwable cause = pae.getCause();
            throw cause == null ? pae:cause;
        }
        
    }

    public MBeanServer getDelegate() {
        return wrapped;
    }
}
