/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/cert/X509Certificate.java,v 1.6 2006/03/14 01:20:29 hargrave Exp $
 *
 * (C) Copyright 2001 Sun Microsystems, Inc.
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
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

package java.security.cert;
public abstract class X509Certificate extends java.security.cert.Certificate implements java.security.cert.X509Extension {
	protected X509Certificate() { super((java.lang.String) null); }
	public abstract void checkValidity() throws java.security.cert.CertificateExpiredException, java.security.cert.CertificateNotYetValidException;
	public abstract void checkValidity(java.util.Date var0) throws java.security.cert.CertificateExpiredException, java.security.cert.CertificateNotYetValidException;
	public abstract int getBasicConstraints();
	public abstract java.security.Principal getIssuerDN();
	public abstract boolean[] getIssuerUniqueID();
	public abstract boolean[] getKeyUsage();
	public abstract java.util.Date getNotAfter();
	public abstract java.util.Date getNotBefore();
	public abstract java.math.BigInteger getSerialNumber();
	public abstract java.lang.String getSigAlgName();
	public abstract java.lang.String getSigAlgOID();
	public abstract byte[] getSigAlgParams();
	public abstract byte[] getSignature();
	public abstract java.security.Principal getSubjectDN();
	public abstract boolean[] getSubjectUniqueID();
	public abstract byte[] getTBSCertificate() throws java.security.cert.CertificateEncodingException;
	public abstract int getVersion();
	public abstract boolean hasUnsupportedCriticalExtension();
	public abstract java.util.Set getCriticalExtensionOIDs();
	public abstract java.util.Set getNonCriticalExtensionOIDs();
	public abstract byte[] getExtensionValue(java.lang.String var0);
}

