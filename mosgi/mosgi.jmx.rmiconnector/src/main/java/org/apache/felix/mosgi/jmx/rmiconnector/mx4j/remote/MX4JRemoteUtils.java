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
package org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import javax.security.auth.AuthPermission;
import javax.security.auth.Policy;
import javax.security.auth.Subject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.remote.SubjectDelegationPermission;

/**
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.2 $
 */
public class MX4JRemoteUtils
{
   private static int connectionNumber;

   /**
    * Returns a copy of the given Map that does not contain non-serializable entries
    */
   public static Map removeNonSerializableEntries(Map map)
   {
      Map newMap = new HashMap(map.size());
      for (Iterator i = map.entrySet().iterator(); i.hasNext();)
      {
         Map.Entry entry = (Map.Entry)i.next();
         if (isSerializable(entry)) newMap.put(entry.getKey(), entry.getValue());
      }
      return newMap;
   }

   private static boolean isSerializable(Object object)
   {
      if (object instanceof Map.Entry) return isSerializable(((Map.Entry)object).getKey()) && isSerializable(((Map.Entry)object).getValue());
      if (object == null) return true;
      if (object instanceof String) return true;
      if (object instanceof Number) return true;
      if (!(object instanceof Serializable)) return false;

      return isTrulySerializable(object);
   }

   public static boolean isTrulySerializable(Object object)
   {
      // Give up and serialize the object
      try
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(object);
         oos.close();
         return true;
      }
      catch (IOException ignored)
      {
      }
      return false;
   }

   public static String createConnectionID(String protocol, String callerAddress, int callerPort, Subject subject)
   {
      // See JSR 160 specification at javax/management/remote/package-summary.html

      StringBuffer buffer = new StringBuffer(protocol);
      buffer.append(':');
      if (callerAddress != null) buffer.append("//").append(callerAddress);
      if (callerPort >= 0) buffer.append(':').append(callerPort);
      buffer.append(' ');

      if (subject != null)
      {
         Set principals = subject.getPrincipals();
         for (Iterator i = principals.iterator(); i.hasNext();)
         {
            Principal principal = (Principal)i.next();
            String name = principal.getName();
            name = name.replace(' ', '_');
            buffer.append(name);
            if (i.hasNext()) buffer.append(';');
         }
      }
      buffer.append(' ');

      buffer.append("0x").append(Integer.toHexString(getNextConnectionNumber()));

      return buffer.toString();
   }

   private static synchronized int getNextConnectionNumber()
   {
      return ++connectionNumber;
   }

   public static Object subjectInvoke(Subject subject, Subject delegate, AccessControlContext context, PrivilegedExceptionAction action) throws Exception
   {
      if (delegate != null)
      {
         if (subject == null) throw new SecurityException("There is no authenticated subject to delegate to");
         checkSubjectDelegationPermission(delegate, getSubjectContext(subject, context));
      }

      if (subject == null)
      {
         if (context == null) return action.run();
         try
         {
            return AccessController.doPrivileged(action, context);
         }
         catch (PrivilegedActionException x)
         {
            throw x.getException();
         }
      }

      try
      {
         AccessControlContext subjectContext = getSubjectContext(subject, context);
         return Subject.doAsPrivileged(subject, action, subjectContext);
      }
      catch (PrivilegedActionException x)
      {
         throw x.getException();
      }
   }

   private static void checkSubjectDelegationPermission(final Subject delegate, AccessControlContext context) throws SecurityException
   {
      final SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         AccessController.doPrivileged(new PrivilegedAction()
         {
            public Object run()
            {
               StringBuffer buffer = new StringBuffer();
               Set principals = delegate.getPrincipals();
               for (Iterator i = principals.iterator(); i.hasNext();)
               {
                  Principal principal = (Principal)i.next();
                  buffer.setLength(0);
                  String permission = buffer.append(principal.getClass().getName()).append(".").append(principal.getName()).toString();
                  sm.checkPermission(new SubjectDelegationPermission(permission));
               }
               return null;
            }
         }, context);
      }
   }

   /**
    * Returns a suitable AccessControlContext that restricts access in a {@link Subject#doAsPrivileged} call
    * based on the current JAAS authorization policy, and combined with the given context.
    *
    * This is needed because the server stack frames in a call to a JMXConnectorServer are,
    * for example for RMI, like this:
    * <pre>
    * java.lang.Thread.run()
    *   [rmi runtime classes]
    *     javax.management.remote.rmi.RMIConnectionImpl
    *       [mx4j JSR 160 implementation code]
    *         javax.security.auth.Subject.doAsPrivileged()
    *           [mx4j JSR 160 implementation code]
    *             [mx4j JSR 3 implementation code]
    * </pre>
    * All protection domains in this stack frames have AllPermission, normally, and the Subject.doAsPrivileged()
    * call stops the checks very early. <br>
    *
    * So we need a restricting context (created at the start() of the connector server), and furthermore we need
    * to combine the restricting context with a "special" context that does not have the same location as the
    * JSR 3 and 160 classes and implementation (in particular will have a null location). <br>
    * The "injection" of this synthetic ProtectionDomain allows to give AllPermission to the JSR 3 and 160 classes
    * and implementation, but still have the possibility to specify a JAAS policy with MBeanPermissions in this way:
    * <pre>
    * grant principal javax.management.remote.JMXPrincipal "mx4j"
    * {
    *    permission javax.management.MBeanPermission "*", "getAttribute";
    * };
    * </pre>
    */
   private static AccessControlContext getSubjectContext(final Subject subject, final AccessControlContext context)
   {
      final SecurityManager sm = System.getSecurityManager();
      if (sm == null)
      {
         return context;
      }
      else
      {
         return (AccessControlContext)AccessController.doPrivileged(new PrivilegedAction()
         {
            public Object run()
            {
               InjectingDomainCombiner combiner = new InjectingDomainCombiner(subject);
               AccessControlContext acc = new AccessControlContext(context, combiner);
               AccessController.doPrivileged(new PrivilegedAction()
               {
                  public Object run()
                  {
                     // Check this permission, that is required anyway, to combine the domains
                     sm.checkPermission(new AuthPermission("doAsPrivileged"));
                     return null;
                  }
               }, acc);
               ProtectionDomain[] combined = combiner.getCombinedDomains();
               return new AccessControlContext(combined);
            }
         });
      }
   }

   private static class InjectingDomainCombiner implements DomainCombiner
   {
      private static Constructor domainConstructor;

      static
      {
         try
         {
            domainConstructor = ProtectionDomain.class.getConstructor(new Class[]{CodeSource.class, PermissionCollection.class, ClassLoader.class, Principal[].class});
         }
         catch (Exception x)
         {
         }
      }

      private ProtectionDomain domain;
      private ProtectionDomain[] combined;

      public InjectingDomainCombiner(Subject subject)
      {
         if (domainConstructor != null)
         {
            Principal[] principals = (Principal[])subject.getPrincipals().toArray(new Principal[0]);
            try
            {
               domain = (ProtectionDomain)domainConstructor.newInstance(new Object[]{new CodeSource(null, (java.security.cert.Certificate[])null), null, null, principals});
            }
            catch (Exception x)
            {
            }
         }

         if (domain == null)
         {
            // This is done for JDK 1.3 compatibility.
            domain = new SubjectProtectionDomain(new CodeSource(null, (java.security.cert.Certificate[])null), subject);
         }
      }

      public ProtectionDomain[] combine(ProtectionDomain[] current, ProtectionDomain[] assigned)
      {
         int length = current.length;
         ProtectionDomain[] result = null;
         if (assigned == null || assigned.length == 0)
         {
            result = new ProtectionDomain[length + 1];
            System.arraycopy(current, 0, result, 0, length);
         }
         else
         {
            result = new ProtectionDomain[length + assigned.length + 1];
            System.arraycopy(current, 0, result, 0, length);
            System.arraycopy(assigned, 0, result, length, assigned.length);
         }
         result[result.length - 1] = domain;
         this.combined = result;
         return result;
      }

      public ProtectionDomain[] getCombinedDomains()
      {
         return combined;
      }

      private static class SubjectProtectionDomain extends ProtectionDomain
      {
         private final Subject subject;

         public SubjectProtectionDomain(CodeSource codesource, Subject subject)
         {
            super(codesource, null);
            this.subject = subject;
         }

         public boolean implies(Permission permission)
         {
            Policy policy = (Policy)AccessController.doPrivileged(new PrivilegedAction()
            {
               public Object run()
               {
                  return Policy.getPolicy();
               }
            });
            PermissionCollection permissions = policy.getPermissions(subject, getCodeSource());
            return permissions.implies(permission);
         }
      }
   }
}
