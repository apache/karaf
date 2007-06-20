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
package org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.rmi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.MarshalledObject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Marshaller/Unmarshaller for RMI's MarshalledObjects.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
class RMIMarshaller
{
   private static Method unmarshal;

   /**
    * MarshalledObject.get() loads the object it contains by using the first user-defined classloader it can find
    * in the stack frames of the call.
    * In a normal usage of JSR 160, this classloader is the one that loaded this class, most probably
    * the system classloader.
    * If the class cannot be found with that loader, then the RMI semantic is tried: first the thread context
    * classloader, then dynamic code download (if there is a security manager).
    * Here we load the Marshaller class using an URLClassLoader that is only able to load classes from the URL
    * where it loaded this class, thus it cannot see other classes in the system classloader.
    * This URLClassLoader then becomes the first user-defined classloader in the stack frames, but it will fail
    * to load anything else, thus allowing MarshalledObject.get() to use the thread context classloader.
    */
   static
   {
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               URL url = RMIMarshaller.class.getProtectionDomain().getCodeSource().getLocation();
               // TODO: is it enough to use the parent, or maybe better use null as parent classloader ?
               URLClassLoader loader = new URLClassLoader(new URL[] {url}, RMIMarshaller.class.getClassLoader().getParent());
               Class marshaller = loader.loadClass(Marshaller.class.getName());
               unmarshal = marshaller.getMethod("unmarshal", new Class[] {MarshalledObject.class});
               return null;
            }
         });
      }
      catch (PrivilegedActionException x)
      {
         throw new Error(x.toString());
      }
   }

   /**
    * Returns a MarshalledObject obtained by marshalling the given object.
    */
   public static MarshalledObject marshal(Object object) throws IOException
   {
      if (object == null) return null;
      return new MarshalledObject(object);
   }

   /**
    * Returns the unmarshalled object obtained unmarshalling the given MarshalledObject,
    * using as context classloader first the given mbeanLoader, if not null, then with the given defaultLoader.
    */
   public static Object unmarshal(MarshalledObject object, ClassLoader mbeanLoader, ClassLoader defaultLoader) throws IOException
   {
      if (object == null) return null;
      if (mbeanLoader == null) return unmarshal(object, defaultLoader);
      return unmarshal(object, new MarshallerClassLoader(mbeanLoader, defaultLoader));
   }

   private static Object unmarshal(MarshalledObject object, ClassLoader loader) throws IOException
   {
      if (loader != null)
      {
         ClassLoader old = Thread.currentThread().getContextClassLoader();
         try
         {
            setContextClassLoader(loader);
            return unmarshal(object);
         }
         catch (IOException x)
         {
            throw x;
         }
         catch (ClassNotFoundException ignored)
         {
         }
         finally
         {
            setContextClassLoader(old);
         }
      }
      throw new IOException("Cannot unmarshal " + object);
   }

   private static Object unmarshal(MarshalledObject marshalled) throws IOException, ClassNotFoundException
   {
      try
      {
         return unmarshal.invoke(null, new Object[]{marshalled});
      }
      catch (InvocationTargetException x)
      {
         Throwable t = x.getTargetException();
         if (t instanceof IOException) throw (IOException)t;
         if (t instanceof ClassNotFoundException) throw (ClassNotFoundException)t;
         throw new IOException(t.toString());
      }
      catch (Exception x)
      {
         throw new IOException(x.toString());
      }
   }

   private static void setContextClassLoader(final ClassLoader loader)
   {
      AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            Thread.currentThread().setContextClassLoader(loader);
            return null;
         }
      });
   }

   public static class Marshaller
   {
      public static Object unmarshal(MarshalledObject obj) throws IOException, ClassNotFoundException
      {
         return obj.get();
      }
   }

   private static class MarshallerClassLoader extends ClassLoader
   {
      private final ClassLoader defaultLoader;

      private MarshallerClassLoader(ClassLoader mbeanLoader, ClassLoader defaultLoader)
      {
         super(mbeanLoader);
         this.defaultLoader = defaultLoader;
      }

      protected Class findClass(String name) throws ClassNotFoundException
      {
         return defaultLoader.loadClass(name);
      }

      protected URL findResource(String name)
      {
         return defaultLoader.getResource(name);
      }
   }
}
