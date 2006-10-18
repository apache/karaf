/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/Observable.java,v 1.6 2006/03/14 01:20:25 hargrave Exp $
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

package java.util;
public class Observable {
	public Observable() { }
	public void addObserver(java.util.Observer var0) { }
	protected void clearChanged() { }
	public int countObservers() { return 0; }
	public void deleteObserver(java.util.Observer var0) { }
	public void deleteObservers() { }
	public boolean hasChanged() { return false; }
	public void notifyObservers() { }
	public void notifyObservers(java.lang.Object var0) { }
	protected void setChanged() { }
}

