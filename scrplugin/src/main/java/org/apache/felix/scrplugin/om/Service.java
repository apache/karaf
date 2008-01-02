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

import java.util.*;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * <code>Service</code>...
 *
 */
public class Service {

    protected String servicefactory;

    /** The list of implemented interfaces. */
    protected final List interfaces = new ArrayList();

    /**
     * Default constructor.
     */
    public Service() {
        // nothing to do
    }

    public String getServicefactory() {
        return this.servicefactory;
    }

    public void setServicefactory(String servicefactory) {
        this.servicefactory = servicefactory;
    }

    public void setServicefactory(boolean servicefactory) {
        this.servicefactory = String.valueOf(servicefactory);
    }

    public List getInterfaces() {
        return this.interfaces;
    }

    /**
     * Search for an implemented interface.
     * @param name The name of the interface.
     * @return The interface if it is implemented by this service or null.
     */
    public Interface findInterface(String name) {
        final Iterator i = this.interfaces.iterator();
        while ( i.hasNext() ) {
            final Interface current = (Interface)i.next();
            if ( current.getInterfacename().equals(name) ) {
                return current;
            }
        }
        return null;
    }

    /**
     * Add an interface to the list of interfaces.
     * @param interf The interface.
     */
    public void addInterface(Interface interf) {
        // add interface only once
        if ( this.findInterface(interf.getInterfacename()) == null ) {
            this.interfaces.add(interf);
        }
    }

    /**
     * Validate the service.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(List issues, List warnings)
    throws MojoExecutionException {
        final Iterator i = this.interfaces.iterator();
        while ( i.hasNext() ) {
            final Interface interf = (Interface)i.next();
            interf.validate(issues, warnings);
        }
    }

}
