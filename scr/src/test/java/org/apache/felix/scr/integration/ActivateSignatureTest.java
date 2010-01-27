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
package org.apache.felix.scr.integration;


import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.activatesignature.AbstractActivateSignatureTestComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;


/**
 * The <code>ActivateSignatureTest</code> tests various DS 1.1 activation
 * signatures for the default method name
 */
@RunWith(JUnit4TestRunner.class)
public class ActivateSignatureTest extends ComponentTestBase
{

    static
    {
        // use different components
        descriptorFile = "/integration_test_signature_components.xml";

        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test()
    {
        // wait for components to fire up in the background....
        delay();

        final Component[] components = getComponents();
        TestCase.assertNotNull( components );

        for ( Component component : components )
        {
            TestCase.assertTrue( "Expecting component " + component.getName() + " to be enabled", component
                .isDefaultEnabled() );

            TestCase.assertEquals( "Expecting component " + component.getName() + " to be active",
                Component.STATE_ACTIVE, component.getState() );

            final Object instance = component.getComponentInstance().getInstance();
            TestCase.assertTrue( "Expecting component " + component.getName()
                + " to be an AbstractActivateSignatureTestComponent",
                instance instanceof AbstractActivateSignatureTestComponent );

            final AbstractActivateSignatureTestComponent aastc = ( AbstractActivateSignatureTestComponent ) instance;
            TestCase.assertEquals( "Expect activate method to be called", component.getName(), aastc.getMethodCalled() );
        }
    }

}
