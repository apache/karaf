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
package org.apache.felix.scr;


import org.osgi.framework.ServiceReference;


/**
 * The <code>Reference</code> interface represents a single reference (or
 * dependency) to a service used by a Component.
 */
public interface Reference
{

    /**
     * Returns the name of this Reference. This method provides access to the
     * <code>name</code> attribute of the <code>referenec</code> element.
     */
    String getName();


    /**
     * Returns the name of the service used by this Reference. This method
     * provides access to the <code>interface</code> attribute of the
     * <code>reference</code> element.
     */
    String getServiceName();


    /**
     * Returns an array of references to the services bound to this Reference
     * or <code>null</code> if no services are currently bound.
     */
    ServiceReference[] getServiceReferences();


    /**
     * Returns whether this reference is satisified. A {@link #isOptional() optional}
     * component is always satsified. Otherwise <code>true</code> is only
     * returned if at least one service is bound.
     */
    boolean isSatisfied();


    /**
     * Returns whether this reference is optional. This method provides access
     * to the lower bound of the <code>cardinality</code> attribute of the
     * <code>reference</code> element. In other words, this method returns
     * <code>true</code> if the cardinality is <em>0..1</em> or <em>0..n</em>.
     */
    boolean isOptional();


    /**
     * Returns whether this reference is multiple. This method provides access
     * to the upper bound of the <code>cardinality</code> attribute of the
     * <code>reference</code> element. In other words, this method returns
     * <code>true</code> if the cardinality is <em>0..n</em> or <em>1..n</em>.
     */
    boolean isMultiple();


    /**
     * Returns <code>true</code> if the reference is defined with static policy.
     * This method provides access to the <code>policy</code> element of the
     * <code>reference</code> element. <code>true</code> is returned if the
     * policy is defined as <em>static</em>.
     */
    boolean isStatic();


    /**
     * Returns the value of the target property of this reference. Initially
     * (without overwriting configuration) this method provides access to the
     * <code>target</code> attribute of the <code>reference</code> element. If
     * configuration overwrites the target property, this method returns the
     * value of the Component property whose name is derived from the
     * {@link #getName() reference name} plus the suffix <em>.target</em>. If
     * no target property exists this method returns <code>null</code>.
     */
    String getTarget();


    /**
     * Returns the name of the method called if a service is being bound to
     * the Component or <code>null</code> if no such method is configued. This
     * method provides access to the <code>bind</code> attribute of the
     * <code>reference</code> element.
     */
    String getBindMethodName();


    /**
     * Returns the name of the method called if a service is being unbound from
     * the Component or <code>null</code> if no such method is configued. This
     * method provides access to the <code>unbind</code> attribute of the
     * <code>reference</code> element.
     */
    String getUnbindMethodName();

}
