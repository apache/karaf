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
package org.apache.felix.moduleloader;

import java.util.EventObject;

/**
 * <p>
 * This is an event class that is used by the <tt>ModuleManager</tt> to
 * indicate when modules are added, removed, or reset. To receive these
 * events, a <tt>ModuleListener</tt> must be added to the <tt>ModuleManager</tt>
 * instance.
 * </p>
 * @see org.apache.felix.moduleloader.ModuleFactoryImpl
 * @see org.apache.felix.moduleloader.ModuleImpl
 * @see org.apache.felix.moduleloader.ModuleListener
**/
public class ModuleEvent extends EventObject
{
    private IModule m_module = null;

    /**
     * <p>
     * Constructs a module event with the specified <tt>ModuleManager</tt>
     * as the event source and the specified module as the subject of
     * the event.
     * </p>
     * @param mgr the source of the event.
     * @param module the subject of the event.
    **/
    public ModuleEvent(IModule module)
    {
        super(module);
        m_module = module;
    }

    /**
     * <p>
     * Returns the module that is the subject of the event.
     * </p>
     * @return the module that is the subject of the event.
    **/
    public IModule getModule()
    {
        return m_module;
    }
}