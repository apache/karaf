/*
 * $Header: /cvshome/build/ee.foundation/src/java/lang/ThreadGroup.java,v 1.6 2006/03/14 01:20:24 hargrave Exp $
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
public class ThreadGroup {
	public ThreadGroup(java.lang.String var0) { }
	public ThreadGroup(java.lang.ThreadGroup var0, java.lang.String var1) { }
	public int activeCount() { return 0; }
	public int activeGroupCount() { return 0; }
	public final void checkAccess() { }
	public final void destroy() { }
	public int enumerate(java.lang.Thread[] var0) { return 0; }
	public int enumerate(java.lang.Thread[] var0, boolean var1) { return 0; }
	public int enumerate(java.lang.ThreadGroup[] var0) { return 0; }
	public int enumerate(java.lang.ThreadGroup[] var0, boolean var1) { return 0; }
	public final int getMaxPriority() { return 0; }
	public final java.lang.String getName() { return null; }
	public final java.lang.ThreadGroup getParent() { return null; }
	public final void interrupt() { }
	public final boolean isDaemon() { return false; }
	public boolean isDestroyed() { return false; }
	public void list() { }
	public final boolean parentOf(java.lang.ThreadGroup var0) { return false; }
	public final void setDaemon(boolean var0) { }
	public final void setMaxPriority(int var0) { }
	public java.lang.String toString() { return null; }
	public void uncaughtException(java.lang.Thread var0, java.lang.Throwable var1) { }
}

