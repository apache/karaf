/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
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

package org.osgi.service.application;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import org.osgi.framework.Constants;

/**
 * ApplicationHandle is an OSGi service interface which represents an instance
 * of an application. It provides the functionality to query and manipulate the
 * lifecycle state of the represented application instance. It defines constants
 * for the lifecycle states.
 * 
 * @version $Revision: 5901 $
 */
public abstract class ApplicationHandle {
	/*
	 * NOTE: An implementor may also choose to replace this class in
	 * their distribution with a class that directly interfaces with the
	 * org.osgi.service.application implementation. This replacement class MUST NOT alter the
	 * public/protected signature of this class.
	 */

	/**
	 * The property key for the unique identifier (PID) of the application
	 * instance.
	 */
	public static final String APPLICATION_PID = Constants.SERVICE_PID;
	
	/**
	 * The property key for the pid of the corresponding application descriptor.
	 */
	public final static String APPLICATION_DESCRIPTOR	= "application.descriptor";
	
	/**
	 * The property key for the state of this application instance.
	 */
	public final static String APPLICATION_STATE		= "application.state";

	/**
	 * The property key for the supports exit value property of this application
	 * instance.
	 * 
	 * @since 1.1
	 */
	public final static String APPLICATION_SUPPORTS_EXITVALUE = "application.supports.exitvalue";

	/**
	 * The application instance is running. This is the initial state of a newly
	 * created application instance.
	 */
	public final static String RUNNING = "RUNNING";
	
  /**
   * The application instance is being stopped. This is the state of the
   * application instance during the execution of the <code>destroy()</code>
   * method.
   */
	public final static String STOPPING = "STOPPING";

	private final String instanceId;
	
	private final ApplicationDescriptor	descriptor;

	/**
	 * Application instance identifier is specified by the container when the
	 * instance is created. The instance identifier must remain static for the 
	 * lifetime of the instance, it must remain the same even across framework
	 * restarts for the same application instance. This value must be the same
	 * as the <code>service.pid</code> service property of this application
	 * handle.
	 * <p>
	 * The instance identifier should follow the following scheme: 
	 * &lt;<i>application descriptor PID</i>&gt;.&lt;<i>index</i>&gt;
	 * where &lt;<i>application descriptor PID</i>&gt; is the PID of the 
	 * corresponding <code>ApplicationDescriptor</code> and &lt;<i>index</i>&gt;
	 * is a unique integer index assigned by the application container. 
	 * Even after destroying the application index the same index value should not
	 * be reused in a reasonably long timeframe.
	 * 
	 * @param instanceId the instance identifier of the represented application
	 * instance. It must not be null.
	 * 
	 * @param descriptor the <code>ApplicationDescriptor</code> of the represented
	 * application instance. It must not be null.
	 * 
	 * @throws NullPointerException if any of the arguments is null.
	 */
	protected ApplicationHandle(String instanceId, ApplicationDescriptor descriptor ) {
		if( (null == instanceId) || (null == descriptor) ) {
			throw new NullPointerException("Parameters must not be null!");
		}
		
		this.instanceId	= instanceId;
		this.descriptor = descriptor;

		try {
			delegate = new Delegate();
			delegate.setApplicationHandle( this, descriptor.delegate );
		}
		catch (Exception e) {
			// Too bad ...
			e.printStackTrace();
			System.err
					.println("No implementation available for ApplicationDescriptor, property is: "
							+ Delegate.cName);
		}
	}

	/**
	 * Retrieves the <code>ApplicationDescriptor</code> to which this 
	 * <code>ApplicationHandle</code> belongs. 
	 * 
	 * @return The corresponding <code>ApplicationDescriptor</code>
	 */
	public final ApplicationDescriptor getApplicationDescriptor() {
		return descriptor;
	}

	/**
	 * Get the state of the application instance.
	 * 
	 * @return the state of the application.
	 * 
	 * @throws IllegalStateException
	 *             if the application handle is unregistered
	 */
	public abstract String getState();

	/**
	 * Returns the exit value for the application instance. The timeout
	 * specifies how the method behaves when the application has not yet
	 * terminated. A negative, zero or positive value may be used.
	 * <ul>
	 * <li> negative - The method does not wait for termination. If the
	 * application has not terminated then an <code>ApplicationException</code>
	 * is thrown.</li>
	 * 
	 * <li> zero - The method waits until the application terminates.</li>
	 * 
	 * <li> positive - The method waits until the application terminates or the
	 * timeout expires. If the timeout expires and the application has not
	 * terminated then an <code>ApplicationException</code> is thrown.</li>
	 * </ul>
	 * <p>
	 * The default implementation throws an
	 * <code>UnsupportedOperationException</code>. The application model should
	 * override this method if exit values are supported.
	 * </p>
	 * 
	 * @param timeout The maximum time in milliseconds to wait for the
	 *        application to timeout.
	 * @return The exit value for the application instance. The value is
	 *         application specific.
	 * @throws UnsupportedOperationException If the application model does not
	 *         support exit values.
	 * @throws InterruptedException If the thread is interrupted while waiting
	 *         for the timeout.
	 * @throws ApplicationException If the application has not terminated. The
	 *         error code will be
	 *         {@link ApplicationException#APPLICATION_EXITVALUE_NOT_AVAILABLE}.
	 * 
	 * @since 1.1
	 */
	public Object getExitValue(long timeout) throws ApplicationException, InterruptedException{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the unique identifier of this instance. This value is also
	 * available as a service property of this application handle's service.pid.
	 * 
	 * @return the unique identifier of the instance
	 */
	public final String getInstanceId() {
		return instanceId;
	}

	/**
	 * The application instance's lifecycle state can be influenced by this
	 * method. It lets the application instance perform operations to stop
	 * the application safely, e.g. saving its state to a permanent storage.
	 * <p>
	 * The method must check if the lifecycle transition is valid; a STOPPING
	 * application cannot be stopped. If it is invalid then the method must
	 * exit. Otherwise the lifecycle state of the application instance must be
	 * set to STOPPING. Then the destroySpecific() method must be called to
	 * perform any application model specific steps for safe stopping of the
	 * represented application instance.
	 * <p>
	 * At the end the <code>ApplicationHandle</code> must be unregistered. 
	 * This method should  free all the resources related to this 
	 * <code>ApplicationHandle</code>.
	 * <p>
	 * When this method is completed the application instance has already made
	 * its operations for safe stopping, the ApplicationHandle has been
	 * unregistered and its related resources has been freed. Further calls on
	 * this application should not be made because they may have unexpected
	 * results.
	 * 
	 * @throws SecurityException
	 *             if the caller doesn't have "lifecycle"
	 *             <code>ApplicationAdminPermission</code> for the corresponding application.
	 * 
	 * @throws IllegalStateException
	 *             if the application handle is unregistered
	 */
	public final void destroy() {
		try {
			delegate.destroy();
		}catch( SecurityException se ) {
			descriptor.isLaunchableSpecific(); /* check whether the bundle was uninstalled */
			                                   /* if yes, throws IllegalStateException */
			throw se; /* otherwise throw the caught SecurityException */
		}
		destroySpecific();
	}

	/**
	 * Called by the destroy() method to perform application model specific
	 * steps to stop and destroy an application instance safely.
	 * 
	 * @throws IllegalStateException
	 *             if the application handle is unregistered
	 */
	protected abstract void destroySpecific();
	
	Delegate	delegate;
	

	/**
	 * This class will load the class named
	 * by the org.osgi.vendor.application.ApplicationHandle and delegate
	 * method calls to an instance of the class.
	 */
	static class Delegate {
		static String cName;
		static Class implementation;
		static Method setApplicationHandle;
		static Method destroy;

		static {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run(){			
					cName = System.getProperty("org.osgi.vendor.application.ApplicationHandle");
					if (cName == null) {
						throw new NoClassDefFoundError("org.osgi.vendor.application.ApplicationHandle property must be set"); 
					}
					
					try {
						implementation = Class.forName(cName);
					}
					catch (ClassNotFoundException e) {
						throw new NoClassDefFoundError(e.toString());
					}
					
					try {
						setApplicationHandle = implementation.getMethod("setApplicationHandle",
								new Class[] {ApplicationHandle.class, Object.class});
						destroy = implementation.getMethod("destroy",
								new Class[] {});
					}
					catch (NoSuchMethodException e) {
						throw new NoSuchMethodError(e.toString());
					}
					
					return null;
				}
			});
		}
		
		Object target; 
		
		Delegate() throws Exception {
			target = AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {			
					return implementation.newInstance();
				}
			});
		}

		void setApplicationHandle(ApplicationHandle d, ApplicationDescriptor.Delegate descriptor ) {
			try {
				try {
					setApplicationHandle.invoke(target, new Object[] {d, descriptor.target});
				}
				catch (InvocationTargetException e) {
					throw e.getTargetException();
				}
			}
			catch (Error e) {
				throw e;
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		void destroy() {
			try {
				try {
					destroy.invoke(target, new Object[] {});
				}
				catch (InvocationTargetException e) {
					throw e.getTargetException();
				}
			}
			catch (Error e) {
				throw e;
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
}
