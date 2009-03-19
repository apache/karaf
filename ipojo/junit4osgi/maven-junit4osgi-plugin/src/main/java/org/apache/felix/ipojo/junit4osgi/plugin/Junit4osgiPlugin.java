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
package org.apache.felix.ipojo.junit4osgi.plugin;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;

import org.apache.felix.framework.Felix;
import org.apache.felix.ipojo.junit4osgi.plugin.log.LogServiceImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

/**
 * Goal starting Felix and executing junit4osgi tests.
 *  
 * @goal test
 * @phase integration-test
 * @requiresDependencyResolution runtime
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * 
 */
public class Junit4osgiPlugin extends AbstractMojo {

    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject m_project;
    
    /**
     * Dependencies of the current plugin. 
     * @parameter expression="${plugin.artifacts}"
     */
    private java.util.List m_pluginArtifacts;
    
    /**
     * Base directory where all reports are written to.
     * 
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    private File m_reportsDirectory;
    
    /**
     * Base directory where all reports are written to.
     * 
     * @parameter expression="${project.build.directory}"
     */
    private File m_targetDir;
    
    /**
     * Must the current artifact be deployed?
     * 
     * @parameter expression="${deployProjectArtifact}" default-value="true"
     */
    private boolean m_deployProjectArtifact;
    
    /**
     * Required bundles.
     * 
     * @parameter
     */
    private List bundles;
    
    /**
     * Felix configuration.
     * 
     * @parameter
     */
    private Map configuration;
    
    /**
     * Enables / Disables the log service provided by the plugin.
     * 
     * @parameter expression="${logService}" default-value="true"
     */
    private boolean m_logEnable;
    
    /**
     * Number of executed test case.
     */
    private int m_total;
    
    /**
     * Number of failing test case.
     */
    private int m_totalFailures;
    
    /**
     * Number of test case in error .
     */
    private int m_totalErrors;
    
    /**
     * Test results in error.
     */
    private List m_errors = new ArrayList();
    
    /**
     * Failing test results. 
     */
    private List m_failures = new ArrayList();
    
    /**
     * Test results.
     */
    private List m_results = new ArrayList();
    
    /**
     * Log Service exposed by the plug-in framework.
     */
    private LogServiceImpl m_logService;
    
    /**
     * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, especially if you
     * enable it using the "maven.test.skip" property, because maven.test.skip disables both running the
     * tests and compiling the tests.  Consider using the skipTests parameter instead.
     * 
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;
    
    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     * 
     * @parameter expression="${maven.test.failure.ignore}"
     */
    private boolean testFailureIgnore;
    
    /**
     * Set this to avoid printing test execution trace on System.out and System.err. This will be written in the
     * reports.
     * @parameter 
     */
    private boolean hideOutputs;
    
    /**
     * Felix configuration.
     */
    private Map felixConf;
    
   
    /**
     * Executes the plug-in.
     * @throws MojoFailureException when the test execution failed.
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoFailureException {
        
        if (skip) {
            getLog().info("Tests are skipped");
            return;
        }
       
        
        List bundles = parseBundleList();
        bundles.addAll(getTestBundle());
        
        List activators = new ArrayList();
        m_logService = new LogServiceImpl();
        if (m_logEnable) { // Starts the log service if enabled
            activators.add(m_logService);
        } else {
            getLog().info("Log Service disabled");
        }
        activators.add(new Installer(m_pluginArtifacts, bundles, m_project, m_deployProjectArtifact));
        felixConf = new HashMap();
        felixConf.put("felix.systembundle.activators", activators);
        felixConf.put("org.osgi.framework.storage.clean", "onFirstInit");
        felixConf.put("ipojo.log.level", "WARNING");
        // Use a boot delagation to share classes between the host and the embedded Felix.
        // The junit.framework package is boot delegated to execute tests
        // The log service package is also boot delegated as the host publish a log service
        // The cobertura package is used during code coverage collection
        felixConf.put("org.osgi.framework.bootdelegation", "junit.framework, org.osgi.service.log, net.sourceforge.cobertura.coveragedata"); 
        
        felixConf.put("org.osgi.framework.storage", m_targetDir.getAbsolutePath() + "/felix-cache"); 
        
        
        if (configuration != null) {
            felixConf.putAll(configuration);
            // Check boot delegation
            String bd = (String) felixConf.get("org.osgi.framework.bootdelegation");
            if (bd.indexOf("junit.framework") == -1) {
                bd.concat(", junit.framework");
            }
            if (bd.indexOf("org.osgi.service.log") == -1) {
                bd.concat(", org.osgi.service.log");
            }
            if (bd.indexOf("net.sourceforge.cobertura.coveragedata") == -1) {
                bd.concat(", net.sourceforge.cobertura.coveragedata");
            }
        }
        
        System.out.println("");
        System.out.println("-------------------------------------------------------");
        System.out.println(" T E S T S");        
        System.out.println("-------------------------------------------------------");
        
        Felix felix = new Felix(felixConf);
        try {
            felix.start();
        } catch (BundleException e) {
            e.printStackTrace();
        }
        
        waitForStability(felix.getBundleContext());
        

        Object runner = waitForRunnerService(felix.getBundleContext());
        invokeRun(runner, felix.getBundleContext());
                
        try {
            felix.stop();
            felix.waitForStop(5000);
            // Delete felix-cache
            File cache = new File(m_targetDir.getAbsolutePath() + "/felix-cache");
            cache.delete();
        } catch (Exception e) {
            getLog().error(e);
        }
        
        if (m_totalErrors > 0 || m_totalFailures > 0) {
            if (! testFailureIgnore) {
            throw new MojoFailureException("There are test failures. \n\n"
                    + "Please refer to " + m_reportsDirectory.getAbsolutePath()
                    + " for the individual test results.");
            } else {
                getLog().warn("There are test failures. \n\n"
                    + "Please refer to " + m_reportsDirectory.getAbsolutePath()
                    + " for the individual test results.");
            }
        }

    }
    
    /**
     * Waits for stability:
     * <ul>
     * <li>all bundles are activated
     * <li>service count is stable
     * </ul>
     * If the stability can't be reached after a specified time,
     * the method throws a {@link MojoFailureException}.
     * @param context the bundle context
     * @throws MojoFailureException when the stability can't be reach after a several attempts.
     */
    private void waitForStability(BundleContext context) throws MojoFailureException {
        // Wait for bundle initialization.
        boolean bundleStability = getBundleStability(context);
        int count = 0;
        while (!bundleStability && count < 500) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // Interrupted
            }
            count++;
            bundleStability = getBundleStability(context);
        }
        
        if (count == 500) {
            getLog().error("Bundle stability isn't reached after 500 tries");
            dumpBundles(context);
            throw new MojoFailureException("Cannot reach the bundle stability");
        }
        
        boolean serviceStability = false;
        count = 0;
        int count1 = 0;
        int count2 = 0;
        while (! serviceStability && count < 500) {
            try {
                ServiceReference[] refs = context.getServiceReferences(null, null);
                count1 = refs.length;
                Thread.sleep(500);
                refs = context.getServiceReferences(null, null);
                count2 = refs.length;
                serviceStability = count1 == count2;
            } catch (Exception e) {
                getLog().error(e);
                serviceStability = false;
                // Nothing to do, while recheck the condition
            }
            count++;
        }
        
        if (count == 500) {
            getLog().error("Service stability isn't reached after 500 tries (" + count1 + " != " + count2);
            dumpBundles(context);
            throw new MojoFailureException("Cannot reach the service stability");
        }
        
    }
    
    /**
     * Are bundle stables.
     * @param bc the bundle context
     * @return <code>true</code> if every bundles are activated.
     */
    private boolean getBundleStability(BundleContext bc) {
        boolean stability = true;
        Bundle[] bundles = bc.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            stability = stability && (bundles[i].getState() == Bundle.ACTIVE);
        }
        return stability;
    }

    /**
     * Computes the URL list of bundles to install from
     * the <code>bundles</code> parameter.
     * @return the list of url of bundles to install.
     */
    private List parseBundleList() {
        List toDeploy = new ArrayList();
        if (bundles == null) {
            return toDeploy;
        }
        System.out.println("Deploy URL bundles " + bundles); // TODO
        for (int i = 0; i < bundles.size(); i++) {
            String bundle = (String) bundles.get(i);
            try {
                URL url = new URL(bundle);
                toDeploy.add(url);
            } catch (MalformedURLException e) {
                // Not a valid url,
                getLog().error(bundle + " is not a valid url, bundle ignored");
            }
        }
        
        return toDeploy;
    }
    
    /**
     * Computes the URL list of bundles to install from 
     * <code>test</code> scoped dependencies.
     * @return the list of url of bundles to install.
     */
    private List getTestBundle() {
        List toDeploy = new ArrayList();
        Set dependencies = m_project.getDependencyArtifacts();
        for (Iterator artifactIterator = dependencies.iterator(); artifactIterator.hasNext();) {
            Artifact artifact = (Artifact) artifactIterator.next();
            if (artifact.getScope() != null) { // Select not null scope... [Select every bundles with a scope TEST, COMPILE and RUNTIME]
                File file = artifact.getFile();
                try {
                    if (file.exists()) {
                        if (file.getName().endsWith("jar")) {
                            JarFile jar = new JarFile(file);
                            if (jar.getManifest().getMainAttributes().getValue("Bundle-ManifestVersion") != null) {
                                toDeploy.add(file.toURL());
                            }
                        } // else {
//                            getLog().info("The artifact " + artifact.getFile().getName() + " is not a Jar file.");
//                        }
                    } else {
                        getLog().info("The artifact " + artifact.getFile().getName() + " does not exist.");
                    }
                } catch (Exception e) {
                    getLog().error(file + " is not a valid bundle, this artifact is ignored");
                }
            }
        }
        return toDeploy;
    }
    
    /**
     * Waits until the {@link OSGiJunitRunner} service
     * is published.
     * @param bc the bundle context
     * @return the {@link OSGiJunitRunner} service object.
     */
    private Object waitForRunnerService(BundleContext bc) {
        ServiceReference ref = bc.getServiceReference(org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner.class.getName());
        int count = 0;
        while (ref == null && count < 1000) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // Nothing to do
            }
            ref = bc.getServiceReference(org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner.class.getName());
        }
        if (ref != null) {
            return bc.getService(ref);
        }
        getLog().error("Junit Runner service unavailable");
        return null;
    }
    
    /**
     * Executes tests by using reflection.
     * @param runner the {@link OSGiJunitRunner} service object
     * @param bc the bundle context
     */
    private void invokeRun(Object runner, BundleContext bc) {
        Method getTest;
        
        try {
            getTest = runner.getClass().getMethod("getTests", new Class[0]);
            List tests = (List) getTest.invoke(runner, new Object[0]);
            Method run = getRunMethod(runner);
            for (int i = 0; i < tests.size(); i++) {
                executeTest(runner, (Test) tests.get(i), run, bc);
            }
            System.out.println("\nResults :");
            if (m_failures.size() > 0) {
                System.out.println("\nFailed tests:");
                for (int i = 0; i < m_failures.size(); i++) {
                    TestResult tr = (TestResult) m_failures.get(i);
                    Enumeration e = tr.failures();
                    while (e.hasMoreElements()) {
                        TestFailure tf = (TestFailure) e.nextElement();
                        System.out.println(" " + tf.toString());
                    }
                }
            }
            
            if (m_failures.size() > 0) {
                System.out.println("\nTests in error:");
                for (int i = 0; i < m_errors.size(); i++) {
                    TestResult tr = (TestResult) m_errors.get(i);
                    Enumeration e = tr.errors();
                    while (e.hasMoreElements()) {
                        TestFailure tf = (TestFailure) e.nextElement();
                        System.out.println(" " + tf.toString());
                    }
                }
            }
            
            System.out.println("\nTests run: " + m_total + ", Failures: " + m_totalFailures + ", Errors:" + m_totalErrors + "\n");          
        } catch (Exception e) {
            getLog().error(e);
        } 
    }
    
    /**
     * Gets the {@link OSGiJunitRunner#run(long)} method from the 
     * {@link OSGiJunitRunner} service object.
     * @param runner the {@link OSGiJunitRunner} service object.
     * @return the Method object for the <code>run</code> method.
     */
    private Method getRunMethod(Object runner) {
        Method[] methods = runner.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals("run") 
                    && methods[i].getParameterTypes().length == 1
                    && ! methods[i].getParameterTypes()[0].equals(Long.TYPE)) {
                return methods[i];
            }
        }
        getLog().error("Cannot find the run method");
        return null;
    }
    
    /**
     * Computes the name of the given test.
     * This method calls the {@link TestCase#getName()}
     * method by reflection. If no success, invokes the
     * {@link Object#toString()}  method.
     * @param test the test object.
     * @return the name of the given test.
     */
    private String getTestName(Object test) {
        try {
            Method getName = test.getClass().getMethod("getName", new Class[0]);
            String name = (String) getName.invoke(test, new Object[0]);
            if (name == null) {
                name = test.toString();
            }
            return name;
        } catch (Exception e) {
            getLog().error(e);
            return null;
        }
            
    }
    
    /**
     * Executes the given test.
     * @param runner the {@link OSGiJunitRunner} service object
     * @param test the test to run
     * @param run the {@link OSGiJunitRunner#run(long)} method
     * @param bc the bundle context
     */
    private void executeTest(Object runner, Test test, Method run,
            BundleContext bc) {
        try {
            XMLReport report = new XMLReport();
            String name = getTestName(test);
            System.out.println("Running " + name);

            TestResult tr = new TestResult();
            tr.addListener(new ResultListener(report));
            test.run(tr);
            m_results.add(tr);

            if (tr.wasSuccessful()) {
                System.out.println("Tests run: "
                        + tr.runCount()
                        + ", Failures: "
                        + tr.failureCount()
                        + ", Errors: "
                        + tr.errorCount()
                        + ", Time elapsed: "
                        + report.elapsedTimeAsString(report.m_endTime
                                - report.m_endTime) + " sec");
            } else {
                System.out.println("Tests run: "
                        + tr.runCount()
                        + ", Failures: "
                        + tr.failureCount()
                        + ", Errors: "
                        + tr.errorCount()
                        + ", Time elapsed: "
                        + report.elapsedTimeAsString(report.m_endTime
                                - report.m_endTime) + " sec <<< FAILURE!");
                if (tr.errorCount() > 0) {
                    m_errors.add(tr);
                }
                if (tr.failureCount() > 0) {
                    m_failures.add(tr);
                }
            }

            m_total += tr.runCount();
            m_totalFailures += tr.failureCount();
            m_totalErrors += tr.errorCount();

            report.generateReport(test, tr, m_reportsDirectory, bc, felixConf);

        } catch (Exception e) {
            getLog().error(e);
        }

    }
    
    /**
     * Prints the bundle list.
     * @param bc the bundle context.
     */
    public void dumpBundles(BundleContext bc) {
        getLog().info("Bundles:");
        Bundle[] bundles = bc.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            getLog().info(bundles[i].getSymbolicName() + " - " + bundles[i].getState());
        }
    }
    
    public LogServiceImpl getLogService() {
        return m_logService;
    }
    
    
    private class ResultListener implements TestListener {
        
        /**
         * The XML Report.
         */
        private XMLReport m_report;
        
        /**
         * Check if the test has failed or thrown an
         * error.
         */
        private boolean m_abort;
        
        /**
         * Backup of the {@link System#out} stream.
         */
        private PrintStream m_outBackup = System.out;
        
        /**
         * Backup of the {@link System#err} stream.
         */
        private PrintStream m_errBackup = System.err;
        
        /**
         * The output stream used during the test execution. 
         */
        private StringOutputStream m_out = new StringOutputStream();
        
        /**
         * The error stream used during the test execution. 
         */
        private StringOutputStream m_err = new StringOutputStream();;
        
        /**
         * Creates a ResultListener.
         * @param report the XML report
         */
        public ResultListener(XMLReport report) {
            this.m_report = report;
        }

        /**
         * An error occurs during the test execution.
         * @param test the test in error
         * @param throwable the thrown error
         * @see junit.framework.TestListener#addError(junit.framework.Test, java.lang.Throwable)
         */
        public void addError(Test test, Throwable throwable) {
            m_report.testError(test, throwable, m_out.toString(), m_err.toString(), getLogService().getLoggedMessages());
            m_abort = true;
        }

        /**
         * An failure occurs during the test execution.
         * @param test the failing test
         * @param assertionfailederror the failure
         * @see junit.framework.TestListener#addFailure(junit.framework.Test, junit.framework.AssertionFailedError)
         */
        public void addFailure(Test test,
                AssertionFailedError assertionfailederror) {
            m_report.testFailed(test, assertionfailederror, m_out.toString(), m_err.toString(), getLogService().getLoggedMessages());
            m_abort = true;
            
        }

        /**
         * The test ends.
         * @param test the test
         * @see junit.framework.TestListener#endTest(junit.framework.Test)
         */
        public void endTest(Test test) {
            if (!m_abort) {
                m_report.testSucceeded(test);
            }
            System.setErr(m_errBackup);
            System.setOut(m_outBackup);
            getLogService().reset();
        }

        /**
         * The test starts.
         * @param test the test
         * @see junit.framework.TestListener#startTest(junit.framework.Test)
         */
        public void startTest(Test test) {
            m_abort = false;
            m_report.testStarting();
            System.setErr(new ReportPrintStream(m_err,m_errBackup, hideOutputs));
            System.setOut(new ReportPrintStream(m_out, m_outBackup, hideOutputs));
            getLogService().enableOutputStream();
        }
        
    }
    
}
