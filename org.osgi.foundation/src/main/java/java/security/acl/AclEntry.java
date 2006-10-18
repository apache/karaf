/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/acl/AclEntry.java,v 1.6 2006/03/14 01:20:29 hargrave Exp $
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
public abstract interface AclEntry extends java.lang.Cloneable {
	public abstract boolean addPermission(java.security.acl.Permission var0);
	public abstract boolean checkPermission(java.security.acl.Permission var0);
	public abstract java.lang.Object clone();
	public abstract java.security.Principal getPrincipal();
	public abstract boolean isNegative();
	public abstract java.util.Enumeration permissions();
	public abstract boolean removePermission(java.security.acl.Permission var0);
	public abstract void setNegativePermissions();
	public abstract boolean setPrincipal(java.security.Principal var0);
	public abstract java.lang.String toString();
}

