/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.test.scenarios.component.A123;

import org.apache.felix.ipojo.test.scenarios.manipulation.service.PrimitiveManipulationTestService;


public class Manipulation23Tester implements PrimitiveManipulationTestService {

    
    // Integer types
	byte b = 1;
	short s = 1;
	int i = 1;
	long l = 1;
	
	// Floatting types
	double d = 1.1;
	float f = 1.1f;
	
	// Character
	char c = 'a';
	
	// Boolean
	boolean bool = false;
	
	// Integer arrays 
	byte[] bs = new byte[] {0,1,2};
	short[] ss = new short[] {0,1,2};
	int[] is = new int[] {0,1,2};
	long[] ls = new long[] {0,1,2};
	
	double[] ds = new double[] {0.0, 1.1, 2.2};
	float[] fs = new float[] {0.0f, 1.1f, 2.2f};
	
	char[] cs = new char[] {'a', 'b', 'c'};
	
	boolean[] bools = new boolean[] {false, true, false};

	public boolean getBoolean() { return bool; }

	public boolean[] getBooleans() { return bools; }

	public byte getByte() { return b; }

	public byte[] getBytes() { return bs; }

	public char getChar() { return c; }

	public char[] getChars() { return cs; }

	public double getDouble() { return d; }

	public double[] getDoubles() { return ds; }

	public float getFloat() { return f; }

	public float[] getFloats() { return fs; }

	public int getInt() { return i; }

	public int[] getInts() { return is; }

	public long getLong() { return l; }

	public long[] getLongs() { return ls; }

	public short getShort() { return s; }

	public short[] getShorts() { return ss; }

	public void setBoolean(boolean b) { this.bool = b; }

	public void setBooleans(boolean[] bs) { this.bools = bs; }

	public void setByte(byte b) { this.b = b; }

	public void setBytes(byte[] bs) { this.bs = bs; }

	public void setChar(char c) { this.c = c; }

	public void setChars(char[] cs) { this.cs = cs; }

	public void setDouble(double d) { this.d = d; }

	public void setDoubles(double[] ds) { this.ds = ds; }

	public void setFloat(float f) { this.f = f; }

	public void setFloats(float[] fs) { this.fs = fs; }

	public void setInt(int i) { this.i = i; }

	public void setInts(int[] is) { this.is = is; }

	public void setLong(long l) { this.l = l; }

	public void setLongs(long[] ls) { this.ls = ls; }

 	public void setShort(short s) { this.s = s; }

	public void setShorts(short[] ss) { this.ss = ss; }	
	
	public void setLong(long l, String s) {
	    this.l = l;
	}

}
