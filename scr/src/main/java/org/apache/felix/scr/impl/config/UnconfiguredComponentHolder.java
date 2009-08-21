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
package org.apache.felix.scr.impl.config;


import java.util.Dictionary;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;


/**
 * The <code>SingletonHolder</code> class is {@link ComponentHolder} for a
 * component configured by a singleton configuration or no configuration
 * at all.
 */
public class UnconfiguredComponentHolder extends AbstractComponentHolder
{

    private final ImmediateComponentManager m_component;


    public UnconfiguredComponentHolder( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        super( activator, metadata );

        m_component = createComponentManager();
    }


    public void configurationDeleted( String pid )
    {
    }


    public void configurationUpdated( String pid, Dictionary props )
    {
    }


    public void enableComponents()
    {
        m_component.enable();
    }


    public void disableComponents()
    {
        m_component.disable();
    }


    public void disposeComponents( int reason )
    {
        m_component.dispose( reason );
    }


    public void disposed( ImmediateComponentManager component )
    {
        // nothing to do here...
    }
}
