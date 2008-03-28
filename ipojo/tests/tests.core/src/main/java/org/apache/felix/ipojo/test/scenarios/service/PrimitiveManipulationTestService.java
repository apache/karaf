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
package org.apache.felix.ipojo.test.scenarios.service;

public interface PrimitiveManipulationTestService {
	
	byte getByte();
	void setByte(byte b);
	
	short getShort();
	void setShort(short s);
	
	int getInt();
	void setInt(int i);
	
	long getLong();
	void setLong(long l);
	
	float getFloat();
	void setFloat(float f);
	
	double getDouble();
	void setDouble(double d);
	
	char getChar();
	void setChar(char c);
	
	boolean getBoolean();
	void setBoolean(boolean b);
	
	// Array types
	byte[] getBytes();
	void setBytes(byte[] bs);
	
	short[] getShorts();
	void setShorts(short[] ss);
	
	int[] getInts();
	void setInts(int is[]);
	
	long[] getLongs();
	void setLongs(long[] ls);
	
	float[] getFloats();
	void setFloats(float[] fs);
	
	double[] getDoubles();
	void setDoubles(double[] ds);
	
	char[] getChars();
	void setChars(char[] cs);
	
	boolean[] getBooleans();
	void setBooleans(boolean[] bs);	
	
	// This method has been added to test an issue when autoboxing.
	void setLong(long l, String s);

}
