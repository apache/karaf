/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.upnp.extra.util;

/**
 * @author Stefano "Kismet" Lenzi
 * @author Francesco Furfari
 * 
 */
public class UPnPException extends Exception {

	private int errorCode;

	public UPnPException(int errorCode, String errorDescription,
			Throwable t) {
		super(errorDescription, t);
		this.errorCode = errorCode;
	}

	public UPnPException(int errorCode, String errorDescription) {
		super(errorDescription);
		this.errorCode = errorCode;
	}

	public String getErrorDescription() {
		return getMessage();
	}

	public int getErrorCode() {
		return errorCode;
	}

}
