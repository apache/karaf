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
package org.apache.felix.servicebinder;

/**
 * Instances created by the service binder, either via the
 * <tt>GenericActivator</tt> or the <tt>GenericFactory</tt>,
 * may implement this interface to receive notification of
 * object life cycle events. See each interface method for
 * a precise description of when the method is invoked.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
**/
public interface Lifecycle
{
    /**
     * This method is called after the instance is created, all of its
     * dependencies are valid, and all implemented services are registered.
    **/
    public void activate();

    /**
     * This method is called prior to instance disposal. At the time
     * of invocation, all dependencies are still valid and all services
     * are still registered. Be aware that at this point some dependent
     * services may have been shutdown and using them may result in
     * error conditions.
    **/
    public void deactivate();
}
