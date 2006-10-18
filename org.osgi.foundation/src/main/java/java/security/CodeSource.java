/*
 * $Header: /cvshome/build/ee.foundation/src/java/security/CodeSource.java,v 1.6 2006/03/14 01:20:27 hargrave Exp $
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
public class CodeSource implements java.io.Serializable {
	public CodeSource(java.net.URL var0, java.security.cert.Certificate[] var1) { }
	public boolean equals(java.lang.Object var0) { return false; }
	public int hashCode() { return 0; }
	public final java.security.cert.Certificate[] getCertificates() { return null; }
	public final java.net.URL getLocation() { return null; }
	public boolean implies(java.security.CodeSource var0) { return false; }
	public java.lang.String toString() { return null; }
}

