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
package org.apache.felix.webconsole.internal;


import org.osgi.framework.BundleContext;


/**
 * OsgiManagerPlugin is an internal interface. When a plugin implements this
 * interface, the Web Console will run it's {@link #activate(BundleContext)} method upon
 * initialization and {@link #deactivate()}, when disposed.
 */
public interface OsgiManagerPlugin
{

    /**
     * This method is called from the Felix Web Console to ensure the
     * AbstractWebConsolePlugin is correctly setup.
     *
     * It is called right after the Web Console receives notification for
     * plugin registration.
     *
     * @param bundleContext the context of the plugin bundle
     */
    void activate( BundleContext bundleContext );


    /**
     * This method is called, by the Web Console to de-activate the plugin and release
     * all used resources.
     */
    void deactivate();

}
