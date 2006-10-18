/*
 * $Header: /cvshome/build/ee.foundation/src/java/lang/SecurityManager.java,v 1.6 2006/03/14 01:20:24 hargrave Exp $
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

package java.lang;
public class SecurityManager {
	public SecurityManager() { }
	public void checkAccept(java.lang.String var0, int var1) { }
	public void checkAccess(java.lang.Thread var0) { }
	public void checkAccess(java.lang.ThreadGroup var0) { }
	public void checkConnect(java.lang.String var0, int var1) { }
	public void checkConnect(java.lang.String var0, int var1, java.lang.Object var2) { }
	public void checkCreateClassLoader() { }
	public void checkDelete(java.lang.String var0) { }
	public void checkExec(java.lang.String var0) { }
	public void checkExit(int var0) { }
	public void checkLink(java.lang.String var0) { }
	public void checkListen(int var0) { }
	public void checkMemberAccess(java.lang.Class var0, int var1) { }
	public void checkMulticast(java.net.InetAddress var0) { }
	public void checkMulticast(java.net.InetAddress var0, byte var1) { }
	public void checkPackageAccess(java.lang.String var0) { }
	public void checkPackageDefinition(java.lang.String var0) { }
	public void checkPropertiesAccess() { }
	public void checkPropertyAccess(java.lang.String var0) { }
	public void checkRead(java.io.FileDescriptor var0) { }
	public void checkRead(java.lang.String var0) { }
	public void checkRead(java.lang.String var0, java.lang.Object var1) { }
	public void checkSecurityAccess(java.lang.String var0) { }
	public void checkSetFactory() { }
	public boolean checkTopLevelWindow(java.lang.Object var0) { return false; }
	public void checkSystemClipboardAccess() { }
	public void checkAwtEventQueueAccess() { }
	public void checkPrintJobAccess() { }
	public void checkWrite(java.io.FileDescriptor var0) { }
	public void checkWrite(java.lang.String var0) { }
	protected java.lang.Class[] getClassContext() { return null; }
	public java.lang.ThreadGroup getThreadGroup() { return null; }
	public java.lang.Object getSecurityContext() { return null; }
	public void checkPermission(java.security.Permission var0) { }
	public void checkPermission(java.security.Permission var0, java.lang.Object var1) { }
}

