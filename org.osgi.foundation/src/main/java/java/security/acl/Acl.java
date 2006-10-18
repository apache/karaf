/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/acl/Acl.java,v 1.6 2006/03/14 01:20:29 hargrave Exp $
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

package java.security.acl;
public abstract interface Acl extends java.security.acl.Owner {
	public abstract boolean addEntry(java.security.Principal var0, java.security.acl.AclEntry var1) throws java.security.acl.NotOwnerException;
	public abstract boolean checkPermission(java.security.Principal var0, java.security.acl.Permission var1);
	public abstract java.util.Enumeration entries();
	public abstract java.lang.String getName();
	public abstract java.util.Enumeration getPermissions(java.security.Principal var0);
	public abstract boolean removeEntry(java.security.Principal var0, java.security.acl.AclEntry var1) throws java.security.acl.NotOwnerException;
	public abstract void setName(java.security.Principal var0, java.lang.String var1) throws java.security.acl.NotOwnerException;
	public abstract java.lang.String toString();
}

