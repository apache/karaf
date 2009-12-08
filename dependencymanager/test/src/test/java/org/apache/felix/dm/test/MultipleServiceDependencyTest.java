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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.util.Properties;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(JUnit4TestRunner.class)
public class MultipleServiceDependencyTest {
   @Configuration
   public static Option[] configuration() {
       return options(
           provision(
               mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
               mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
           )
       );
   }    

   @Test
   public void testMultipleServiceRegistrationAndConsumption(BundleContext context) {
       DependencyManager m = new DependencyManager(context);
       // helper class that ensures certain steps get executed in sequence
       Ensure e = new Ensure();
       // create a service provider and consumer
       Service provider = m.createService().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
       Service providerWithHighRank = m.createService().setImplementation(new ServiceProvider2(e)).setInterface(ServiceInterface.class.getName(), new Properties() {{ put(Constants.SERVICE_RANKING, Integer.valueOf(5)); }});
       Service consumer = m.createService().setImplementation(new ServiceConsumer(e)).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true));
       m.add(provider);
       m.add(providerWithHighRank);
       m.add(consumer);
       e.waitForStep(3, 1000);
       m.remove(providerWithHighRank);
       e.step(4);
       e.waitForStep(5, 1000);
       m.remove(provider);
       m.remove(consumer);
       e.waitForStep(6, 1000);
   }

   @Test
   public void testReplacementAutoConfig(BundleContext context) {
       DependencyManager m = new DependencyManager(context);
       // helper class that ensures certain steps get executed in sequence
       Ensure e = new Ensure();
       // create a service provider and consumer
       Service provider = m.createService().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
       Service provider2 = m.createService().setImplementation(new ServiceProvider2(e)).setInterface(ServiceInterface.class.getName(), null);
       Service consumer = m.createService().setImplementation(new ServiceConsumer(e)).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true));
       m.add(provider2);
       m.add(consumer);
       e.waitForStep(3, 1000);
       m.add(provider);
       m.remove(provider2);
       e.step(4);
       e.waitForStep(5, 1000);
       m.remove(provider);
       m.remove(consumer);
       e.waitForStep(6, 1000);
   }

   @Test
   public void testReplacementCallbacks(BundleContext context) {
       DependencyManager m = new DependencyManager(context);
       // helper class that ensures certain steps get executed in sequence
       Ensure e = new Ensure();
       // create a service provider and consumer
       Service provider = m.createService().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
       Service provider2 = m.createService().setImplementation(new ServiceProvider2(e)).setInterface(ServiceInterface.class.getName(), null);
       Service consumer = m.createService().setImplementation(new ServiceConsumer(e))
         .add(m.createServiceDependency()
              .setService(ServiceInterface.class)
              .setRequired(true)
              .setCallbacks("add", "remove"));
       m.add(provider2);
       m.add(consumer);
       e.waitForStep(3, 1000);
       m.add(provider);
       m.remove(provider2);
       e.step(4);
       e.waitForStep(5, 1000);
       m.remove(provider);
       m.remove(consumer);
       e.waitForStep(6, 1000);
   }

   static interface ServiceInterface {
       public void invoke();
   }

   static class ServiceProvider implements ServiceInterface {
       private final Ensure m_ensure;
       public ServiceProvider(Ensure e) {
           m_ensure = e;
       }
       public void invoke() {
           m_ensure.step(5);
       }
   }

   static class ServiceProvider2 implements ServiceInterface {
       private final Ensure m_ensure;
       public ServiceProvider2(Ensure e) {
           m_ensure = e;
       }
       public void invoke() {
           m_ensure.step(2);
       }
   }

   static class ServiceConsumer implements Runnable {
       private volatile ServiceInterface m_service;
       private final Ensure m_ensure;

       private void add(ServiceInterface service) { m_service = service; }
       private void remove(ServiceInterface service) { if (m_service == service) { m_service = null; }};
       public ServiceConsumer(Ensure e) { m_ensure = e; }

       public void start() {
           Thread t = new Thread(this);
           t.start();
       }

       public void run() {
           m_ensure.step(1);
           m_service.invoke();
           m_ensure.step(3);
           m_ensure.waitForStep(4, 1000);
           m_service.invoke();
       }

       public void stop() {
           m_ensure.step(6);
       }
   }
}
