/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.agent.mx4j.server;

import javax.management.loading.ClassLoaderRepository;

/**
 * Base class to extend to create custom ClassLoaderRepositories.
 * MX4J's MBeanServer can use a custom ClassLoaderRepository instead of the default one
 * by simply specifying a suitable system property, see {@link mx4j.MX4JSystemKeys}.
 * It must be a class, otherwise it opens up a security hole, as anyone can cast the MBeanServer's
 * ClassLoaderRepository down to this class and call addClassLoader or removeClassLoader
 * since, if this class is an interface, they must be public.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public abstract class ModifiableClassLoaderRepository implements ClassLoaderRepository
{
   /**
    * Adds, if does not already exist, the specified ClassLoader to this repository.
    * @param cl The classloader to add
    * @see #removeClassLoader
    */
	protected abstract void addClassLoader(ClassLoader cl);

   /**
    * Removes, if exists, the specified ClassLoader from this repository.
    * @param cl The classloader to remove
    * @see #addClassLoader
    */
	protected abstract void removeClassLoader(ClassLoader cl);
}
