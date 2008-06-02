/*
 * $Header: /cvshome/build/org.osgi.service.application/src/org/osgi/service/application/ApplicationException.java,v 1.10 2006/07/10 11:49:12 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2005, 2006). All Rights Reserved.
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

/**
 * This exception is used to indicate problems related to application 
 * lifecycle management.
 * 
 * <code>ApplicationException</code> object is created by the Application Admin to denote
 * an exception condition in the lifecycle of an application.
 * <code>ApplicationException</code>s should not be created by developers.
 * <br/>
 * <code>ApplicationException</code>s are associated with an error code. This code
 * describes the type of problem reported in this exception. The possible codes are:
 * <ul>
 * <li> {@link #APPLICATION_LOCKED} - The application couldn't be launched because it is locked.</li>
 * <li> {@link #APPLICATION_NOT_LAUNCHABLE} - The application is not in launchable state.</li>
 * <li> {@link #APPLICATION_INTERNAL_ERROR} - An exception was thrown by the application or its
 *       container during launch.</li>
 * <li> {@link #APPLICATION_SCHEDULING_FAILED} - The scheduling of an application
 *       failed.
 * </ul>
 * 
 */
public class ApplicationException extends Exception {
	private static final long serialVersionUID = -7173190453622508207L;
	private final Throwable cause;
	private final int errorCode;
	
	/**
	 * The application couldn't be launched because it is locked.
	 */
	public static final int APPLICATION_LOCKED	= 0x01;
	
	/**
	 * The application is not in launchable state, it's 
	 * {@link ApplicationDescriptor#APPLICATION_LAUNCHABLE}
	 * attribute is false.
	 */
	public static final int APPLICATION_NOT_LAUNCHABLE = 0x02;
	
	/**
	 * An exception was thrown by the application or the corresponding
	 * container during launch. The exception is available in {@link #getCause()}.
	 */
	public static final int APPLICATION_INTERNAL_ERROR = 0x03;
    
    /**
     * The application schedule could not be created due to some internal error
     * (for example, the schedule information couldn't be saved).
     */
    public static final int APPLICATION_SCHEDULING_FAILED = 0x04;
    
    /**
     * The application scheduling failed because the specified identifier
     * is already in use.
     */
    public static final int APPLICATION_DUPLICATE_SCHEDULE_ID = 0x05;

	/**
	 * Creates an <code>ApplicationException</code> with the specified error code.
	 * @param errorCode The code of the error 
	 */
	public ApplicationException(int errorCode) {
		this(errorCode,(Throwable) null);
	}
	
	/**
	 * Creates a <code>ApplicationException</code> that wraps another exception.
	 * 
	 * @param errorCode The code of the error 
	 * @param cause The cause of this exception.
	 */
	public ApplicationException(int errorCode, Throwable cause) {
		super();
		this.cause = cause;
		this.errorCode = errorCode;
	}

	/**
	 * Creates an <code>ApplicationException</code> with the specified error code.
	 * @param errorCode The code of the error 
	 * @param message The associated message
	 */
	public ApplicationException(int errorCode, String message) {
		this(errorCode, message,null);
	}

	/**
	 * Creates a <code>ApplicationException</code> that wraps another exception.
	 * 
	 * @param errorCode The code of the error 
	 * @param message The associated message.
	 * @param cause The cause of this exception.
	 */
	public ApplicationException(int errorCode, String message, Throwable cause) {
		super(message);
		this.cause = cause;
		this.errorCode = errorCode;
	}

	/**
	 * Returns the cause of this exception or <code>null</code> if no cause
	 * was specified when this exception was created.
	 * 
	 * @return The cause of this exception or <code>null</code> if no cause
	 *         was specified.
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns the error code associcated with this exception.
	 * @return The error code of this exception.
	 */
	public int getErrorCode() {
		return errorCode;
	}
}
