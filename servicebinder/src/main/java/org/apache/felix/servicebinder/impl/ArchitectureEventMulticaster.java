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
package org.apache.felix.servicebinder.impl;

import org.apache.felix.servicebinder.architecture.DependencyChangeEvent;
import org.apache.felix.servicebinder.architecture.InstanceChangeEvent;
import org.apache.felix.servicebinder.architecture.ServiceBinderListener;

/**
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ArchitectureEventMulticaster implements ServiceBinderListener
{
    /**
     * 
     * @uml.property name="a"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    protected ServiceBinderListener a;

    /**
     * 
     * @uml.property name="b"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    protected ServiceBinderListener b;

    protected ArchitectureEventMulticaster(ServiceBinderListener a, ServiceBinderListener b)    
    {        
        this.a = a;        
        this.b = b;    
    }    
    
    public void dependencyChanged(DependencyChangeEvent e)    
    {
        a.dependencyChanged(e);        
        b.dependencyChanged(e);
    }
    
    public void instanceReferenceChanged(InstanceChangeEvent e)    
    {
        a.instanceReferenceChanged(e);        
        b.instanceReferenceChanged(e);
    }
    
    public static ServiceBinderListener add(ServiceBinderListener a, ServiceBinderListener b)
    {
        if (a == null)
            return b;
        else if (b == null)
            return a;
        else
            return new ArchitectureEventMulticaster(a, b);
    }
    
    public static ServiceBinderListener remove(ServiceBinderListener a, ServiceBinderListener b)
    {
        if ((a == null) || (a == b))            
            return null;        
        else if (a instanceof ArchitectureEventMulticaster)            
            return add (remove (((ArchitectureEventMulticaster) a).a, b),remove (((ArchitectureEventMulticaster) a).b, b));        
        else            
            return a;    
    }
}
