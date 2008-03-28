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
package org.apache.felix.ipojo.test.composite.service;

import java.util.Properties;

public interface Tata {
    
    public Properties getPropsTata();
    
    public void tata();
    
    public int tataInt(int i);
    public long tataLong(long l);
    public double tataDouble(double d);
    public char tataChar(char c);
    public boolean tataBoolean(boolean b);
    public short tataShort(short s);
    public float tataFloat(float f);
    public byte tataByte(byte b);
    
    public int[] tataInts(int[] its);
    public long[] tataLongs(long[] l);
    public double[] tataDoubles(double[] d);
    public char[] tataChars(char[] c);
    public boolean[] tataBooleans(boolean[] b);
    public short[] tataShorts(short[] s);
    public float[] tataFloats(float[] f);
    public byte[] tataBytes(byte[] b);
    
    public String tataStr();
    public String[] tataStrs();
    
    public void tata(int i, int j);
    public void tata(String s);
    
    public String tata1(String a);
    public String tata1(char[] a);
    
    public String tata5(String a, int i);
    public String tata5(String[] a, int i);
    public String tata5(String a, int[] i);
    
    public long add(int i, int j, int k);

}
