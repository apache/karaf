/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/Identity.java,v 1.6 2006/03/14 01:20:27 hargrave Exp $
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

package java.security;
public abstract class Identity implements java.security.Principal, java.io.Serializable {
	protected Identity() { }
	public Identity(java.lang.String var0) { }
	public Identity(java.lang.String var0, java.security.IdentityScope var1) throws java.security.KeyManagementException { }
	public final java.security.IdentityScope getScope() { return null; }
	public java.security.PublicKey getPublicKey() { return null; }
	public void setPublicKey(java.security.PublicKey var0) throws java.security.KeyManagementException { }
	public final java.lang.String getName() { return null; }
	public java.lang.String getInfo() { return null; }
	public void setInfo(java.lang.String var0) { }
	public java.security.Certificate[] certificates() { return null; }
	public void addCertificate(java.security.Certificate var0) throws java.security.KeyManagementException { }
	public void removeCertificate(java.security.Certificate var0) throws java.security.KeyManagementException { }
	public final boolean equals(java.lang.Object var0) { return false; }
	protected boolean identityEquals(java.security.Identity var0) { return false; }
	public java.lang.String toString() { return null; }
	public java.lang.String toString(boolean var0) { return null; }
	public int hashCode() { return 0; }
}

