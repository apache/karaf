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

package org.apache.felix.sigil.junit.server.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.felix.sigil.junit.server.JUnitService;
import org.osgi.framework.BundleContext;

public class JUnitServiceImpl implements JUnitService {
	
	private static final Logger log = Logger.getLogger(JUnitServiceImpl.class.getName());

	private static final Class<?>[] BUNDLE_CONTEXT_PARAMS = new Class[] { BundleContext.class };

	private final JUnitServiceFactory junitServiceFactory;
	private final BundleContext bundleContext;

	public JUnitServiceImpl(JUnitServiceFactory junitServiceFactory, BundleContext bundleContext) {
		this.junitServiceFactory = junitServiceFactory;
		this.bundleContext = bundleContext;
	}

	public Set<String> getTests() {
		return junitServiceFactory.getTests();
	}
	
	public TestSuite createTest(String test) {
		return createTest(test, null);
	}
	
	public TestSuite createTest(String test, BundleContext ctx) {
		try {
			TestSuite ts = junitServiceFactory.getTest(test);
			
			if ( ts == null ) return null;
			
			TestSuite ret = new TestSuite(ts.getName());
			
			Enumeration<Test> e = ts.tests();
			
			while ( e.hasMoreElements() ) {
				Test t = e.nextElement();
				setContext(t, ctx);
				ret.addTest(t);
			}
			
			return ret;
		}
		catch (final NoClassDefFoundError e) {
			TestSuite s = new TestSuite(test);
			s.addTest( new Test() {
				public int countTestCases() {
					return 1;
				}

				public void run(TestResult result) {
					result.addError(this, e);
				}
			});
			return s;
		}
		catch (final RuntimeException e) {
			TestSuite s = new TestSuite(test);
			s.addTest( new Test() {
				public int countTestCases() {
					return 1;
				}

				public void run(TestResult result) {
					result.addError(this, e);
				}
				
			});
			return s;
		}
	}

	private void setContext(Test t, BundleContext ctx) {
		try {
			Method m = findMethod( t.getClass(), "setBundleContext", BUNDLE_CONTEXT_PARAMS );
			if ( m != null )
				m.invoke(t, ctx == null ? bundleContext : ctx );
		} catch (SecurityException e) {
			log.log( Level.WARNING, "Failed to set bundle context on " + t, e);
		} catch (IllegalArgumentException e) {
			log.log( Level.WARNING, "Failed to set bundle context on " + t, e);
		} catch (IllegalAccessException e) {
			log.log( Level.WARNING, "Failed to set bundle context on " + t, e);
		} catch (InvocationTargetException e) {
			log.log( Level.WARNING, "Failed to set bundle context on " + t, e);
		}
	}

	private Method findMethod(Class<?> clazz, String name,
			Class<?>[] params) {
		Method found = null;
		
		for ( Method m : clazz.getDeclaredMethods() ) {
			if ( m.getName().equals(name) && Arrays.deepEquals(m.getParameterTypes(), params) ) {
				found = m;
				break;
			}
		}
		
		if ( found == null ) {
			Class<?> c = clazz.getSuperclass();
			
			if ( c != null && c != Object.class ) {
				found = findMethod(c, name, params);
			}
		}
		
		if ( found == null ) {
			for ( Class<?> c : clazz.getInterfaces() ) {
				found = findMethod(c, name, params);
				if ( found != null ) {
					break;
				}
			}
		}
		
		return found;
	}
}
