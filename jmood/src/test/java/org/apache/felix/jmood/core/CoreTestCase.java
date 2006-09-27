/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.jmood.core;
import java.util.logging.Logger;

import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

import junit.framework.Assert;

import org.apache.felix.jmood.core.CoreControllerMBean;
import org.apache.felix.jmood.core.ServiceNotAvailableException;
import org.osgi.framework.BundleException;

import org.apache.felix.jmood.utils.ObjectNames;

public class CoreTestCase extends TestHarness {
//    CoreControllerMBean core;
    private static Logger l=Logger.getLogger(CoreTestCase.class.getPackage().getName());

    public CoreTestCase() throws Exception {
        super();
    }

    protected void setUp() throws Exception {
        super.setUp();
//        core = (CoreControllerMBean) MBeanServerInvocationHandler
//                .newProxyInstance(getServer(), new ObjectName(
//                        ObjectNames.CORE_CONTROLLER),
//                        CoreControllerMBean.class, false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.startBundle(String)'
     */
    public void testStartBundle() {

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.batchStartBundles(String[])'
     */
    public void testBatchStartBundles() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.stopBundle(String)'
     */
    public void testStopBundle() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.batchStopBundles(String[])'
     */
    public void testBatchStopBundles() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.updateBundle(String)'
     */
    public void testUpdateBundle() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.batchUpdateBundles(String[])'
     */
    public void testBatchUpdateBundles() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.updateBundleFromUrl(String,
     * String)'
     */
    public void testUpdateBundleFromUrl() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.batchUpdateBundleFromUrl(String[],
     * String[])'
     */
    public void testBatchUpdateBundleFromUrl() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.installBundle(String)'
     */
    public void testInstallBundle() throws Exception {
//        String[] badUrls= {null, "MiCarro", "http://www.dit.upm.es"};
//        for (int i = 0; i < badUrls.length; i++) {
//            try {
//                core.installBundle(badUrls[i]);
//                assertTrue("Should've thrown bundle exception", false);
//            } catch (BundleException e) {
//                // OK
//            }
//        }
//        String[] goodUrls= {"http://maquina:9000/testing/bundle1.jar", "http://maquina:9000/testing/bundle2.jar"};
//        for (int i = 0; i < goodUrls.length; i++) {
//                core.installBundle(goodUrls[i]);
//        }
    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.batchInstallBundle(String[])'
     */
    public void testBatchInstallBundle() throws Exception{
//        String[] badUrls= {null, "MiCarro", "http://www.dit.upm.es"};
//        try {
//            core.batchInstallBundle(badUrls);
//            assertTrue("Should've thrown bundle exception", false);
//        } catch (BundleException e) {
//            // OK
//        }
//        
//        String[] goodUrls= {"http://maquina:9000/testing/bundle1.jar", "http://maquina:9000/testing/bundle2.jar"};
//        core.batchInstallBundle(goodUrls);

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.setBundleStartLevel(String,
     * int)'
     */
    public void testSetBundleStartLevel() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.batchSetBundleStartLevel(String[],
     * int[])'
     */
    public void testBatchSetBundleStartLevel() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.refreshPackages(String[])'
     */
    public void testRefreshPackages() throws Exception{
//        try {
//        core.refreshPackages(null);
//        }
//        catch(RuntimeMBeanException e) {
//            assertTrue(e.getTargetException() instanceof IllegalArgumentException);
//        }
//        core.refreshPackages(new String[] {"es.upm.dit.jmood;0.9.0"});
//
    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.resolveBundles(String[])'
     */
    public void testResolveBundles() throws Exception{
//        try {
//        core.resolveBundles(null);
//        }
//        catch(RuntimeMBeanException e) {
//            assertTrue(e.getTargetException() instanceof IllegalArgumentException);
//        }
//        core.resolveBundles(new String[] {"es.upm.dit.jmood;0.9.0"});

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.setPlatformStartLevel(int)'
     */
    public void testSetPlatformStartLevel() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.getPlatformStartLevel()'
     */
    public void testGetPlatformStartLevel() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.setInitialBundleStartLevel(int)'
     */
    public void testSetInitialBundleStartLevel() throws Exception{
//        int init=core.getInitialBundleStartLevel();
//        l.info("INITIAL BUNDLE STARTLEVEL"+ init);
//        int [] good= {1,10, 3};
//        int [] bad= {-1,0};
//        for (int i = 0; i < good.length; i++) {
//            core.setInitialBundleStartLevel(good[i]);
//            assertEquals(core.getInitialBundleStartLevel(), good[i]);
//        }
//        for (int i = 0; i < bad.length; i++) {
//            try {
//            core.setInitialBundleStartLevel(bad[i]);
//            assertTrue(false);
//            } catch(RuntimeMBeanException e) {
//                assertTrue(e.getTargetException() instanceof IllegalArgumentException);
//            }
//        }

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.getInitialBundleStartLevel()'
     */
    public void testGetInitialBundleStartLevel() throws Exception{
//        core.getInitialBundleStartLevel();

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.restartFramework()'
     */
    public void testRestartFramework() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.shutdownFramework()'
     */
    public void testShutdownFramework() {
        // TODO Auto-generated method stub

    }

    /*
     * Test method for
     * 'es.upm.dit.osgi.management.agent.core.CoreController.updateFramework()'
     */
    public void testUpdateFramework() {
        // TODO Auto-generated method stub

    }

}
