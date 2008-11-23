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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.component.FooProviderType1;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.FooService;
import org.osgi.framework.ServiceReference;

/**
 * Test exception handling. POJO exception must be propagated.
 */
public class ExceptionTest extends OSGiTestCase {
	
	private ComponentInstance ci_lazzy;
	
	private ServiceReference lazzyRef;
	private ServiceReference immRef;	
	
	IPOJOHelper helper;

	public void setUp() {
	    helper = new IPOJOHelper(this);
	    
		String factName = "Manipulation-FooProviderType-1";
		String compName = "FooProvider-1";
		ci_lazzy = helper.createComponentInstance(factName, compName);
		
		String factName2 = "Manipulation-ImmediateFooProviderType";
		String compName2 = "FooProvider-2";
		helper.createComponentInstance(factName2, compName2);
		
		lazzyRef = getServiceReference(Architecture.class.getName(), "(architecture.instance="+compName+")");
		immRef =   getServiceReference(Architecture.class.getName(), "(architecture.instance="+compName2+")");
		
		assertNotNull("LazzyRef", lazzyRef);
		assertNotNull("ImmRef", immRef);
	}
	
	public void tearDown() {
	    helper.dispose();
	}
	
    
    /**
     * Check that the exception is correctly propagated.
     */
    public void testException() {
        ServiceReference ref = helper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available",ref);
        FooProviderType1 fs = (FooProviderType1) getServiceObject(ref);
        try {
            fs.testException();
            fail("The method must returns an exception");
        } catch(Exception e) {
            // OK
        }
    }
    
    /**
     * Check that the exception is correctly catch by the POJO.
     */
    public void testTry() {
        ServiceReference ref = helper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available",ref);
        FooProviderType1 fs = (FooProviderType1) context.getService(ref);
        try {
            fs.testTry();
        } catch(Exception e) {
            fail("The method has returned an exception");
        }
    }

}
