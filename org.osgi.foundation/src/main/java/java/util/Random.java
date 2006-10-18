/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/Random.java,v 1.6 2006/03/14 01:20:25 hargrave Exp $
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
public class Random implements java.io.Serializable {
	public Random() { }
	public Random(long var0) { }
	protected int next(int var0) { return 0; }
	public boolean nextBoolean() { return false; }
	public void nextBytes(byte[] var0) { }
	public double nextDouble() { return 0.0d; }
	public float nextFloat() { return 0.0f; }
	public double nextGaussian() { return 0.0d; }
	public int nextInt() { return 0; }
	public int nextInt(int var0) { return 0; }
	public long nextLong() { return 0l; }
	public void setSeed(long var0) { }
}

