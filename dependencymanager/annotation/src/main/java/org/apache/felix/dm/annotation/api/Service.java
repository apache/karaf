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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an OSGi Service class with its dependencies. 
 * By default, all directly implemented interfaces are registered into the OSGi registry,
 * and the Service is instantiated automatically, when the Service' bundle is started and 
 * when the Service dependencies are available. If you need to take control of when and how 
 * much Service instances are getting created, then you can use the <code>factory</code> 
 * Service attribute.<p> 
 * If a <code>factory</code> attribute is set, the Service is not started automatically 
 * during bundle startup, and a <code>java.util.Set&lt;Dictionary&gt;</code> 
 * object is registered into the OSGi registry on behalf of the Service. This Set will act 
 * as a Factory API, and another component may use this Set and add some configuration 
 * dictionaries in it, in order to fire some Service activations (there is one Service 
 * instantiated per dictionary, which is passed to Service instances via a configurable 
 * callback method).
 *
 * <h3>Usage Examples</h3>
 * 
 * <p> Here is a sample showing a X service, which depends on a configuration dependency:<p>
 * <blockquote>
 * 
 * <pre>
 * &#47;**
 *   * This Service will be activated once the bundle is started and when all required dependencies
 *   * are available.
 *   *&#47;
 * &#64;Service
 * class X implements Z {
 *     &#64;ConfigurationDependency(pid="MyPid")
 *     void configure(Dictionary conf) {
 *          // Configure or reconfigure our service.
 *     }
 *   
 *     &#64;Start
 *     void start() {
 *         // Our Service is starting and is about to be registered in the OSGi registry as a Z service.
 *   }
 *   
 *   public void doService() {
 *         // ...
 *   }   
 * </pre>
 * </blockquote>
 * 
 * Here is a sample showing how a Y service may dynamically instantiate several X Service instances, 
 * using the {@link #factory()} attribute:<p>
 * <blockquote>
 * 
 * <pre>
 *  &#47;**
 *    * All Service instances will be created/updated/removed by the "Y" Service
 *    *&#47;
 *  &#64;Service(factory="MyServiceFactory", factoryConfigure="configure")
 *  class X implements Z {                 
 *      void configure(Dictionary conf) {
 *          // Configure or reconfigure our service. The conf is provided by the factory,
 *          // and all public properties (which don't start with a dot) are propagated with the
 *          // Service properties specified in the properties's Service attribute.
 *      }
 * 
 *      &#64;ServiceDependency
 *      void bindOtherService(OtherService other) {
 *          // store this require dependency
 *      }
 *      
 *      &#64;Start
 *      void start() {
 *          // Our Service is starting and is about to be registered in the OSGi registry as a Z service.
 *      } 
 *      
 *      public void doService() {
 *          // ...
 *      }   
 *  }
 * 
 *  &#47;**
 *    * This class will instantiate some X Service instances
 *    *&#47;
 *  &#64;Service 
 *  class Y {
 *      &#64;ServiceDependency(filter="(dm.factory.name=MyServiceFactory))
 *      Set&lt;Dictionary&gt; _XFactory; // This Set acts as a Factory API for creating X Service instances.
 *    
 *      &#64;Start
 *      void start() {
 *          // Instantiate a X Service instance
 *          Dictionary x1 = new Hashtable() {{ put("foo", "bar1"); }};
 *          _XFactory.add(x1);
 *      
 *          // Instantiate another X Service instance
 *          Dictionary x2 = new Hashtable() {{ put("foo", "bar2"); }};
 *          _XFactory.add(x2);
 *      
 *          // Update the first X Service instance
 *          x1.put("foo", "bar1_modified");
 *          _XFactory.add(x1);
 *      
 *          // Destroy X Services (Notice that invoking XFactory.clear() will destroy all X Service instances)
 *          _XFactory.remove(x1);
 *          _XFactory.remove(x2); 
 *      }
 *  }
 * </pre>
 * 
 * </blockquote>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Service
{
    /**
     * Returns the list of provided interfaces. By default, the directly implemented interfaces are provided.
     */
    Class<?>[] provide() default {};

    /**
     * Returns the list of provided service properties.
     */
    Property[] properties() default {};

    /**
     * Returns the <code>factory</code> name used to dynamically instantiate the Service annotated by this class.
     * When you set this attribute, a <code>java.util.Set&lt;java.lang.Dictionary&gt;</code> Service will 
     * be provided with a <code>dm.factory.name</code> service property matching your specified <code>factory</code> attribute.
     * This Set will be provided once the Service's bundle is started, even if required dependencies are not available, and the
     * Set will be unregistered from the OSGi registry once the Service's bundle is stopped or being updated.<p>
     * So, basically, another component may then be injected with this set in order to dynamically instantiate some Service instances:
     * <ul>
     * <li> Each time a new Dictionary is added into the Set, then a new instance of the annotated service will be instantiated.</li>
     * <li> Each time an existing Dictionary is updated from the Set, then the corresponding Service instance will be updated.</li>
     * <li> Each time an existing Dictionary is removed from the Set, then the corresponding Service instance will be destroyed.</li>
     * </ul>
     * The dictionary registered in the Set will be provided to the created Service instance using a callback method that you can 
     * optionally specify in the {@link Service#factoryConfigure()} attribute. Each public properties from that dictionary 
     * (which don't start with a dot) will be propagated along with the annotated Service properties.
     */
    String factory() default "";

    /**
     * Returns the "configure" callback method name to be called with the factory configuration. This attribute only makes sense if the 
     * {@link #factory()} attribute is used. If specified, then this attribute references a Service callback method, which is called 
     * for providing the configuration supplied by the factory that instantiated this Service. The current Service properties will be 
     * also updated with all public properties (which don't start with a dot).
     */
    String factoryConfigure() default "";
}
