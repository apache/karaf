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
package org.apache.felix.scrplugin.om;

import java.util.List;

import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * <code>Interface.java</code>...
 *
 */
public class Interface extends AbstractObject {

    protected String interfacename;

    /**
     * Default constructor.
     */
    public Interface() {
        this(null);
    }

    /**
     * Constructor from java source.
     */
    public Interface(JavaTag t) {
        super(t);
    }

    public String getInterfacename() {
        return this.interfacename;
    }

    public void setInterfacename(String name) {
        this.interfacename = name;
    }

    /**
     * Validate the interface.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(List<String> issues, List<String> warnings)
    throws MojoExecutionException {
        final JavaClassDescription javaClass = this.tag.getJavaClassDescription();
        if (javaClass == null) {
            issues.add(this.getMessage("Must be declared in a Java class"));
        } else {

            if ( !javaClass.isA(this.getInterfacename()) ) {
               // interface not implemented
                issues.add(this.getMessage("Class must implement provided interface " + this.getInterfacename()));
            }
        }
    }
}
