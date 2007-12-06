/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scrplugin.tags.cl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.felix.scrplugin.tags.JavaMethod;
import org.apache.felix.scrplugin.tags.JavaParameter;

/**
 * <code>ClassLoaderJavaMethod.java</code>...
 *
 */
public class ClassLoaderJavaMethod implements JavaMethod {

    protected final Method method;

    protected boolean isConstructor = false;

    public ClassLoaderJavaMethod(Method m) {
        this.method = m;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaMethod#getName()
     */
    public String getName() {
        return this.method.getName();
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaMethod#getParameters()
     */
    public JavaParameter[] getParameters() {
        final JavaParameter[] params = new JavaParameter[this.method.getParameterTypes().length];
        for(int i=0; i<this.method.getParameterTypes().length; i++) {
            params[i] = new ClassLoaderJavaParameter(this.method.getParameterTypes()[i].getName());
        }
        return params;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaMethod#isConstructor()
     */
    public boolean isConstructor() {
        return this.isConstructor;
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaMethod#isProtected()
     */
    public boolean isProtected() {
        return Modifier.isProtected(this.method.getModifiers());
    }

    /**
     * @see org.apache.felix.scrplugin.tags.JavaMethod#isPublic()
     */
    public boolean isPublic() {
        return Modifier.isPublic(this.method.getModifiers());
    }

}
