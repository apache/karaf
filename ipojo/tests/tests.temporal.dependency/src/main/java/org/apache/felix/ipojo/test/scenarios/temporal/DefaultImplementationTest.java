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

public class DefaultImplementationTest extends OSGiTestCase {
    
   public void testDefaultImplementation() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-DICheckServiceProvider", un);
       
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
       DelayedProvider dp = new DelayedProvider(provider, 200);
       dp.start();
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 2", cs.check());
       long end = System.currentTimeMillis();
      
       assertTrue("Assert delay", (end - begin) >= 200);
       
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 3", ref_cs);
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 3", cs.check());
       
       provider.stop();
       provider.dispose();
       under.stop();
       under.dispose();
   }
   
   public void testDefaultImplementationTimeout() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-DICheckServiceProvider", un);
       
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
       cs = (CheckService) context.getService(ref_cs);
       boolean res = false;
       try {
           res = cs.check();
       } catch(RuntimeException e) {
           fail("A nullable was expected ...");
       }   
       assertFalse("Check nullable", res);

       provider.stop();
       provider.dispose();
       under.stop();
       under.dispose();
       return;
   }
   
   public void testDelayTimeout() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-DICheckServiceProviderTimeout", un);
       
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
       DelayedProvider dp = new DelayedProvider(provider, 200);
       dp.start();
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 2", cs.check());
       long end = System.currentTimeMillis();
       
       assertTrue("Assert delay", (end - begin) >= 200);
       
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 3", ref_cs);
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 3", cs.check());
       
       provider.stop();
       provider.dispose();
       under.stop();
       under.dispose();
   }
   
   public void testDefaultImplementationMultipleTimeout() {
       String prov = "provider";
       ComponentInstance provider = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-DIMultipleCheckServiceProviderTimeout", un);
       
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
       DelayedProvider dp = new DelayedProvider(provider, 400);
       dp.start();
       cs = (CheckService) context.getService(ref_cs);
       boolean res = false;
       try {
           res = cs.check();
       } catch(RuntimeException e) {
           fail("A nullable was expected ...");
       }   
       assertFalse("Check nullable", res);
       
       dp.stop();
       provider.stop();
       provider.dispose();
       under.stop();
       under.dispose();
       return;
   }
   
   public void testDelayOnMultipleDependency() {
       String prov = "provider";
       ComponentInstance provider1 = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov);
       String prov2 = "provider2";
       ComponentInstance provider2 = Utils.getComponentInstanceByName(context, "TEMPORAL-FooProvider", prov2);
       String un = "under-1";
       ComponentInstance under = Utils.getComponentInstanceByName(context, "TEMPORAL-NullableMultipleCheckServiceProviderTimeout", un);
       
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
       DelayedProvider dp = new DelayedProvider(provider1, 250);
       DelayedProvider dp2 = new DelayedProvider(provider2, 100);
       dp.start();
       dp2.start();
       cs = (CheckService) context.getService(ref_cs);
       assertTrue("Check invocation - 2", cs.check());
       long end = System.currentTimeMillis();
       assertTrue("Assert delay", (end - begin) >= 100  && (end - begin) <= 250);
       dp.stop();
       dp2.stop();
       
       provider1.stop();
       provider2.stop();
       
       ref_cs = Utils.getServiceReferenceByName(context, CheckService.class.getName(), un);
       assertNotNull("Check cs availability - 3", ref_cs);
       cs = (CheckService) context.getService(ref_cs);
       
       assertFalse("Check invocation - 3", cs.check()); // Will return false as the contained DI will return false to the foo method.

       provider1.dispose();
       provider2.dispose();
       under.stop();
       under.dispose();
   }
}
