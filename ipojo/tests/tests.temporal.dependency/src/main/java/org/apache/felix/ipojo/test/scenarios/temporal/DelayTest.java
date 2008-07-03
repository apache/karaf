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
package org.apache.felix.ipojo.test.scenarios.temporal;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.temporal.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.temporal.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class DelayTest extends OSGiTestCase {
    
   public void testDelay() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-CheckServiceProvider", un);
       
       ServiceReference ref_fs = Utils.getServiceReferenceByName(context, FooService.class.getName(), prov);
       assertNotNull("Check foo availability", ref_fs);
       
       ServiceReference ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability", ref_cs);
       
       CheckService cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation", cs.check());
       
       // Stop the provider.
       provider.stop();
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 2", ref_cs);
       long begin = System.currentTimeMillis();
       DelayedProvider dp = new DelayedProvider(provider, 2000);
       dp.start();
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 2", cs.check());
       long end = System.currentTimeMillis();
       
       assertTrue("Assert delay", (end - begin) >= 2000);
       
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 3", ref_cs);
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 3", cs.check());
       
       provider.stop();
       provider.dispose();
       under.stop();
       under.dispose();
   }
   
   public void testTimeout() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-CheckServiceProvider", un);
       
       ServiceReference ref_fs = Utils.getServiceReferenceByName(context, FooService.class.getName(), prov);
       assertNotNull("Check foo availability", ref_fs);
       
       ServiceReference ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability", ref_cs);
       
       CheckService cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation", cs.check());
       
       // Stop the provider.
       provider.stop();
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 2", ref_cs);
       DelayedProvider dp = new DelayedProvider(provider, 5000);
       dp.start();
       cs = (CheckService) context.getService(ref_cs);
       try {
           cs.check();
       } catch(RuntimeException e) {
           // OK
           dp.stop();
           provider.stop();
           provider.dispose();
           under.stop();
           under.dispose();
           return;
       }   
       
       fail("Timeout expected");
   }
   
   public void testDelayTimeout() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-CheckServiceProviderTimeout", un);
       
       ServiceReference ref_fs = Utils.getServiceReferenceByName(context, FooService.class.getName(), prov);
       assertNotNull("Check foo availability", ref_fs);
       
       ServiceReference ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability", ref_cs);
       
       CheckService cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation", cs.check());
       
       // Stop the provider.
       provider.stop();
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 2", ref_cs);
       long begin = System.currentTimeMillis();
       DelayedProvider dp = new DelayedProvider(provider, 2000);
       dp.start();
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 2", cs.check());
       long end = System.currentTimeMillis();
       
       assertTrue("Assert delay", (end - begin) >= 2000);
       
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 3", ref_cs);
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 3", cs.check());
       
       provider.stop();
       provider.dispose();
       under.stop();
       under.dispose();
   }
   
   public void testSetTimeout() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-CheckServiceProviderTimeout", un);
       
       ServiceReference ref_fs = Utils.getServiceReferenceByName(context, FooService.class.getName(), prov);
       assertNotNull("Check foo availability", ref_fs);
       
       ServiceReference ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability", ref_cs);
       
       CheckService cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation", cs.check());
       
       // Stop the provider.
       provider.stop();
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 2", ref_cs);
       DelayedProvider dp = new DelayedProvider(provider, 5000);
       dp.start();
       cs = (CheckService) context.getService(ref_cs);
       try {
           cs.check();
       } catch(RuntimeException e) {
           // OK
           dp.stop();
           provider.stop();
           provider.dispose();
           under.stop();
           under.dispose();
           return;
       }   
       
       fail("Timeout expected");
   }
   
   public void testDelayOnMultipleDependency() {
       String prov = "provider";
       ComponentInstance provider1 = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String prov2 = "provider2";
       ComponentInstance provider2 = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov2);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-CheckServiceProvider", un);
       
       ServiceReference ref_fs = Utils.getServiceReferenceByName(context, FooService.class.getName(), prov);
       assertNotNull("Check foo availability", ref_fs);
       
       ServiceReference ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability", ref_cs);
       
       CheckService cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation", cs.check());
       
       // Stop the providers.
       provider1.stop();
       provider2.stop();
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 2", ref_cs);
       long begin = System.currentTimeMillis();
       DelayedProvider dp = new DelayedProvider(provider1, 2500);
       DelayedProvider dp2 = new DelayedProvider(provider2, 1000);
       dp.start();
       dp2.start();
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 2", cs.check());
       long end = System.currentTimeMillis();
       assertTrue("Assert delay", (end - begin) >= 1000  && (end - begin) <= 2500);
       dp.stop();
       dp2.stop();
       
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 3", ref_cs);
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 3", cs.check());
       
       provider1.stop();
       provider2.stop();
       provider1.dispose();
       provider2.dispose();
       under.stop();
       under.dispose();
   }
}
