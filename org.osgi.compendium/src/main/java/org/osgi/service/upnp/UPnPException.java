/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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
package org.osgi.service.upnp;

/**
 * There are several defined error situations describing UPnP problems while a
 * control point invokes actions to UPnPDevices.
 * 
 * @since 1.1
 * @version $Revision: 5673 $
 */
public class UPnPException extends Exception {
	static final long		serialVersionUID		= -262013318122195146L;

	/**
	 * No Action found by that name at this service.
	 */
	public final static int	INVALID_ACTION			= 401;

	/**
	 * Not enough arguments, too many arguments with a specific name, or one of
	 * more of the arguments are of the wrong type.
	 */
	public final static int	INVALID_ARGS			= 402;

	/**
	 * The different end-points are no longer in synchronization.
	 */
	public final static int	INVALID_SEQUENCE_NUMBER	= 403;

	/**
	 * Refers to a non existing variable.
	 */
	public final static int	INVALID_VARIABLE		= 404;

	/**
	 * The invoked action failed during execution.
	 */
	public final static int	DEVICE_INTERNAL_ERROR	= 501;

	/**
	 * Key for an error information that is an int type variable and that is
	 * used to identify occured errors.
	 */
	private final int				errorCode;

	/**
	 * This constructor creates a UPnPException on the specified error code and
	 * error description.
	 * 
	 * @param errorCode errorCode which defined UPnP Device Architecture V1.0.
	 * @param errordesc errorDescription which explain the type of propblem.
	 */
	public UPnPException(int errorCode, String errordesc) {
		super(errordesc);
		this.errorCode = errorCode;
	}

	/**
	 * Returns the UPnPError Code occured by UPnPDevices during invocation.
	 * 
	 * @return The UPnPErrorCode defined by a UPnP Forum working committee or
	 *         specified by a UPnP vendor.
	 */
	public int getUPnPError_Code() {
		return errorCode;
	}
}
