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
package org.apache.felix.ipojo.junit4osgi.impl;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.OSGiTestSuite;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Detect test suite from installed bundles.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class JunitExtender implements OSGiJunitRunner {

    /**
     * Suite method name.
     */
    public static final String SUITE_METHODNAME = "suite";

    /**
     * List of found suites (Bundle-> List of Class).
     */
    private Map/*<Bundle, List<Class>>*/ m_suites = new HashMap/*<Bundle, List<Class>>*/();

    /**
     * The result printer.
     * By default, prints result on {@link System#out}
     */
    private ResultPrinter m_printer = new ResultPrinter(System.out);
    
    /**
     * The log service used to log messages.
     * If not provided, a default implementation
     * printing messages on the console is used.
     */
    private LogService m_log;

    /**
     * A new matching bundle arrives.
     * @param bundle the matching bundle
     * @param header the looked header value
     */
    void onBundleArrival(Bundle bundle, String header) {
        String[] tss = ParseUtils.split(header, ",");
        for (int i = 0; i < tss.length; i++) {
            try {
                if (tss[i].length() != 0) {
                    m_log.log(LogService.LOG_INFO, "Loading " + tss[i]);
                    Class/*<? extends Test>*/ clazz = bundle.loadClass(tss[i].trim());
                    addTestSuite(bundle, clazz);
                }
            } catch (ClassNotFoundException e) {
                m_log.log(LogService.LOG_ERROR, "The test suite " + tss[i] + " is not in the bundle " + bundle.getBundleId() + " : " + e.getMessage());
            }
        }
    }

    /**
     * Adds a test suite.
     * @param bundle the bundle declaring the test suite.
     * @param test the test class.
     */
    private synchronized void addTestSuite(Bundle bundle, Class/*<? extends Test>*/ test) {
        List/*<Class>*/ list = (List) m_suites.get(bundle);
        if (list == null) {
            list = new ArrayList/*<Class>*/();
            list.add(test);
            m_suites.put(bundle, list);
        } else {
            list.add(test);
        }
    }

    /**
     * Removes the test suites provided by the given bundles.
     * @param bundle the leaving bundles.
     */
    private synchronized void removeTestSuites(Bundle bundle) {
        List list = (List) m_suites.remove(bundle);
        m_log.log(LogService.LOG_INFO, "Unload test suites " + list);
    }

    /**
     * A matching bundle is leaving.
     * @param bundle the leaving bundle.
     */
    void onBundleDeparture(Bundle bundle) {
        removeTestSuites(bundle);
    }

    /**
     * Set the result printer.
     * @param pw the stream to use.
     * @see org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner#setResultPrinter(java.io.PrintStream)
     */
    public void setResultPrinter(PrintStream pw) {
        m_printer = new ResultPrinter(pw);
    }

    /**
     * Runs tests.
     * @return the list of {@link TestResult}
     * @see org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner#run()
     */
    public synchronized List/*<TestResult>*/ run() {
        List/*<TestResult>*/ results = new ArrayList/*<TestResult>*/(m_suites.size());
        Iterator/*<Entry<Bundle, List<Class>>>*/ it = m_suites.entrySet().iterator();
        while (it.hasNext()) {
            Entry/*<Bundle, List<Class>>*/ entry = (Entry) it.next();
            Bundle bundle = (Bundle) entry.getKey();
            List/*<Class>*/ list = (List) m_suites.get(bundle);
            for (int i = 0; i < list.size(); i++) {
                Test test = createTestFromClass((Class) list.get(i), bundle);
                TestResult tr = doRun(test);
                results.add(tr);
            }
        }
        return results;
    }

    /**
     * Internal methods executing tests.
     * @param test the test to execute
     * @return the result
     */
    private TestResult doRun(Test test) {
        TestResult result = new TestResult();
        result.addListener(m_printer);
        long startTime = System.currentTimeMillis();

        test.run(result);

        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;
        m_printer.print(result, runTime);

        return result;
    }

    /**
     * Creates a {@link Test} object from the
     * given class from the given bundle.
     * This method creates {@link OSGiTestCase} and
     * {@link OSGiTestSuite} when required.
     * @param clazz the class
     * @param bundle the bundle
     * @return the resulting Test object.
     */
    private Test createTestFromClass(Class/*<?>*/ clazz, Bundle bundle) {
        Method suiteMethod = null;
        boolean bc = false;
        try {
            suiteMethod = clazz.getMethod(SUITE_METHODNAME, new Class[0]);
        } catch (Exception e) {
            // try to use a suite method receiving a bundle context
            try {
                suiteMethod = clazz.getMethod(SUITE_METHODNAME, new Class[] { BundleContext.class });
                bc = true;
            } catch (Exception e2) {
                // try to extract a test suite automatically
                if (OSGiTestSuite.class.isAssignableFrom(clazz)) {
                    OSGiTestSuite ts = new OSGiTestSuite(clazz, getBundleContext(bundle));
                    return ts;
                } else if (OSGiTestCase.class.isAssignableFrom(clazz)) {
                    OSGiTestSuite ts = new OSGiTestSuite(clazz, getBundleContext(bundle));
                    return ts;
                } else {
                    return new TestSuite(clazz);
                }
            }
        }

        if (!Modifier.isStatic(suiteMethod.getModifiers())) {
            m_log.log(LogService.LOG_ERROR, "Suite() method must be static");
            return null;
        }
        Test test = null;
        try {
            if (bc) {
                test = (Test) suiteMethod.invoke(null, new Object[] { getBundleContext(bundle) }); // static method injection the bundle context
            } else {
                test = (Test) suiteMethod.invoke(null, (Object[]) new Class[0]); // static method
            }
        } catch (InvocationTargetException e) {
            m_log.log(LogService.LOG_ERROR, "Failed to invoke suite():" + e.getTargetException().toString());
            return null;
        } catch (IllegalAccessException e) {
            m_log.log(LogService.LOG_ERROR, "Failed to invoke suite():" + e.toString());
            return null;
        }

        return test;
    }

    /**
     * Gets the list of {@link Test}.
     * @return the list of {@link Test}
     * @see org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner#getTests()
     */
    public synchronized List/*<Test>*/ getTests() {
        List/*<Test>*/ results = new ArrayList/*<Test>*/();
        Iterator/*<Entry<Bundle, List<Class>>>*/ it = m_suites.entrySet().iterator();
        while (it.hasNext()) {
            Entry/*<Bundle, List<Class>>*/ entry = (Entry) it.next();
            Bundle bundle = (Bundle) entry.getKey();
            List/*<Class>*/ list = (List) m_suites.get(bundle);
            for (int i = 0; i < list.size(); i++) {
                Test test = createTestFromClass((Class) list.get(i), bundle);
                results.add(test);
            }
        }
        return results;
    }

    /**
     * Runs the given tests.
     * @param test the test to execute
     * @return the result
     * @see org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner#run(junit.framework.Test)
     */
    public TestResult run(Test test) {
        return doRun(test);
    }

    /**
     * Gets the list of {@link Test} from the bundle (specified
     * by using the bundle id).
     * @param bundleId the bundle id
     * @return the list of {@link Test} declared in this bundle.
     * @see org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner#getTests(long)
     */
    public synchronized List/*<Test>*/ getTests(long bundleId) {
        Iterator/*<Entry<Bundle, List<Class>>>*/ it = m_suites.entrySet().iterator();
        while (it.hasNext()) {
            Entry/*<Bundle, List<Class>>*/ entry = (Entry) it.next();
            Bundle bundle = (Bundle) entry.getKey();
            if (bundle.getBundleId() == bundleId) {
                List/*<Test>*/ results = new ArrayList/*<Test>*/();
                List/*<Class>*/ list = (List) m_suites.get(bundle);
                for (int i = 0; i < list.size(); i++) {
                    Test test = createTestFromClass((Class) list.get(i), bundle);
                    results.add(test);
                }
                return results;
            }
        }
        return null;
    }

    /**
     * Runs the tests declared in the bundle
     * (specified by the bundle id).
     * @param bundleId the bundle id
     * @return the List of {@link TestResult}
     * @see org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner#run(long)
     */
    public synchronized List/*<TestResult>*/ run(long bundleId) {
        Iterator/*<Entry<Bundle, List<Class>>>*/ it = m_suites.entrySet().iterator();
        while (it.hasNext()) {
            Entry/*<Bundle, List<Class>>*/ entry = (Entry) it.next();
            Bundle bundle = (Bundle) entry.getKey();
            if (bundle.getBundleId() == bundleId) {
                List/*<TestResult>*/ results = new ArrayList/*<TestResult>*/();
                List/*<Class>*/ list = (List) m_suites.get(bundle);
                for (int i = 0; i < list.size(); i++) {
                    Test test = createTestFromClass((Class) list.get(i), bundle);
                    TestResult tr = doRun(test);
                    results.add(tr);
                }
                return results;
            }
        }
        return null;
    }

    /**
     * Stop method.
     * Clears test suites.
     */
    public synchronized void stopping() {
        m_log.log(LogService.LOG_INFO, "Cleaning test suites ...");
        m_suites.clear();
    }
    
    /**
     * Start method.
     */
    public void starting() {
        m_log.log(LogService.LOG_INFO, "Junit Extender starting ...");
    }

    /**
     * Helper method analyzing the {@link Bundle} object
     * to get the {@link BundleContext} object.
     * @param bundle the Bundle
     * @return the BundleContext of <code>null</code> if
     * the BundleContext cannot be collected.
     */
    private BundleContext getBundleContext(Bundle bundle) {
        if (bundle == null) { return null; }

        // getBundleContext (OSGi 4.1)
        Method meth = null;
        try {
            meth = bundle.getClass().getMethod("getBundleContext", new Class[0]);
        } catch (SecurityException e) {
            // Nothing do to, will try the Equinox method
        } catch (NoSuchMethodException e) {
            // Nothing do to, will try the Equinox method
        }

        // try Equinox getContext if not found.
        if (meth == null) {
            try {
                meth = bundle.getClass().getMethod("getContext", new Class[0]);
            } catch (SecurityException e) {
                // Nothing do to, will try field inspection
            } catch (NoSuchMethodException e) {
                // Nothing do to, will try field inspection
            }
        }

        if (meth != null) {
            if (! meth.isAccessible()) {
                meth.setAccessible(true);
            }
            try {
                return (BundleContext) meth.invoke(bundle, new Object[0]);
            } catch (IllegalArgumentException e) {
                m_log.log(LogService.LOG_ERROR, "Cannot get the BundleContext by invoking " + meth.getName(), e);
                return null;
            } catch (IllegalAccessException e) {
                m_log.log(LogService.LOG_ERROR, "Cannot get the BundleContext by invoking " + meth.getName(), e);
                return null;
            } catch (InvocationTargetException e) {
                m_log.log(LogService.LOG_ERROR, "Cannot get the BundleContext by invoking " + meth.getName(), e);
                return null;
            }
        }

        // Else : Field inspection (KF and Prosyst)        
        Field[] fields = bundle.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (BundleContext.class.isAssignableFrom(fields[i].getType())) {
                if (! fields[i].isAccessible()) {
                    fields[i].setAccessible(true);
                }
                try {
                    return (BundleContext) fields[i].get(bundle);
                } catch (IllegalArgumentException e) {
                    m_log.log(LogService.LOG_ERROR, "Cannot get the BundleContext by reflecting on " + fields[i].getName(), e);
                    return null;
                } catch (IllegalAccessException e) {
                    m_log.log(LogService.LOG_ERROR, "Cannot get the BundleContext by reflecting on " + fields[i].getName(), e);
                    return null;
                }
            }
        }
        m_log.log(LogService.LOG_ERROR, "Cannot find the BundleContext for " + bundle.getSymbolicName(), null);
        return null;
    }

}
