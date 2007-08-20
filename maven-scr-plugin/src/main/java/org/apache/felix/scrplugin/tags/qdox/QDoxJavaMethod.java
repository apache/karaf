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
package org.apache.felix.scrplugin.tags.qdox;

import org.apache.felix.scrplugin.tags.JavaMethod;
import org.apache.felix.scrplugin.tags.JavaParameter;

/**
 * <code>QDoxJavaMethod.java</code>...
 *
 */
public class QDoxJavaMethod implements JavaMethod {

    protected final com.thoughtworks.qdox.model.JavaMethod method;

    public QDoxJavaMethod(com.thoughtworks.qdox.model.JavaMethod m) {
        this.method = m;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaMethod#getName()
     */
    public String getName() {
        return this.method.getName();
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaMethod#getParameters()
     */
    public JavaParameter[] getParameters() {
        final com.thoughtworks.qdox.model.JavaParameter[] params = this.method.getParameters();
        if ( params == null || params.length == 0) {
            return new JavaParameter[0];
        }
        final JavaParameter[] p = new JavaParameter[params.length];
        for(int i=0; i<params.length; i++) {
            p[i] = new QDoxJavaParameter(params[i]);
        }
        return p;
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaMethod#isConstructor()
     */
    public boolean isConstructor() {
        return this.method.isConstructor();
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaMethod#isProtected()
     */
    public boolean isProtected() {
        return this.method.isProtected();
    }

    /**
     * @see org.apache.felix.sandbox.scrplugin.tags.JavaMethod#isPublic()
     */
    public boolean isPublic() {
        return this.method.isPublic();
    }
}
