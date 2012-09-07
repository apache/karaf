/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KarafBundleCommandsTest extends KarafTestSupport {

    @Test
    public void testBundleCapabilitiesCommand() throws Exception {
        String allCapabilitiesOutput = executeCommand("bundle:capabilities");
        System.out.println(allCapabilitiesOutput);
        assertFalse(allCapabilitiesOutput.isEmpty());
        String jmxWhiteboardBundleCapabilitiesOutput = executeCommand("bundle:capabilities 74");
        System.out.println(jmxWhiteboardBundleCapabilitiesOutput);
        assertTrue(jmxWhiteboardBundleCapabilitiesOutput.contains("osgi.wiring.bundle; org.apache.aries.jmx.whiteboard 1.0.0 [UNUSED]"));
    }

    @Test
    public void testBundleClassesCommand() throws Exception {
        String allClassesOutput = executeCommand("bundle:classes");
        System.out.println(allClassesOutput);
        assertFalse(allClassesOutput.isEmpty());
        String jmxWhiteboardBundleClassesOutput = executeCommand("bundle:classes 74");
        System.out.println(jmxWhiteboardBundleClassesOutput);
        assertTrue(jmxWhiteboardBundleClassesOutput.contains("org/apache/aries/jmx/whiteboard/Activator$MBeanTracker.class"));
    }

    @Test
    public void testBundleDiagCommand() throws Exception {
        String allDiagOutput = executeCommand("bundle:diag");
        System.out.println(allDiagOutput);
        assertFalse(allDiagOutput.isEmpty());
    }

    @Test
    public void testBundleFindClassCommand() throws Exception {
        String findClassOutput = executeCommand("bundle:find-class jmx");
        System.out.println(findClassOutput);
        assertFalse(findClassOutput.isEmpty());
    }

    @Test
    public void testBundleHeadersCommand() throws Exception {
        String headersOutput = executeCommand("bundle:headers 74");
        System.out.println(headersOutput);
        assertTrue(headersOutput.contains("Bundle-Activator = org.apache.aries.jmx.whiteboard.Activator"));
    }

    @Test
    public void testBundleInfoCommand() throws Exception {
        String infoOutput = executeCommand("bundle:info 69");
        System.out.println(infoOutput);
        assertTrue(infoOutput.contains("This bundle starts the Karaf embedded MBean server"));
    }

    @Test
    public void testBundleInstallCommand() throws Exception {
        System.out.println(executeCommand("bundle:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-lang/2.4_6"));
        String bundleListOutput = executeCommand("bundle:list -l | grep -i commons-lang");
        System.out.println(bundleListOutput);
        assertFalse(bundleListOutput.isEmpty());
    }

    @Test
    public void testBundleShowTreeCommand() throws Exception {
        String bundleTreeOutput = executeCommand("bundle:tree-show 69");
        System.out.println(bundleTreeOutput);
        assertFalse(bundleTreeOutput.isEmpty());
    }

}
