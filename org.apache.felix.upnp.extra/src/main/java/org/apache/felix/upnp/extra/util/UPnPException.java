/*
 DomoWare UPnP Base Driver is an implementation of the OSGi UnP Device Spcifaction
 Copyright (C) 2004  Matteo Demuru, Francesco Furfari, Stefano "Kismet" Lenzi

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 You can contact us at:
 {matte-d, sygent, kismet-sl} [at] users.sourceforge.net
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
