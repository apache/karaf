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
package org.apache.felix.mosgi.jmx.agent.mx4j;

/**
 * This class holds the system property keys that the MX4J implementation uses to plugin
 * custom components. <br>
 * The naming convention is that, for a defined constant, the corrispondent system property
 * is obtained by converting the constant name to lowercase and by replacing the underscores
 * with dots so that, for example, the constant <code>MX4J_MBEANSERVER_CLASSLOADER_REPOSITORY</code>
 * correspond to the system property key <code>mx4j.mbeanserver.classloader.repository</code>
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public final class MX4JSystemKeys
{
   /**
    * Specifies a full qualified class name of a class implementing the {@link mx4j.server.MBeanRepository}
    * interface, that will be used by the MBeanServer to store information about registered MBeans.
    */
   public static final String MX4J_MBEANSERVER_REPOSITORY = "mx4j.mbeanserver.repository";

   /**
    * Specifies a full qualified class name of a class extending the {@link mx4j.server.ModifiableClassLoaderRepository}
    * class, that will be used by the MBeanServer to store ClassLoader MBeans that wants to be registered in
    * the MBeanServer's ClassLoaderRepository.
    */
   public static final String MX4J_MBEANSERVER_CLASSLOADER_REPOSITORY = "mx4j.mbeanserver.classloader.repository";

   /**
    * Specifies the level of logging performed by the MX4J JMX implementation.
    * Possible value are (case insensitive), from most verbose to least verbose:
    * <ul>
    * <li>trace</li>
    * <li>debug</li>
    * <li>info</li>
    * <li>warn</li>
    * <li>error</li>
    * <li>fatal</li>
    * </ul>
    */
   public static final String MX4J_LOG_PRIORITY = "mx4j.log.priority";

   /**
    * Specifies a full qualified class name of a class extending the {@link mx4j.log.Logger} class, that
    * will be used as prototype for new loggers created.
    */
   public static final String MX4J_LOG_PROTOTYPE = "mx4j.log.prototype";

   /**
    * When this property is set to false (as specified by {@link Boolean#valueOf(String)}), the MX4J
    * JMX implementation will accept as MBean interfaces of standard MBeans also interfaces defined in
    * different packages or as nested classes of the MBean class.
    * So for example, will be possible for a com.foo.Service to have a management interface called
    * com.bar.ServiceMBean.
    * If not defined, or if set to true, only MBean interfaces of the same package of the MBean class
    * are considered valid management interfaces.
    */
   public static final String MX4J_STRICT_MBEAN_INTERFACE = "mx4j.strict.mbean.interface";

   /**
    * Specifies a full qualified class name of a class implementing the {@link mx4j.server.MBeanInvoker} interface,
    * that will be used as invoker for standard MBeans.
    * Two classes are provided by the MX4J JMX implementation: {@link mx4j.server.BCELMBeanInvoker} and
    * {@link mx4j.server.ReflectedMBeanInvoker}.
    * The first one will use BCEL classes (if present) to speed up invocations on standard MBeans, while the second
    * uses reflection.
    * If, for any reason, the BCEL invocation fails, then the reflected invoker is used.
    */
   public static final String MX4J_MBEAN_INVOKER = "mx4j.mbean.invoker";

   /**
    * From JMX 1.2, names for attributes and operations, as well as their (return) types, must be valid
    * Java identifiers, as specified by {@link Character#isJavaIdentifierStart} and {@link Character#isJavaIdentifierPart}.
    * When set to true, (as specified by {@link Boolean#valueOf(String)}), this property turnes off this check.
    */
   public static String MX4J_UNCHECKED_IDENTIFIERS = "jmx.unchecked.identifiers";
}
