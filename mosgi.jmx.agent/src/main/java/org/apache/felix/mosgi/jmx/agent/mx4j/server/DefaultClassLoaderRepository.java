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

import java.util.ArrayList;

import javax.management.loading.MLet;

/**
 * Default implementation of a ClassLoaderRepository
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.2 $
 */
public class DefaultClassLoaderRepository extends ModifiableClassLoaderRepository implements java.io.Serializable
{
   private static final int WITHOUT = 1;
   private static final int BEFORE = 2;

   private ArrayList classLoaders = new ArrayList();

   public Class loadClass(String className) throws ClassNotFoundException
   {
      return loadClassWithout(null, className);
   }

   public Class loadClassWithout(ClassLoader loader, String className) throws ClassNotFoundException
   {
       return loadClassFromRepository(loader, className, WITHOUT);
   }

   public Class loadClassBefore(ClassLoader loader, String className) throws ClassNotFoundException
   {
       return loadClassFromRepository(loader, className, BEFORE);
   }

   protected void addClassLoader(ClassLoader cl)
   {
      if (cl == null) return;

      ArrayList loaders = getClassLoaders();
      synchronized (loaders)
      {
         if (!loaders.contains(cl)) loaders.add(cl);
      }
   }

   protected void removeClassLoader(ClassLoader cl)
   {
      if (cl == null) return;

      ArrayList loaders = getClassLoaders();
      synchronized (loaders)
      {
         loaders.remove(cl);
      }
   }

   protected ArrayList cloneClassLoaders()
   {
      ArrayList loaders = getClassLoaders();
      synchronized (loaders)
      {
         return (ArrayList)loaders.clone();
      }
   }

   protected ArrayList getClassLoaders()
   {
       return classLoaders;
   }

   private Class loadClassFromRepository(ClassLoader loader, String className, int algorithm) throws ClassNotFoundException
   {
      ArrayList copy = cloneClassLoaders();
      for (int i = 0; i < copy.size(); ++i)
      {
         try
         {
            ClassLoader cl = (ClassLoader)copy.get(i);
            if (cl.equals(loader))
            {
               if (algorithm == BEFORE) break;
               else continue;
            }

            return loadClass(cl, className);
         }
         catch (ClassNotFoundException ignored)
         {
         }
      }
      throw new ClassNotFoundException(className);
   }

   private Class loadClass(ClassLoader loader, String className) throws ClassNotFoundException
   {
      // This is an optimization: if the classloader is an MLet (and not a subclass)
      // then the method MLet.loadClass(String, ClassLoaderRepository) is used.
      if (loader.getClass() == MLet.class) return ((MLet)loader).loadClass(className, null);
      return loader.loadClass(className);
   }

   private int getSize()
   {
      ArrayList loaders = getClassLoaders();
      synchronized (loaders)
      {
         return loaders.size();
      }
   }
}
