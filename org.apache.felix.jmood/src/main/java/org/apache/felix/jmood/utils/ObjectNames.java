/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.jmood.utils;

/**
 * This interface holds the <code>ObjectName</code>s under which the
 * core MBeans (<code>CoreControllerMBean</code>, <code>ManagedBundleMBean</code>, 
 * <code>ManagedServiceMBean</code>) and <code>ManagedPackageMBean</code>
 * Note that service, bundle and package data mbeans are created dynamically
 * and need a dynamic property to be added to the objectName.
 *
 */
public interface ObjectNames {
    public static final String CORE= "osgi.core";
    public static final String COMPENDIUM="osgi.compendium";
    public static final String CORE_CONTROLLER=CORE+":type=controller";
    public static final String FRAMEWORK=CORE+":type=framework";
    public static final String BUNDLE=CORE+":type=bundle, symbolicName=";
    public static final String SERVICE=CORE+":type=service, service.id=";
    public static final String CM_SERVICE=COMPENDIUM+":service=cm, type=manager";
    public static final String CM_OBJECT=COMPENDIUM+":service=cm, type=object,"; 
    public static final String LOG_SERVICE = COMPENDIUM+":service=log";
    public static final String UA_SERVICE = COMPENDIUM+":service=useradmin"; 

    /**
     * package mbean object names also contain a version property to 
     * avoid <code>InstanceAlreadyExistsException</code> when two
     * versions of the same package co-exist.  
     */
    public static final String PACKAGE=CORE+":type=package, name=";
    public static final String ALLBUNDLES=CORE+":type=bundle,*";
    public static final String ALLSERVICES=CORE+":type=service,*";
    public static final String ALLPACKAGES=CORE+":type=package,*";
    public static final String ALL_CM_OBJECT=COMPENDIUM+":service=cm, type=object,*";
    
}
