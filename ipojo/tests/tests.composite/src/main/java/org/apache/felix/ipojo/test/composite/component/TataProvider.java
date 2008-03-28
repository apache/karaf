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
package org.apache.felix.ipojo.test.composite.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.composite.service.Tata;


public class TataProvider implements Tata {
    
    int tata = 0;
    int tataStr = 0;
    int tataStrs = 0;
    int tata_2 = 0;
    int tata_3 = 0;
    int tata1 = 0;
    int tata1_1 = 0;
    int tata5 = 0;
    int tata5_1 = 0;
    int tata5_2 = 0;
    int tataBoolean = 0;
    int tataBooleans = 0;
    int tataByte = 0;
    int tataBytes = 0;
    private int add;
    private int tataShorts;
    private int tataShort;
    private int tataLongs;
    private int tataLong;
    private int tataInts;
    private int tataInt;
    private int tataFloat;
    private int tataFloats;
    private int tataDoubles;
    private int tataDouble;
    private int tataChars;
    private int tataChar;
    
    public Properties getPropsTata() {
        Properties props = new Properties();
        props.put("tata", new Integer(tata));
        props.put("tataStr", new Integer(tataStr));
        props.put("tataStrs", new Integer(tataStrs));
        props.put("tata_2", new Integer(tata_2));
        props.put("tata_3", new Integer(tata_3));
        props.put("tata1", new Integer(tata1));
        props.put("tata1_1", new Integer(tata1_1));
        props.put("tata5", new Integer(tata5));
        props.put("tata5_1", new Integer(tata5_1));
        props.put("tata5_2", new Integer(tata5_2));
        props.put("add", new Integer(add));
        props.put("tataBoolean", new Integer(tataBoolean));
        props.put("tataBoolean", new Integer(tataBoolean));
        props.put("tataByte", new Integer(tataByte));
        props.put("tataBytes", new Integer(tataBytes));
        props.put("tataShort", new Integer(tataShort));
        props.put("tataShorts", new Integer(tataShorts));
        props.put("tataLongs", new Integer(tataLongs));
        props.put("tataLong", new Integer(tataLong));
        props.put("tataInt", new Integer(tataInt));
        props.put("tataInts", new Integer(tataInts));
        props.put("tataFloat", new Integer(tataFloat));
        props.put("tataFloats", new Integer(tataFloats));
        props.put("tataDouble", new Integer(tataDouble));
        props.put("tataDoubles", new Integer(tataDoubles));
        props.put("tataChar", new Integer(tataChar));
        props.put("tataChars", new Integer(tataChars));
        return props;
    }

    public void tata() {
        tata++;
    }

    public String tataStr() {
        tataStr++;
        return "Tata";
    }

    public String[] tataStrs() {
        tataStrs++;
        return new String[] {"T", "A", "T", "A"};
    }

    public void tata(int i, int j) {
        tata_2++;        
    }

    public void tata(String s) {
        tata_3++;
    }

    public String tata1(String a) {
        tata1++;
       return a;
    }

    public String tata1(char[] a) {
        tata1_1++;
        String s = new String(a);
        return s;
    }

    public String tata5(String a, int i) {
       tata5++;
       return a+i;
    }

    public String tata5(String[] a, int i) {
       tata5_1++;
       return ""+a.length + i;
    }

    public String tata5(String a, int[] i) {
        tata5_2++;
        return a + i.length;
    }

    public boolean tataBoolean(boolean b) {
        tataBoolean++;
        return b;
    }

    public boolean[] tataBooleans(boolean[] b) {
        tataBooleans++;
       return b;
    }

    public byte tataByte(byte b) {
        tataByte++;
        return b;
    }

    public byte[] tataBytes(byte[] b) {
        tataBytes++;
        return b;
    }

    public char tataChar(char c) {
       tataChar++;
       return c;
    }

    public char[] tataChars(char[] c) {
        tataChars++;
        return c;
    }

    public double tataDouble(double d) {
        tataDouble++;
        return d;
    }

    public double[] tataDoubles(double[] d) {
        tataDoubles++;
        return d;
    }

    public float tataFloat(float f) {
        tataFloat++;
        return f;
    }

    public float[] tataFloats(float[] f) {
        tataFloats++;
        return f;
    }

    public int tataInt(int i) {
        tataInt++;
        return i;
    }

    public int[] tataInts(int[] its) {
        tataInts++;
        return its;
    }

    public long tataLong(long l) {
        tataLong++;
        return l;
    }

    public long[] tataLongs(long[] l) {
        tataLongs++;
        return l;
    }

    public short tataShort(short s) {
        tataShort++;
        return s;
    }

    public short[] tataShorts(short[] s) {
        tataShorts++;
        return s;
    }

    public long add(int i, int j, int k) {
        add++;
        return i + j + k;
    }


}
