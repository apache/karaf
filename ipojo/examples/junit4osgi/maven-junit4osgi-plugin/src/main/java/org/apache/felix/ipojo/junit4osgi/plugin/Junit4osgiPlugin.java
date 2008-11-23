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
package org.apache.felix.ipojo.junit4osgi.plugin;/*

 *
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
    private MavenProject project;
    
    /** @parameter expression="${plugin.artifacts}" */
    private java.util.List pluginArtifacts;
    
    /**
     * Base directory where all reports are written to.
     * 
     * @parameter expression="${project.build.directory}/junit4osgi-reports"
     */
    private File reportsDirectory;
    
    /**
     * Base directory where all reports are written to.
     * 
     * @parameter expression="${project.build.directory}"
     */
    private File targetDir;
    
    /**
     * Must the current artifact be deployed?.
     * 
     * @parameter expression="${deployProjectArtifact}" default-value="false"
     */
    private boolean deployProjectArtifact;
    
    /**
     * Required bundles
     * 
     * @parameter expression="${bundles}"
     */
    private ArrayList bundles;
    
    int total;
    int totalFailures;
    int totalErrors;
    
    List errors = new ArrayList();
    List failures = new ArrayList();
    List results = new ArrayList();
    
    /**
     * Log Service exposed by the plugin framework.
     */
    private LogServiceImpl logService;
    
   
    public void execute() throws MojoFailureException {
        
        List bundles = parseBundleList();
        bundles.addAll(getTestBundle());
        
        List activators = new ArrayList();
        logService = new LogServiceImpl();
        activators.add(logService);
        activators.add(new Installer(pluginArtifacts, bundles, project, deployProjectArtifact));
        Map map = new HashMap();
        map.put("felix.systembundle.activators", activators);
        map.put("org.osgi.framework.storage.clean", "onFirstInit");
        map.put("ipojo.log.level", "WARNING");
        map.put("org.osgi.framework.bootdelegation", "junit.framework, org.osgi.service.log");
        map.put("org.osgi.framework.storage", targetDir.getAbsolutePath() + "/felix-cache"); 

        
        System.out.println("");
        System.out.println("-------------------------------------------------------");
        System.out.println(" T E S T S");        
        System.out.println("-------------------------------------------------------");
        
        Felix felix = new Felix(map);
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
        } catch (Exception e) {
            getLog().error(e);
        }
        
        if (totalErrors > 0 || totalFailures > 0) {
            throw new MojoFailureException("There are test failures. \n\n" +
                    "Please refer to " + reportsDirectory.getAbsolutePath() + 
                    " for the individual test results.");
        }

    }
    
    private void waitForStability(BundleContext context) throws MojoFailureException {
        // Wait for bundle initialization.
        boolean bundleStability = getBundleStability(context);
        int count = 0;
        while(!bundleStability && count < 500) {
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
        int count1 = 0, count2 = 0;
        while(! serviceStability && count < 500) {
            try {
                ServiceReference[] refs = context.getServiceReferences(null, null);
                count1 = refs.length;
                Thread.sleep(500);
                refs = context.getServiceReferences(null, null);
                count2 = refs.length;
                serviceStability = (count1 == count2);
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
    
    private boolean getBundleStability(BundleContext bc) {
        boolean stability = true;
        Bundle[] bundles = bc.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            stability = stability && (bundles[i].getState() == Bundle.ACTIVE);
        }
        return stability;
    }

    private List parseBundleList() {
        List toDeploy = new ArrayList();
        if (bundles == null) {
            return toDeploy;
        }
        
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
    
    private List getTestBundle() {
        List toDeploy = new ArrayList();
        Set dependencies = project.getDependencyArtifacts();
        for (Iterator artifactIterator = dependencies.iterator(); artifactIterator.hasNext();) {
            Artifact artifact = (Artifact) artifactIterator.next();
            if (Artifact.SCOPE_TEST.equals(artifact.getScope())) { // Select scope=test.
                File file = artifact.getFile();
                try {
                    JarFile jar = new JarFile(file);
                    if (jar.getManifest().getMainAttributes().getValue("Bundle-ManifestVersion") != null) {
                        toDeploy.add(file.toURL());
                    }
                } catch (Exception e) {
                    getLog().error(e);
                }
            }
        }
        return toDeploy;
    }
    
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
    
    private void invokeRun(Object runner, BundleContext bc) {
        Method getTest;
        
        try {
            getTest = runner.getClass().getMethod("getTests", new Class[0]);
            List tests = (List) getTest.invoke(runner, new Object[0]);
            Method run = getRunMethod(runner);
            for (int i = 0; i < tests.size(); i++) {
                executeTest(runner, (Test) tests.get(i),run, bc);
            }
            System.out.println("\nResults :");
            if (failures.size() > 0) {
                System.out.println("\nFailed tests:");
                for (int i = 0; i < failures.size(); i++) {
                    TestResult tr = (TestResult) failures.get(i);
                    Enumeration e = tr.failures();
                    while(e.hasMoreElements()) {
                        TestFailure tf = (TestFailure) e.nextElement();
                        System.out.println(" " + tf.toString());
                    }
                }
            }
            
            if (failures.size() > 0) {
                System.out.println("\nTests in error:");
                for (int i = 0; i < errors.size(); i++) {
                    TestResult tr = (TestResult) errors.get(i);
                    Enumeration e = tr.errors();
                    while(e.hasMoreElements()) {
                        TestFailure tf = (TestFailure) e.nextElement();
                        System.out.println(" " + tf.toString());
                    }
                }
            }
            
            System.out.println("\nTests run: " + total + ", Failures: " + totalFailures + ", Errors:" + totalErrors + "\n");          
        } catch (Exception e) {
            getLog().error(e);
        } 
    }
    
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
    
    private void executeTest(Object runner, Test test, Method run, BundleContext bc) {
        try {
        XMLReport report = new XMLReport();
        String name = getTestName(test);
        System.out.println("Running " + name);

        TestResult tr = new TestResult();
        tr.addListener(new ResultListener(report));
        test.run(tr);        
        results.add(tr);
        
        if (tr.wasSuccessful()) {
            System.out.println("Tests run: " + tr.runCount() + ", Failures: " + tr.failureCount() + ", Errors: " + tr.errorCount() + ", Time elapsed: " + report.elapsedTimeAsString(report.endTime - report.endTime) + " sec");
        } else {
            System.out.println("Tests run: " + tr.runCount() + ", Failures: " + tr.failureCount() + ", Errors: " + tr.errorCount() + ", Time elapsed: " + report.elapsedTimeAsString(report.endTime - report.endTime) + " sec <<< FAILURE!");
            if (tr.errorCount() > 0) {
                errors.add(tr);
            }
            if (tr.failureCount() > 0) {
                failures.add(tr);
            }
        }
        
        total += tr.runCount();
        totalFailures += tr.failureCount();
        totalErrors += tr.errorCount();
        
        report.generateReport(test, tr, reportsDirectory, bc);
        
        } catch (Exception e) {
            getLog().error(e);
        }
        
       
        
    }
    
    public void dumpBundles(BundleContext bc) {
        getLog().info("Bundles:");
        Bundle[] bundles = bc.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            getLog().info(bundles[i].getSymbolicName() + " - " + bundles[i].getState());
        }
    }
    
    public LogServiceImpl getLogService() {
        return logService;
    }
    
    
    private class ResultListener implements TestListener {
        
        private XMLReport report;
        private boolean abort;
        
        private PrintStream outBackup = System.out;
        private PrintStream errBackup = System.err;
        
        private StringOutputStream out = new StringOutputStream();
        private StringOutputStream err = new StringOutputStream();;
        
        public ResultListener(XMLReport report) {
            this.report = report;
        }

        public void addError(Test test, Throwable throwable) {
            report.testError(test, throwable, out.toString(), err.toString(), getLogService().getLoggedMessages());
            abort = true;
        }

        public void addFailure(Test test,
                AssertionFailedError assertionfailederror) {
            report.testFailed(test, assertionfailederror, out.toString(), err.toString(), getLogService().getLoggedMessages());
            abort = true;
            
        }

        public void endTest(Test test) {
           if (!abort) {
               report.testSucceeded(test);
           }
           System.setErr(errBackup);
           System.setOut(outBackup);
           getLogService().reset();
        }

        public void startTest(Test test) {
            abort = false;
            report.testStarting();
            System.setErr(new PrintStream(err));
            System.setOut(new PrintStream(out));
            getLogService().enableOutputStream();
        }
        
    }
    
}
