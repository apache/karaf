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
package org.apache.felix.mosgi.jmx.agent.mx4j.loading;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.lang.reflect.Proxy;

/**
 * ObjectInputStream that can read serialized java Objects using a supplied classloader
 * to find the object's classes.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream
{
   private ClassLoader classLoader;

   /**
    * Creates a new ClassLoaderObjectInputStream
    * @param stream The decorated stream
    * @param classLoader The ClassLoader used to load classes
    */
   public ClassLoaderObjectInputStream(InputStream stream, ClassLoader classLoader) throws IOException, StreamCorruptedException
   {
      super(stream);
      if (classLoader == null) throw new IllegalArgumentException("Classloader cannot be null");
      this.classLoader = classLoader;
   }

   protected Class resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException
   {
      String name = osc.getName();
      return loadClass(name);
   }

   protected Class resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException
   {
      Class[] classes = new Class[interfaces.length];
      for (int i = 0; i < interfaces.length; ++i) classes[i] = loadClass(interfaces[i]);

      return Proxy.getProxyClass(classLoader, classes);
   }

   private Class loadClass(String name) throws ClassNotFoundException
   {
      return classLoader.loadClass(name);
   }
}
