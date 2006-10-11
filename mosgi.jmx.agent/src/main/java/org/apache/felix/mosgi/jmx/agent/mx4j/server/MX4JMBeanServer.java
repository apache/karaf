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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.BadAttributeValueExpException;
import javax.management.BadBinaryOpValueExpException;
import javax.management.BadStringOperationException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidApplicationException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMRuntimeException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanPermission;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.MBeanServerPermission;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeOperationsException;
import javax.management.StandardMBean;
import javax.management.loading.ClassLoaderRepository;
import javax.management.loading.PrivateClassLoader;

import org.apache.felix.mosgi.jmx.agent.mx4j.ImplementationException;
import org.apache.felix.mosgi.jmx.agent.mx4j.MX4JSystemKeys;
import org.apache.felix.mosgi.jmx.agent.mx4j.loading.ClassLoaderObjectInputStream;
import org.apache.felix.mosgi.jmx.agent.mx4j.log.Log;
import org.apache.felix.mosgi.jmx.agent.mx4j.log.Logger;
import org.apache.felix.mosgi.jmx.agent.mx4j.server.interceptor.InvokerMBeanServerInterceptor;
import org.apache.felix.mosgi.jmx.agent.mx4j.server.interceptor.MBeanServerInterceptor;
import org.apache.felix.mosgi.jmx.agent.mx4j.server.interceptor.MBeanServerInterceptorConfigurator;
import org.apache.felix.mosgi.jmx.agent.mx4j.util.Utils;

/**
 * The MX4J MBeanServer implementation. <br>
 * The MBeanServer accomplishes these roles:
 * <ul>
 * <li> Returns information about the Agent
 * <li> Acts as a repository for MBeans
 * <li> Acts as an invoker, on behalf of the user, on MBeans
 * </ul>
 * <br>
 * The repository function is delegated to instances of {@link MBeanRepository} classes.
 * This class acts as a factory for MBeanRepository instances, that can be controlled via the system property
 * {@link mx4j.MX4JSystemKeys#MX4J_MBEANSERVER_REPOSITORY} to the qualified name of the implementation class. <br>
 *
 * This class also acts as an invoker on MBeans. The architecture is interceptor-based, that is whenever you call
 * from a client an MBeanServer method that will end up to call the MBean instance, the call is dispatched to
 * the interceptor chain and eventually to the MBean. <br>
 * The interceptors are configurable via the MBean {@link MBeanServerInterceptorConfigurator}.
 * When the call is about to arrive to the MBean instance, the last interceptor dispatches the call depending on
 * the MBean type: if the MBean is a dynamic MBean, the call is dispatched directly; if the MBean is a standard
 * MBean an {@link MBeanInvoker} is delegated to invoke on the MBean instance.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.3 $
 */
public class MX4JMBeanServer implements MBeanServer, java.io.Serializable
{
   private String defaultDomain;
   private MBeanRepository mbeanRepository;
   private MBeanServerDelegate delegate;
   private ObjectName delegateName;
   private MBeanIntrospector introspector;
   private MBeanServerInterceptorConfigurator invoker;
   private static long notifications;
   private ModifiableClassLoaderRepository classLoaderRepository;
   private Map domains = new HashMap();

   private static final String[] EMPTY_PARAMS = new String[0];
   private static final Object[] EMPTY_ARGS = new Object[0];

   /**
    * Create a new MBeanServer implementation with the specified default domain.
    * If the default domain is null, then the empty string is assumed.
    *
    * @param defaultDomain The default domain to be used
    * @throws SecurityException if access is not granted to create an MBeanServer instance
    */
   public MX4JMBeanServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate)
   {
      Logger logger = getLogger();
      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Creating MBeanServer instance...");

      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Checking permission to create MBeanServer...");
         sm.checkPermission(new MBeanServerPermission("newMBeanServer"));
      }

      if (defaultDomain == null) defaultDomain = "";
      this.defaultDomain = defaultDomain;

      if (delegate==null) throw new JMRuntimeException("Delegate can't be null");
      this.delegate = delegate;

      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("MBeanServer default domain is: '" + this.defaultDomain + "'");

      mbeanRepository = createMBeanRepository();

      classLoaderRepository = createClassLoaderRepository();
      // JMX 1.2 requires the CLR to have as first entry the classloader of this class
      classLoaderRepository.addClassLoader(getClass().getClassLoader());

      introspector = new MBeanIntrospector();

      // This is the official name of the delegate, it is used as a source for MBeanServerNotifications
      try
      {
         delegateName = new ObjectName("JMImplementation", "type", "MBeanServerDelegate");
      }
      catch (MalformedObjectNameException ignored)
      {
      }

      try
      {
         ObjectName invokerName = new ObjectName(MBeanServerInterceptorConfigurator.OBJECT_NAME);
         invoker = new MBeanServerInterceptorConfigurator(this);

//         ContextClassLoaderMBeanServerInterceptor ccl = new ContextClassLoaderMBeanServerInterceptor();
//         NotificationListenerMBeanServerInterceptor notif = new NotificationListenerMBeanServerInterceptor();
//         SecurityMBeanServerInterceptor sec = new SecurityMBeanServerInterceptor();
         InvokerMBeanServerInterceptor inv = new InvokerMBeanServerInterceptor(outer==null ? this : outer);

//         invoker.addPreInterceptor(ccl);
//         invoker.addPreInterceptor(notif);
//         invoker.addPostInterceptor(sec);
         invoker.addPostInterceptor(inv);

         invoker.start();

         // The interceptor stack is in place, register the configurator and all interceptors
         privilegedRegisterMBean(invoker, invokerName);

//         ObjectName cclName = new ObjectName("JMImplementation", "interceptor", "contextclassloader");
//         ObjectName notifName = new ObjectName("JMImplementation", "interceptor", "notificationwrapper");
//         ObjectName secName = new ObjectName("JMImplementation", "interceptor", "security");
         ObjectName invName = new ObjectName("JMImplementation", "interceptor", "invoker");

//         privilegedRegisterMBean(ccl, cclName);
//         privilegedRegisterMBean(notif, notifName);
//         privilegedRegisterMBean(sec, secName);
         privilegedRegisterMBean(inv, invName);
      }
      catch (Exception x)
      {
         logger.error("MBeanServerInterceptorConfigurator cannot be registered", x);
         throw new ImplementationException();
      }

      // Now register the delegate
      try
      {
         privilegedRegisterMBean(delegate, delegateName);
      }
      catch (Exception x)
      {
         logger.error("MBeanServerDelegate cannot be registered", x);
         throw new ImplementationException(x.toString());
      }

      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("MBeanServer instance created successfully");
   }

   /**
    * Returns the ClassLoaderRepository for this MBeanServer.
    * When first the ClassLoaderRepository is created in the constructor, the system property
    * {@link mx4j.MX4JSystemKeys#MX4J_MBEANSERVER_CLASSLOADER_REPOSITORY} is tested;
    * if it is non-null and defines a subclass of
    * {@link ModifiableClassLoaderRepository}, then that class is used instead of the default one.
    */
   public ClassLoaderRepository getClassLoaderRepository()
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         sm.checkPermission(new MBeanPermission("-#-[-]", "getClassLoaderRepository"));
      }

      return getModifiableClassLoaderRepository();
   }

   private ModifiableClassLoaderRepository getModifiableClassLoaderRepository()
   {
      return classLoaderRepository;
   }

   public ClassLoader getClassLoader(ObjectName name) throws InstanceNotFoundException
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         name = secureObjectName(name);

         if (name == null)
         {
            sm.checkPermission(new MBeanPermission("-#-[-]", "getClassLoader"));
         }
         else
         {
            MBeanMetaData metadata = findMBeanMetaData(name);
            sm.checkPermission(new MBeanPermission(metadata.info.getClassName(), "-", name, "getClassLoader"));
         }
      }

      return getClassLoaderImpl(name);
   }

   public ClassLoader getClassLoaderFor(ObjectName name) throws InstanceNotFoundException
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         name = secureObjectName(name);
      }

      // If name is null, I get InstanceNotFoundException
      MBeanMetaData metadata = findMBeanMetaData(name);

      if (sm != null)
      {
         sm.checkPermission(new MBeanPermission(metadata.info.getClassName(), "-", name, "getClassLoaderFor"));
      }

      return metadata.mbean.getClass().getClassLoader();
   }

   /**
    * Returns the MBean classloader corrispondent to the given ObjectName.
    * If <code>name</code> is null, the classloader of this class is returned.
    */
   private ClassLoader getClassLoaderImpl(ObjectName name) throws InstanceNotFoundException
   {
      if (name == null)
      {
         return getClass().getClassLoader();
      }
      else
      {
         MBeanMetaData metadata = findMBeanMetaData(name);
         if (metadata.mbean instanceof ClassLoader)
         {
            return (ClassLoader)metadata.mbean;
         }
         else
         {
            throw new InstanceNotFoundException(name.getCanonicalName());
         }
      }
   }

   public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] bytes)
           throws InstanceNotFoundException, OperationsException, ReflectionException
   {
      if (className == null || className.trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid class name '" + className + "'"));
      }

      ClassLoader cl = getClassLoader(loaderName);

      try
      {
         Class cls = cl.loadClass(className);
         return deserializeImpl(cls.getClassLoader(), bytes);
      }
      catch (ClassNotFoundException x)
      {
         throw new ReflectionException(x);
      }
   }

   public ObjectInputStream deserialize(String className, byte[] bytes)
           throws OperationsException, ReflectionException
   {
      if (className == null || className.trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid class name '" + className + "'"));
      }

      // Find the classloader that can load the given className using the ClassLoaderRepository
      try
      {
         Class cls = getClassLoaderRepository().loadClass(className);
         return deserializeImpl(cls.getClassLoader(), bytes);
      }
      catch (ClassNotFoundException x)
      {
         throw new ReflectionException(x);
      }
   }

   public ObjectInputStream deserialize(ObjectName objectName, byte[] bytes)
           throws InstanceNotFoundException, OperationsException
   {
      ClassLoader cl = getClassLoaderFor(objectName);
      return deserializeImpl(cl, bytes);
   }

   /**
    * Deserializes the given bytes using the specified classloader.
    */
   private ObjectInputStream deserializeImpl(ClassLoader classloader, byte[] bytes) throws OperationsException
   {
      if (bytes == null || bytes.length == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid byte array " + bytes));
      }

      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      try
      {
         return new ClassLoaderObjectInputStream(bais, classloader);
      }
      catch (IOException x)
      {
         throw new OperationsException(x.toString());
      }
   }

   private MBeanServerInterceptor getHeadInterceptor()
   {
      MBeanServerInterceptor head = invoker.getHeadInterceptor();

      if (head == null) throw new IllegalStateException("No MBeanServer interceptor, probably the configurator has been stopped");

      return head;
   }

   private Logger getLogger()
   {
      return Log.getLogger(getClass().getName());
   }

   /**
    * Creates a new repository for MBeans.
    * The system property {@link mx4j.MX4JSystemKeys#MX4J_MBEANSERVER_REPOSITORY} is tested
    * for a full qualified name of a class implementing the {@link MBeanRepository} interface.
    * In case the system property is not defined or the class is not loadable or instantiable, a default
    * implementation is returned.
    */
   private MBeanRepository createMBeanRepository()
   {
      Logger logger = getLogger();

      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Checking for system property " + MX4JSystemKeys.MX4J_MBEANSERVER_REPOSITORY);

      String value = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty(MX4JSystemKeys.MX4J_MBEANSERVER_REPOSITORY);
         }
      });

      if (value != null)
      {
         if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("Property found for custom MBeanServer registry; class is: " + value);

         try
         {
            MBeanRepository registry = (MBeanRepository)Thread.currentThread().getContextClassLoader().loadClass(value).newInstance();
            if (logger.isEnabledFor(Logger.TRACE))
            {
               logger.trace("Custom MBeanServer registry created successfully");
            }
            return registry;
         }
         catch (Exception x)
         {
            if (logger.isEnabledFor(Logger.TRACE))
            {
               logger.trace("Custom MBeanServer registry could not be created", x);
            }
         }
      }

      return new DefaultMBeanRepository();
   }

   /**
    * Creates a new ClassLoaderRepository for ClassLoader MBeans.
    * The system property {@link mx4j.MX4JSystemKeys#MX4J_MBEANSERVER_CLASSLOADER_REPOSITORY}
    * is tested for a full qualified name of a class
    * extending the {@link ModifiableClassLoaderRepository} class.
    * In case the system property is not defined or the class is not loadable or instantiable, a default
    * implementation is returned.
    */
   private ModifiableClassLoaderRepository createClassLoaderRepository()
   {
      Logger logger = getLogger();

      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Checking for system property " + MX4JSystemKeys.MX4J_MBEANSERVER_CLASSLOADER_REPOSITORY);

      String value = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty(MX4JSystemKeys.MX4J_MBEANSERVER_CLASSLOADER_REPOSITORY);
         }
      });

      if (value != null)
      {
         if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("Property found for custom ClassLoaderRepository; class is: " + value);

         try
         {
            ModifiableClassLoaderRepository repository = (ModifiableClassLoaderRepository)Thread.currentThread().getContextClassLoader().loadClass(value).newInstance();
            if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Custom ClassLoaderRepository created successfully " + repository);
            return repository;
         }
         catch (Exception x)
         {
            if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Custom ClassLoaderRepository could not be created", x);
         }
      }
      return new DefaultClassLoaderRepository();
   }

   /**
    * Returns the repository of MBeans for this MBeanServer
    */
   private MBeanRepository getMBeanRepository()
   {
      return mbeanRepository;
   }

   /**
    * Looks up the metadata associated with the given ObjectName.
    * @throws InstanceNotFoundException if the given ObjectName is not a registered MBean
    */
   private MBeanMetaData findMBeanMetaData(ObjectName objectName) throws InstanceNotFoundException
   {
      MBeanMetaData metadata = null;
      if (objectName != null)
      {
         objectName = normalizeObjectName(objectName);

         MBeanRepository repository = getMBeanRepository();
         synchronized (repository)
         {
            metadata = repository.get(objectName);
         }
      }
      if (metadata == null)
      {
         throw new InstanceNotFoundException("MBeanServer cannot find MBean with ObjectName " + objectName);
      }
      return metadata;
   }

   public void addNotificationListener(ObjectName observed, ObjectName listener, NotificationFilter filter, Object handback)
           throws InstanceNotFoundException
   {
      listener = secureObjectName(listener);

      Object mbean = findMBeanMetaData(listener).mbean;
      if (!(mbean instanceof NotificationListener))
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("MBean " + listener + " is not a NotificationListener"));
      }
      addNotificationListener(observed, (NotificationListener)mbean, filter, handback);
   }

   public void addNotificationListener(ObjectName observed, NotificationListener listener, NotificationFilter filter, Object handback)
           throws InstanceNotFoundException
   {
      if (listener == null)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("NotificationListener cannot be null"));
      }

      observed = secureObjectName(observed);

      MBeanMetaData metadata = findMBeanMetaData(observed);

      Object mbean = metadata.mbean;

      if (!(mbean instanceof NotificationBroadcaster))
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("MBean " + observed + " is not a NotificationBroadcaster"));
      }

      addNotificationListenerImpl(metadata, listener, filter, handback);
   }

   private void addNotificationListenerImpl(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback)
   {
      getHeadInterceptor().addNotificationListener(metadata, listener, filter, handback);
   }

   public void removeNotificationListener(ObjectName observed, ObjectName listener)
           throws InstanceNotFoundException, ListenerNotFoundException
   {
      listener = secureObjectName(listener);

      Object mbean = findMBeanMetaData(listener).mbean;
      if (!(mbean instanceof NotificationListener))
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("MBean " + listener + " is not a NotificationListener"));
      }
      removeNotificationListener(observed, (NotificationListener)mbean);
   }

   public void removeNotificationListener(ObjectName observed, NotificationListener listener)
           throws InstanceNotFoundException, ListenerNotFoundException
   {
      if (listener == null)
      {
         throw new ListenerNotFoundException("NotificationListener cannot be null");
      }

      observed = secureObjectName(observed);

      MBeanMetaData metadata = findMBeanMetaData(observed);
      Object mbean = metadata.mbean;

      if (!(mbean instanceof NotificationBroadcaster))
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("MBean " + observed + " is not a NotificationBroadcaster"));
      }

      removeNotificationListenerImpl(metadata, listener);
   }

   public void removeNotificationListener(ObjectName observed, ObjectName listener, NotificationFilter filter, Object handback)
           throws InstanceNotFoundException, ListenerNotFoundException
   {
      listener = secureObjectName(listener);

      Object mbean = findMBeanMetaData(listener).mbean;
      if (!(mbean instanceof NotificationListener))
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("MBean " + listener + " is not a NotificationListener"));
      }
      removeNotificationListener(observed, (NotificationListener)mbean, filter, handback);
   }

   public void removeNotificationListener(ObjectName observed, NotificationListener listener, NotificationFilter filter, Object handback)
           throws InstanceNotFoundException, ListenerNotFoundException
   {
      if (listener == null)
      {
         throw new ListenerNotFoundException("NotificationListener cannot be null");
      }

      observed = secureObjectName(observed);

      MBeanMetaData metadata = findMBeanMetaData(observed);
      Object mbean = metadata.mbean;

      if (!(mbean instanceof NotificationEmitter))
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("MBean " + observed + " is not a NotificationEmitter"));
      }

      removeNotificationListenerImpl(metadata, listener, filter, handback);
   }

   private void removeNotificationListenerImpl(MBeanMetaData metadata, NotificationListener listener)
           throws ListenerNotFoundException
   {
      getHeadInterceptor().removeNotificationListener(metadata, listener);
   }

   private void removeNotificationListenerImpl(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback)
           throws ListenerNotFoundException
   {
      getHeadInterceptor().removeNotificationListener(metadata, listener, filter, handback);
   }

   public Object instantiate(String className)
           throws ReflectionException, MBeanException
   {
      return instantiate(className, null, null);
   }

   public Object instantiate(String className, Object[] args, String[] parameters)
           throws ReflectionException, MBeanException
   {
      if (className == null || className.trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Class name cannot be null or empty"));
      }

      try
      {
         Class cls = getModifiableClassLoaderRepository().loadClass(className);
         return instantiateImpl(className, cls.getClassLoader(), null, parameters, args).mbean;
      }
      catch (ClassNotFoundException x)
      {
         throw new ReflectionException(x);
      }
   }

   public Object instantiate(String className, ObjectName loaderName)
           throws ReflectionException, MBeanException, InstanceNotFoundException
   {
      return instantiate(className, loaderName, null, null);
   }

   public Object instantiate(String className, ObjectName loaderName, Object[] args, String[] parameters)
           throws ReflectionException, MBeanException, InstanceNotFoundException
   {
      if (className == null || className.trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Class name cannot be null or empty"));
      }

      // loaderName can be null: means using this class' ClassLoader

      loaderName = secureObjectName(loaderName);
      if (loaderName != null && loaderName.isPattern())
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("ObjectName for the ClassLoader cannot be a pattern ObjectName: " + loaderName));
      }

      ClassLoader cl = getClassLoaderImpl(loaderName);
      return instantiateImpl(className, cl, null, parameters, args).mbean;
   }

   private MBeanMetaData instantiateImpl(String className, ClassLoader classloader, ObjectName name, String[] params, Object[] args)
           throws ReflectionException, MBeanException
   {
      if (params == null) params = EMPTY_PARAMS;
      if (args == null) args = EMPTY_ARGS;

      MBeanMetaData metadata = createMBeanMetaData();
      metadata.classloader = classloader;
      metadata.name = secureObjectName(name);

      getHeadInterceptor().instantiate(metadata, className, params, args);

      return metadata;
   }

   public ObjectInstance createMBean(String className, ObjectName objectName)
           throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException
   {
      return createMBean(className, objectName, null, null);
   }

   public ObjectInstance createMBean(String className, ObjectName objectName, Object[] args, String[] parameters)
           throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException
   {
      try
      {
         Class cls = getModifiableClassLoaderRepository().loadClass(className);
         MBeanMetaData metadata = instantiateImpl(className, cls.getClassLoader(), objectName, parameters, args);

         registerImpl(metadata, false);

         return metadata.instance;
      }
      catch (ClassNotFoundException x)
      {
         throw new ReflectionException(x);
      }
   }

   public ObjectInstance createMBean(String className, ObjectName objectName, ObjectName loaderName)
           throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException
   {
      return createMBean(className, objectName, loaderName, null, null);
   }

   public ObjectInstance createMBean(String className, ObjectName objectName, ObjectName loaderName, Object[] args, String[] parameters)
           throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException
   {
      loaderName = secureObjectName(loaderName);

      ClassLoader cl = getClassLoaderImpl(loaderName);

      MBeanMetaData metadata = instantiateImpl(className, cl, objectName, parameters, args);

      registerImpl(metadata, false);

      return metadata.instance;
   }

   public ObjectInstance registerMBean(Object mbean, ObjectName objectName)
           throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException
   {
      return registerMBeanImpl(mbean, objectName, false);
   }

   private ObjectInstance registerMBeanImpl(Object mbean, ObjectName objectName, boolean privileged)
           throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException
   {
      if (mbean == null)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("MBean instance cannot be null"));
      }

      MBeanMetaData metadata = createMBeanMetaData();
      metadata.mbean = mbean;
      metadata.classloader = mbean.getClass().getClassLoader();
      metadata.name = secureObjectName(objectName);

      registerImpl(metadata, privileged);

      return metadata.instance;
   }

   /**
    * Returns a new instance of the metadata class used to store MBean information.
    */
   private MBeanMetaData createMBeanMetaData()
   {
      return new MBeanMetaData();
   }

   /**
    * This method is called only to register implementation MBeans from the constructor.
    * Since to create an instance of this class already requires a permission, here we hide the registration
    * of implementation MBeans to the client that thus need no further permissions.
    */
   private ObjectInstance privilegedRegisterMBean(final Object mbean, final ObjectName name)
           throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException
   {
      try
      {
         return (ObjectInstance)AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return registerMBeanImpl(mbean, name, true);
            }
         });
      }
      catch (PrivilegedActionException x)
      {
         Exception xx = x.getException();
         if (xx instanceof InstanceAlreadyExistsException)
            throw (InstanceAlreadyExistsException)xx;
         else if (xx instanceof MBeanRegistrationException)
            throw (MBeanRegistrationException)xx;
         else if (xx instanceof NotCompliantMBeanException)
            throw (NotCompliantMBeanException)xx;
         else
            throw new MBeanRegistrationException(xx);
      }
   }

   private void registerImpl(MBeanMetaData metadata, boolean privileged) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException
   {
      introspector.introspect(metadata);

      if (!introspector.isMBeanCompliant(metadata)) throw new NotCompliantMBeanException("MBean is not compliant");

      MBeanServerInterceptor head = getHeadInterceptor();

      try
      {
         // With this call, the MBean implementor can replace the ObjectName with a subclass that is not secure, secure it again
         head.registration(metadata, MBeanServerInterceptor.PRE_REGISTER);
         metadata.name = secureObjectName(metadata.name);

         metadata.instance = new ObjectInstance(metadata.name, metadata.info.getClassName());

         register(metadata, privileged);

         head.registration(metadata, MBeanServerInterceptor.POST_REGISTER_TRUE);
      }
      catch (Throwable x)
      {
         try
         {
            head.registration(metadata, MBeanServerInterceptor.POST_REGISTER_FALSE);
         }
         catch (MBeanRegistrationException ignored)
         {/* Ignore this one to rethrow the other one */
         }

         if (x instanceof SecurityException)
         {
            throw (SecurityException)x;
         }
         else if (x instanceof InstanceAlreadyExistsException)
         {
            throw (InstanceAlreadyExistsException)x;
         }
         else if (x instanceof MBeanRegistrationException)
         {
            throw (MBeanRegistrationException)x;
         }
         else if (x instanceof RuntimeOperationsException) 
     	 {
     		throw (RuntimeOperationsException)x;
     	 }
		 else if (x instanceof JMRuntimeException)
	     {
		    throw (JMRuntimeException)x;
	     }
         else if (x instanceof Exception)
         {
            throw new MBeanRegistrationException((Exception)x);
         }
         else if (x instanceof Error)
         {
            throw new MBeanRegistrationException(new RuntimeErrorException((Error)x));
         }
         else
         {
            throw new ImplementationException();
         }
      }

      if (metadata.mbean instanceof ClassLoader && !(metadata.mbean instanceof PrivateClassLoader))
      {
         ClassLoader cl = (ClassLoader)metadata.mbean;
         getModifiableClassLoaderRepository().addClassLoader(cl);
      }
   }

   private void register(MBeanMetaData metadata, boolean privileged) throws InstanceAlreadyExistsException
   {
      metadata.name = normalizeObjectName(metadata.name);

      ObjectName objectName = metadata.name;
      if (objectName == null || objectName.isPattern())
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("ObjectName cannot be null or a pattern ObjectName"));
      }
      if (objectName.getDomain().equals("JMImplementation") && !privileged)
      {
		throw new JMRuntimeException("Domain 'JMImplementation' is reserved for the JMX Agent");
      }

      MBeanRepository repository = getMBeanRepository();
      synchronized (repository)
      {
         if (repository.get(objectName) != null) throw new InstanceAlreadyExistsException(objectName.toString());

         repository.put(objectName, metadata);
      }
      addDomain(objectName.getDomain());

      notify(objectName, MBeanServerNotification.REGISTRATION_NOTIFICATION);
   }

   private void notify(ObjectName objectName, String notificationType)
   {
      long sequenceNumber = 0;
      synchronized (MX4JMBeanServer.class)
      {
         sequenceNumber = notifications;
         ++notifications;
      }

      delegate.sendNotification(new MBeanServerNotification(notificationType, delegateName, sequenceNumber, objectName));
   }

   private void addDomain(String domain)
   {
      synchronized (domains)
      {
         Integer count = (Integer)domains.get(domain);
         if (count == null)
            domains.put(domain, new Integer(1));
         else
            domains.put(domain, new Integer(count.intValue() + 1));
      }
   }

   private void removeDomain(String domain)
   {
      synchronized (domains)
      {
         Integer count = (Integer)domains.get(domain);
         if (count == null) throw new ImplementationException();
         if (count.intValue() < 2)
            domains.remove(domain);
         else
            domains.put(domain, new Integer(count.intValue() - 1));
      }
   }

   public void unregisterMBean(ObjectName objectName)
           throws InstanceNotFoundException, MBeanRegistrationException
   {
      objectName = secureObjectName(objectName);

      if (objectName == null || objectName.isPattern())
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("ObjectName cannot be null or a pattern ObjectName"));
      }

      if (objectName.getDomain().equals("JMImplementation"))
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Domain 'JMImplementation' is reserved for the JMX Agent"));
      }

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      try
      {
         MBeanServerInterceptor head = getHeadInterceptor();
         head.registration(metadata, MBeanServerInterceptor.PRE_DEREGISTER);

         unregister(metadata);

         getHeadInterceptor().registration(metadata, MBeanServerInterceptor.POST_DEREGISTER);

         if (metadata.mbean instanceof ClassLoader && !(metadata.mbean instanceof PrivateClassLoader))
         {
            getModifiableClassLoaderRepository().removeClassLoader((ClassLoader)metadata.mbean);
         }
      }
      catch (MBeanRegistrationException x)
      {
         throw x;
      }
      catch (SecurityException x)
      {
         throw x;
      }
      catch (Exception x)
      {
         throw new MBeanRegistrationException(x);
      }
      catch (Error x)
      {
         throw new MBeanRegistrationException(new RuntimeErrorException(x));
      }
   }

   private void unregister(MBeanMetaData metadata)
   {
      ObjectName objectName = metadata.name;

      MBeanRepository repository = getMBeanRepository();
      synchronized (repository)
      {
         repository.remove(objectName);
      }
      removeDomain(objectName.getDomain());

      notify(objectName, MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
   }

   public Object getAttribute(ObjectName objectName, String attribute)
           throws InstanceNotFoundException, MBeanException, AttributeNotFoundException, ReflectionException
   {
      if (attribute == null || attribute.trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid attribute"));
      }

      objectName = secureObjectName(objectName);

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      return getHeadInterceptor().getAttribute(metadata, attribute);
   }


   public void setAttribute(ObjectName objectName, Attribute attribute)
           throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
   {
      if (attribute == null || attribute.getName().trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid attribute"));
      }

      objectName = secureObjectName(objectName);

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      getHeadInterceptor().setAttribute(metadata, attribute);
   }

   public AttributeList getAttributes(ObjectName objectName, String[] attributes)
           throws InstanceNotFoundException, ReflectionException
   {
      if (attributes == null || attributes.length == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid attribute list"));
      }

      objectName = secureObjectName(objectName);

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         // Must check if the user has the right to call this method, regardless of the attributes
         sm.checkPermission(new MBeanPermission(metadata.info.getClassName(), "-", objectName, "getAttribute"));
      }

      return getHeadInterceptor().getAttributes(metadata, attributes);
   }

   public AttributeList setAttributes(ObjectName objectName, AttributeList attributes)
           throws InstanceNotFoundException, ReflectionException
   {
      if (attributes == null)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid attribute list"));
      }

      objectName = secureObjectName(objectName);

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         // Must check if the user has the right to call this method, regardless of the attributes
         sm.checkPermission(new MBeanPermission(metadata.info.getClassName(), "-", objectName, "setAttribute"));
      }

      return getHeadInterceptor().setAttributes(metadata, attributes);
   }

   public Object invoke(ObjectName objectName, String methodName, Object[] args, String[] parameters)
           throws InstanceNotFoundException, MBeanException, ReflectionException
   {
      if (methodName == null || methodName.trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid operation name '" + methodName + "'"));
      }

      if (args == null) args = EMPTY_ARGS;
      if (parameters == null) parameters = EMPTY_PARAMS;

      objectName = secureObjectName(objectName);

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      return getHeadInterceptor().invoke(metadata, methodName, parameters, args);
   }

   public String getDefaultDomain()
   {
      return defaultDomain;
   }

   public String[] getDomains()
   {
      synchronized (domains)
      {
      	Set keys = domains.keySet();
         return (String[])keys.toArray(new String[keys.size()]);
      }
   }

   public Integer getMBeanCount()
   {
      MBeanRepository repository = getMBeanRepository();
      synchronized (repository)
      {
         return new Integer(repository.size());
      }
   }

   public boolean isRegistered(ObjectName objectName)
   {
      try
      {
         return findMBeanMetaData(objectName) != null;
      }
      catch (InstanceNotFoundException x)
      {
         return false;
      }
   }

   public MBeanInfo getMBeanInfo(ObjectName objectName)
           throws InstanceNotFoundException, IntrospectionException, ReflectionException
   {
      objectName = secureObjectName(objectName);

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      MBeanInfo info = getHeadInterceptor().getMBeanInfo(metadata);
      if (info == null) throw new JMRuntimeException("MBeanInfo returned for MBean " + objectName + " is null");
      return info;
   }

   public ObjectInstance getObjectInstance(ObjectName objectName)
           throws InstanceNotFoundException
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         objectName = secureObjectName(objectName);
      }

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      if (sm != null)
      {
         sm.checkPermission(new MBeanPermission(metadata.info.getClassName(), "-", objectName, "getObjectInstance"));
      }

      return metadata.instance;
   }

   public boolean isInstanceOf(ObjectName objectName, String className)
           throws InstanceNotFoundException
   {
      if (className == null || className.trim().length() == 0)
      {
         throw new RuntimeOperationsException(new IllegalArgumentException("Invalid class name"));
      }

      objectName = secureObjectName(objectName);

      MBeanMetaData metadata = findMBeanMetaData(objectName);

      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         sm.checkPermission(new MBeanPermission(metadata.info.getClassName(), "-", objectName, "isInstanceOf"));
      }

      try
      {
         ClassLoader loader = metadata.classloader;
         Class cls = null;
         if (loader != null)
            cls = loader.loadClass(className);
         else
            cls = Class.forName(className, false, null);
            
		if (metadata.mbean instanceof StandardMBean) 
		{
		   Object impl = ((StandardMBean) metadata.mbean).getImplementation();
		   return cls.isInstance(impl);
		}
		else
		{
		   return cls.isInstance(metadata.mbean);
		}
      }
      catch (ClassNotFoundException x)
      {
         return false;
      }
   }

   public Set queryMBeans(ObjectName patternName, QueryExp filter)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         patternName = secureObjectName(patternName);
         // Must check if the user has the right to call this method,
         // no matter which ObjectName has been passed.
         sm.checkPermission(new MBeanPermission("-#-[-]", "queryMBeans"));
      }

      Set match = queryObjectNames(patternName, filter, true);

      Set set = new HashSet();
      for (Iterator i = match.iterator(); i.hasNext();)
      {
         ObjectName name = (ObjectName)i.next();
         try
         {
            MBeanMetaData metadata = findMBeanMetaData(name);
            set.add(metadata.instance);
         }
         catch (InstanceNotFoundException ignored)
         {
            // A concurrent thread removed the MBean after queryNames, ignore
         }
      }
      return set;
   }

   public Set queryNames(ObjectName patternName, QueryExp filter)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         patternName = secureObjectName(patternName);
         // Must check if the user has the right to call this method,
         // no matter which ObjectName has been passed.
         sm.checkPermission(new MBeanPermission("-#-[-]", "queryNames"));
      }

      return queryObjectNames(patternName, filter, false);
   }

   /**
    * Utility method for queryNames and queryMBeans that returns a set of ObjectNames.
    * It does 3 things:
    * 1) filter the MBeans following the given ObjectName pattern
    * 2) filter the MBeans following the permissions that client code has
    * 3) filter the MBeans following the given QueryExp
    * It is important that these 3 operations are done in this order
    */
   private Set queryObjectNames(ObjectName patternName, QueryExp filter, boolean instances)
   {
      // First, retrieve the scope of the query: all mbeans matching the patternName
      Set scope = findMBeansByPattern(patternName);

      // Second, filter the scope by checking the caller's permissions
      Set secureScope = filterMBeansBySecurity(scope, instances);

      // Third, filter the scope using the given QueryExp
      Set match = filterMBeansByQuery(secureScope, filter);

      return match;
   }

   /**
    * Returns a set of ObjectNames of the registered MBeans that match the given ObjectName pattern
    */
   private Set findMBeansByPattern(ObjectName pattern)
   {
      if (pattern == null)
      {
         try
         {
            pattern = new ObjectName("*:*");
         }
         catch (MalformedObjectNameException ignored)
         {
         }
      }

      pattern = normalizeObjectName(pattern);

      String patternDomain = pattern.getDomain();
      Hashtable patternProps = pattern.getKeyPropertyList();

      Set set = new HashSet();

      // Clone the repository, we are faster than holding the lock while iterating
      MBeanRepository repository = (MBeanRepository)getMBeanRepository().clone();

      for (Iterator i = repository.iterator(); i.hasNext();)
      {
         MBeanMetaData metadata = (MBeanMetaData)i.next();
         ObjectName name = metadata.name;
         Hashtable props = name.getKeyPropertyList();

         String domain = name.getDomain();
         if (Utils.wildcardMatch(patternDomain, domain))
         {
            // Domain matches, now check properties
            if (pattern.isPropertyPattern())
            {
               // A property pattern with no entries, can only be '*'
               if (patternProps.size() == 0)
               {
                  // User wants all properties
                  set.add(name);
               }
               else
               {
                  // Loop on the properties of the pattern.
                  // If one is not found then the current ObjectName does not match
                  boolean found = true;
                  for (Iterator j = patternProps.entrySet().iterator(); j.hasNext();)
                  {
                     Map.Entry entry = (Map.Entry)j.next();
                     Object patternKey = entry.getKey();
                     Object patternValue = entry.getValue();
                     if (patternKey.equals("*"))
                     {
                        continue;
                     }

                     // Try to see if the current ObjectName contains this entry
                     if (!props.containsKey(patternKey))
                     {
                        // Not even the key is present
                        found = false;
                        break;
                     }
                     else
                     {
                        // The key is present, let's check if the values are equal
                        Object value = props.get(patternKey);
                        if (value == null && patternValue == null)
                        {
                           // Values are equal, go on with next pattern entry
                           continue;
                        }
                        if (value != null && value.equals(patternValue))
                        {
                           // Values are equal, go on with next pattern entry
                           continue;
                        }
                        // Here values are different
                        found = false;
                        break;
                     }
                  }
                  if (found) set.add(name);
               }
            }
            else
            {
               if (props.entrySet().equals(patternProps.entrySet())) set.add(name);
            }
         }
      }
      return set;
   }

   /**
    * Filters the given set of ObjectNames following the permission that client code has granted.
    * Returns a set containing the allowed ObjectNames.
    */
   private Set filterMBeansBySecurity(Set mbeans, boolean instances)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null) return mbeans;

      HashSet set = new HashSet();
      for (Iterator i = mbeans.iterator(); i.hasNext();)
      {
         ObjectName name = (ObjectName)i.next();
         try
         {
            MBeanMetaData metadata = findMBeanMetaData(name);
            String className = metadata.info.getClassName();
            sm.checkPermission(new MBeanPermission(className, "-", name, instances ? "queryMBeans" : "queryNames"));
            set.add(name);
         }
         catch (InstanceNotFoundException ignored)
         {
            // A concurrent thread removed this MBean, continue
            continue;
         }
         catch (SecurityException ignored)
         {
            // Don't add the name to the list, and go on.
         }
      }
      return set;
   }

   /**
    * Filters the given set of ObjectNames following the given QueryExp.
    * Returns a set of ObjectNames that match the given QueryExp.
    */
   private Set filterMBeansByQuery(Set scope, QueryExp filter)
   {
      if (filter == null) return scope;

      Set set = new HashSet();
      for (Iterator i = scope.iterator(); i.hasNext();)
      {
         ObjectName name = (ObjectName)i.next();
         filter.setMBeanServer(this);
         try
         {
            if (filter.apply(name)) set.add(name);
         }
         catch (BadStringOperationException ignored)
         {
         }
         catch (BadBinaryOpValueExpException ignored)
         {
         }
         catch (BadAttributeValueExpException x)
         {
         }
         catch (InvalidApplicationException x)
         {
         }
         catch (SecurityException x)
         {
         }
		 catch (Exception x)
		 {
		   // The 1.2 spec says Exceptions must not be propagated
		 }
      }
      return set;
   }

   /**
    * Returns a normalized ObjectName from the given one.
    * If an ObjectName is specified with the abbreviated notation for the default domain, that is ':key=value'
    * this method returns an ObjectName whose domain is the default domain of this MBeanServer and with the same
    * properties.
    */
   private ObjectName normalizeObjectName(ObjectName name)
   {
      if (name == null) return null;

      String defaultDomain = getDefaultDomain();
      String domain = name.getDomain();

      if (domain.length() == 0 && defaultDomain.length() > 0)
      {
         // The given object name specifies the abbreviated form to indicate the default domain,
         // ie ':key=value', the empty string as domain. I must convert this abbreviated form
         // to the full one, if the default domain of this mbeanserver is not the empty string as well
         StringBuffer buffer = new StringBuffer(defaultDomain).append(":").append(name.getKeyPropertyListString());
         if (name.isPropertyPattern())
         {
            if (name.getKeyPropertyList().size() > 0)
               buffer.append(",*");
            else
               buffer.append("*");
         }
         try
         {
            name = new ObjectName(buffer.toString());
         }
         catch (MalformedObjectNameException ignored)
         {
         }
      }
      return name;
   }

   /**
    * Returns an ObjectName instance even if the provided ObjectName is a subclass.
    * This is done to avoid security holes: a nasty ObjectName subclass can bypass security checks.
    */
   private ObjectName secureObjectName(ObjectName name)
   {
      // I cannot trust ObjectName, since a malicious user can send a subclass that overrides equals and hashcode
      // to match another ObjectName for which it does not have permission, or returns different results from
      // ObjectName.getCanonicalName() for different calls, so that passes the security checks but in fact will
      // later refer to a different ObjectName for which it does not have permission.
      if (name == null) return null;
      return ObjectName.getInstance(name);
   }
}
