/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/IdentityScope.java,v 1.6 2006/03/14 01:20:26 hargrave Exp $
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
public abstract class IdentityScope extends java.security.Identity {
	protected IdentityScope() { }
	public IdentityScope(java.lang.String var0) { }
	public IdentityScope(java.lang.String var0, java.security.IdentityScope var1) throws java.security.KeyManagementException { }
	public abstract void addIdentity(java.security.Identity var0) throws java.security.KeyManagementException;
	public abstract void removeIdentity(java.security.Identity var0) throws java.security.KeyManagementException;
	public abstract java.util.Enumeration identities();
	public java.security.Identity getIdentity(java.security.Principal var0) { return null; }
	public abstract java.security.Identity getIdentity(java.security.PublicKey var0);
	public abstract java.security.Identity getIdentity(java.lang.String var0);
	protected static void setSystemScope(java.security.IdentityScope var0) { }
	public abstract int size();
	public java.lang.String toString() { return null; }
	public static java.security.IdentityScope getSystemScope() { return null; }
}

