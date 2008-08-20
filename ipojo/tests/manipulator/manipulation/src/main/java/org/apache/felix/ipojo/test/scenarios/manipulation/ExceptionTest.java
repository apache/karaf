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
package org.apache.felix.ipojo.test.scenarios.manipulation;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.component.FooProviderType1;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Test execption handling. POJO exception must be propagated.
 */
public class ExceptionTest extends OSGiTestCase {
	
	private ComponentInstance ci_lazzy;
	private ComponentInstance ci_immediate;
	
	private ServiceReference lazzyRef;
	private ServiceReference immRef;	

	public void setUp() {
		String factName = "Manipulation-FooProviderType-1";
		String compName = "FooProvider-1";
		
		Properties p = new Properties();
		p.put("instance.name",compName);
		ci_lazzy = Utils.getComponentInstance(context, factName, p);
		
		String factName2 = "Manipulation-ImmediateFooProviderType";
		String compName2 = "FooProvider-2";
		
		Properties p2 = new Properties();
		p2.put("instance.name",compName2);
		ci_immediate = Utils.getComponentInstance(context, factName2, p2);
		
		lazzyRef = Utils.getServiceReference(context, Architecture.class.getName(), "(architecture.instance="+compName+")");
		immRef =   Utils.getServiceReference(context, Architecture.class.getName(), "(architecture.instance="+compName2+")");
		assertNotNull("LazzyRef", lazzyRef);
		assertNotNull("ImmRef", immRef);
	}
	
	public void tearDown() {
		context.ungetService(lazzyRef);
		context.ungetService(immRef);
		ci_lazzy.dispose();
		ci_immediate.dispose();
	}
	
    
    /**
     * Check that the exception is correctly propagated.
     */
    public void testException() {
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy.getInstanceName()+")");
        } catch (InvalidSyntaxException e) { e.printStackTrace(); }
        assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available",refs);
        FooProviderType1 fs = (FooProviderType1) context.getService(refs[0]);
        try {
            fs.testException();
            context.ungetService(refs[0]);
            fail("The method must returns an exception");
        } catch(Exception e) {
            context.ungetService(refs[0]);
        }
    }
    
    /**
     * Check that the exception is correctly catch by the POJO.
     */
    public void testTry() {
        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(FooService.class.getName(), "(instance.name="+ci_lazzy.getInstanceName()+")");
        } catch (InvalidSyntaxException e) { e.printStackTrace(); }
        assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available",refs);
        FooProviderType1 fs = (FooProviderType1) context.getService(refs[0]);
        try {
            fs.testTry();
            context.ungetService(refs[0]);
        } catch(Exception e) {
            context.ungetService(refs[0]);
            fail("The method has returned an exception");
        }
    }

}
