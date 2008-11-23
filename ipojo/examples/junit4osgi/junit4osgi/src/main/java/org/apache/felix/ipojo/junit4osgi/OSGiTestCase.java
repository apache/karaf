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
package org.apache.felix.ipojo.junit4osgi;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * OSGi Test Case. Allow the injection of the bundle context.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class OSGiTestCase extends TestCase {

	protected BundleContext context;
	
	
	private List references = new ArrayList();
	
    /**
     * Extends runBare to release (unget) services after the teardown.
     * @throws Throwable when an error occurs.
     * @see junit.framework.TestCase#runBare()
     */
    public void runBare() throws Throwable {
	    super.runBare();
	    // Unget services
	    for (int i = 0; i < references.size(); i++) {
	        context.ungetService((ServiceReference) references.get(i));
	    }
	    references.clear();
	}

	public void setBundleContext(BundleContext bc) {
		context = bc;
	}
	
	public BundleContext getBundleContext() {
	    return context;
	}

	public static void assertContains(String message, String[] array, String txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(txt)) {
				return;
			}
		}
		fail(formatContainsMessage(message, array, txt));
	}

	public static void assertContains(String message, byte[] array, int txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == txt) {
				return;
			}
		}
		Byte[] bytes = new Byte[array.length];
		for (int i = 0; i < array.length; i++) {
			bytes[i] = new Byte(array[i]);
		}
		fail(formatContainsMessage(message, bytes, new Integer(txt)));
	}

	public static void assertContains(String message, short[] array, int txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == txt) {
				return;
			}
		}
		Short[] bytes = new Short[array.length];
		for (int i = 0; i < array.length; i++) {
			bytes[i] = new Short(array[i]);
		}
		fail(formatContainsMessage(message, bytes, new Integer(txt)));
	}

	public static void assertContains(String message, int[] array, int txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == txt) {
				return;
			}
		}
		Integer[] bytes = new Integer[array.length];
		for (int i = 0; i < array.length; i++) {
			bytes[i] = new Integer(array[i]);
		}
		fail(formatContainsMessage(message, bytes, new Integer(txt)));
	}

	public static void assertContains(String message, long[] array, long txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == txt) {
				return;
			}
		}
		Long[] bytes = new Long[array.length];
		for (int i = 0; i < array.length; i++) {
			bytes[i] = new Long(array[i]);
		}
		fail(formatContainsMessage(message, bytes, new Long(txt)));
	}

	public static void assertContains(String message, float[] array, float txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == txt) {
				return;
			}
		}
		Float[] bytes = new Float[array.length];
		for (int i = 0; i < array.length; i++) {
			bytes[i] = new Float(array[i]);
		}
		fail(formatContainsMessage(message, bytes, new Float(txt)));
	}

	public static void assertContains(String message, double[] array, double txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == txt) {
				return;
			}
		}
		Double[] bytes = new Double[array.length];
		for (int i = 0; i < array.length; i++) {
			bytes[i] = new Double(array[i]);
		}
		fail(formatContainsMessage(message, bytes, new Double(txt)));
	}

	public static void assertContains(String message, char[] array, char txt) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == txt) {
				return;
			}
		}
		Character[] bytes = new Character[array.length];
		for (int i = 0; i < array.length; i++) {
			bytes[i] = new Character(array[i]);
		}
		fail(formatContainsMessage(message, bytes, new Character(txt)));
	}

	/**
	 * Asserts that two doubles are equal. If they are not an
	 * AssertionFailedError is thrown with the given message.
	 */
	public static void assertEquals(String message, double expected,
			double actual) {
		if (expected != actual) {
			fail(formatEqualsMessage(message, new Double(expected), new Double(
					actual)));
		}
	}

	public static void assertNotEquals(String message, Object o1, Object o2) {
		if (o1.equals(o2)) {
			fail(formatNotEqualsMessage(message, o1, o2));
		}
	}

	public static boolean contains(String string, String[] array) {
		for (int i = 0; array != null && i < array.length; i++) {
			if (array[i] != null && array[i].equals(string)) {
				return true;
			}
		}
		return false;
	}

	public static boolean contains(int value, int[] array) {
		for (int i = 0; array != null && i < array.length; i++) {
			if (array[i] == value) {
				return true;
			}
		}
		return false;
	}

	private static String formatEqualsMessage(String message, Object expected,
			Object actual) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}
		return formatted + "expected:<" + expected + "> but was:<" + actual
				+ ">";
	}

	private static String formatNotEqualsMessage(String message, Object o1,
			Object o2) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}
		return formatted + "o1:<" + o1 + "> is equals to o2:<" + o2 + ">";
	}

	private static String formatContainsMessage(String message, Object[] array,
			Object txt) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}

		String arr = null;
		for (int i = 0; i < array.length; i++) {
			if (arr == null) {
				arr = "[" + array[i];
			} else {
				arr += "," + array[i];
			}
		}
		arr += "]";

		return formatted + "array:" + arr + " does not contains:<" + txt + ">";
	}

	

	/**
	 * Returns the service object of a service provided by the specified bundle,
	 * offering the specified interface and matching the given filter.
	 * 
	 * @param bundle
	 *            the bundle in which the service is searched.
	 * @param itf
	 *            the interface provided by the searched service.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return the service object provided by the specified bundle, offering the
	 *         specified interface and matching the given filter.
	 */
	public static Object getServiceObject(Bundle bundle, String itf,
			String filter) {
		ServiceReference ref = getServiceReference(bundle, itf, filter);
		if (ref != null) {
			return bundle.getBundleContext().getService(ref);
		} else {
			return null;
		}
	}


	/**
	 * Returns the service objects of the services provided by the specified
	 * bundle, offering the specified interface and matching the given filter.
	 * 
	 * @param bundle
	 *            the bundle in which services are searched.
	 * @param itf
	 *            the interface provided by the searched services.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return the service objects provided by the specified bundle, offering
	 *         the specified interface and matching the given filter.
	 */
	public static Object[] getServiceObjects(Bundle bundle, String itf,
			String filter) {
		ServiceReference[] refs = getServiceReferences(bundle, itf, filter);
		if (refs != null) {
			Object[] list = new Object[refs.length];
			for (int i = 0; i < refs.length; i++) {
				list[i] = bundle.getBundleContext().getService(refs[i]);
			}
			return list;
		} else {
			return new Object[0];
		}
	}



	/**
	 * Returns the service reference of a service provided by the specified
	 * bundle, offering the specified interface and matching the given filter.
	 * 
	 * @param bundle
	 *            the bundle in which the service is searched.
	 * @param itf
	 *            the interface provided by the searched service.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return a service reference provided by the specified bundle, offering
	 *         the specified interface and matching the given filter. If no
	 *         service is found, {@code null} is returned.
	 */
	public static ServiceReference getServiceReference(Bundle bundle,
			String itf, String filter) {
		ServiceReference[] refs = getServiceReferences(bundle, itf, filter);
		if (refs.length != 0) {
			return refs[0];
		} else {
			// No service found
			return null;
		}
	}
	
	/**
	 * Checks if the service is available.
	 * @param itf the service interface
	 * @return <code>true</code> if the service is available,
	 * <code>false</code> otherwise.
	 */
	public boolean isServiceAvailable(String itf) {
        ServiceReference ref = getServiceReference(itf, null);
        return ref != null;
    }
    
    /**
     * Checks if the service is available.
     * @param itf the service interface
     * @param pid the service pid
     * @return <code>true</code> if the service is available,
     * <code>false</code> otherwise.
     */
    public boolean isServiceAvailableByPID(String itf, String pid) {
        ServiceReference ref = getServiceReferenceByPID(itf, pid);
        return ref != null;
    }

	

	/**
	 * Returns the service reference of the service provided by the specified
	 * bundle, offering the specified interface and having the given persistent
	 * ID.
	 * 
	 * @param bundle
	 *            the bundle in which the service is searched.
	 * @param itf
	 *            the interface provided by the searched service.
	 * @param pid
	 *            the persistent ID of the searched service.
	 * @return a service provided by the specified bundle, offering the
	 *         specified interface and having the given persistent ID.
	 */
	public static ServiceReference getServiceReferenceByPID(Bundle bundle,
			String itf, String pid) {
		String filter = "(" + "service.pid" + "=" + pid + ")";
		ServiceReference[] refs = getServiceReferences(bundle, itf, filter);
		if (refs == null) {
			return null;
		} else if (refs.length == 1) {
			return refs[0];
		} else {
			throw new IllegalStateException(
					"A service lookup by PID returned several providers ("
							+ refs.length + ")" + " for " + itf + " with pid="
							+ pid);
		}
	}

	

	/**
	 * Returns the service reference of all the services provided in the
	 * specified bundle, offering the specified interface and matching the given
	 * filter.
	 * 
	 * @param bundle
	 *            the bundle in which services are searched.
	 * @param itf
	 *            the interface provided by the searched services.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return all the service references provided in the specified bundle,
	 *         offering the specified interface and matching the given filter.
	 *         If no service matches, an empty array is returned.
	 */
	public static ServiceReference[] getServiceReferences(Bundle bundle,
			String itf, String filter) {
		ServiceReference[] refs = null;
		try {
			// Get all the service references
			refs = bundle.getBundleContext().getServiceReferences(itf, filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(
					"Cannot get service references: " + e.getMessage());
		}
		if (refs == null) {
			return new ServiceReference[0];
		} else {
			return refs;
		}
	}

	
	
	/**
	 * Returns the service object of a service provided by the local bundle,
	 * offering the specified interface and matching the given filter.
	 * 
	 * @param itf
	 *            the interface provided by the searched service.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return the service object provided by the local bundle, offering the
	 *         specified interface and matching the given filter.
	 */
	public Object getServiceObject(String itf, String filter) {
		ServiceReference ref = getServiceReference(itf, filter);
        if (ref != null) {
            references.add(ref);
            return context.getService(ref);
        } else {
            return null;
        }
	}
	
	   
	/**
     * Returns the service object associated with this service
     * reference.
     * 
     * @param ref
     *            service reference
     * @return the service object.
     */
    public Object getServiceObject(ServiceReference ref) {
        if (ref != null) {
            references.add(ref);
            return context.getService(ref);
        } else {
            return null;
        }
    }

	/**
	 * Returns the service objects of the services provided by the local
	 * bundle, offering the specified interface and matching the given filter.
	 * 
	 * @param itf
	 *            the interface provided by the searched services.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return the service objects provided by the local bundle, offering
	 *         the specified interface and matching the given filter.
	 */
	public Object[] getServiceObjects(String itf, String filter) {
	    ServiceReference[] refs = getServiceReferences(itf, filter);
        if (refs != null) {
            Object[] list = new Object[refs.length];
            for (int i = 0; i < refs.length; i++) {
                references.add(refs[i]);
                list[i] = context.getService(refs[i]);
            }
            return list;
        } else {
            return new Object[0];
        }
    }

	/**
	 * Returns the service reference of a service provided by the local
	 * bundle, offering the specified interface and matching the given filter.
	 * 
	 * @param itf
	 *            the interface provided by the searched service.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return a service reference provided by the local bundle, offering
	 *         the specified interface and matching the given filter. If no
	 *         service is found, {@code null} is returned.
	 */
	public ServiceReference getServiceReference(String itf, String filter) {
		return getServiceReference(context.getBundle(), itf, filter);
	}
	
	/**
     * Returns the service reference of a service provided 
     * offering the specified interface.
     * 
     * @param itf
     *            the interface provided by the searched service.
     * @return a service reference provided by the local bundle, offering
     *         the specified interface and matching the given filter. If no
     *         service is found, {@code null} is returned.
     */
    public ServiceReference getServiceReference(String itf) {
        return getServiceReference(context.getBundle(), itf, null);
    }

	/**
	 * Returns the service reference of the service provided by the local
	 * bundle, offering the specified interface and having the given persistent
	 * ID.
	 * 
	 * @param itf
	 *            the interface provided by the searched service.
	 * @param pid
	 *            the persistent ID of the searched service.
	 * @return a service provided by the local bundle, offering the
	 *         specified interface and having the given persistent ID.
	 */
	public ServiceReference getServiceReferenceByPID(String itf, String pid) {
		return getServiceReferenceByPID(context.getBundle(), itf, pid);
	}

	/**
	 * Returns the service reference of all the services provided in the
	 * local bundle, offering the specified interface and matching the given
	 * filter.
	 * 
	 * @param itf
	 *            the interface provided by the searched services.
	 * @param filter
	 *            an additional filter (can be {@code null}).
	 * @return all the service references provided in the local bundle,
	 *         offering the specified interface and matching the given filter.
	 *         If no service matches, an empty array is returned.
	 */
	public ServiceReference[] getServiceReferences(String itf, String filter) {
		return getServiceReferences(context.getBundle(), itf, filter);
	}
	
	

}
