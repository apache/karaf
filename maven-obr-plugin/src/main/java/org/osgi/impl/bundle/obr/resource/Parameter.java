/*
 * $Header: /cvshome/bundles/bundles.obr/src/bundles/obr/resource/Parameter.java,v 1.1 2006/07/27 10:31:02 pkriens Exp $
 * 
 * Copyright (c) The OSGi Alliance (2005). All Rights Reserved.
 * 
 * Implementation of certain elements of the OSGi Specification may be subject
 * to third party intellectual property rights, including without limitation,
 * patent rights (such a third party may or may not be a member of the OSGi
 * Alliance). The OSGi Alliance is not responsible and shall not be held
 * responsible in any manner for identifying or failing to identify any or all
 * such third party intellectual property rights.
 * 
 * This document and the information contained herein are provided on an "AS IS"
 * basis and THE OSGI ALLIANCE DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION
 * HEREIN WILL NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL THE
 * OSGI ALLIANCE BE LIABLE FOR ANY LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF
 * USE OF DATA, INTERRUPTION OF BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR
 * EXEMPLARY, INCIDENTIAL, PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN
 * CONNECTION WITH THIS DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH LOSS OR DAMAGE.
 * 
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.impl.bundle.obr.resource;

class Parameter {
	final static int	ATTRIBUTE	= 1;
	final static int	DIRECTIVE	= 2;
	final static int	SINGLE		= 0;

	int					type;
	String				key;
	String				value;

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(key);
		switch (type) {
			case ATTRIBUTE :
				sb.append("=");
				break;
			case DIRECTIVE :
				sb.append(":=");
				break;
			case SINGLE :
				return sb.toString();
		}
		sb.append(value);
		return sb.toString();
	}

	boolean is(String s, int type) {
		return this.type == type && key.equalsIgnoreCase(s);
	}
}
