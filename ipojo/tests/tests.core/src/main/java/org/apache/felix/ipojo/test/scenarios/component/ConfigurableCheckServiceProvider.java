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
package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.CheckService;

public class ConfigurableCheckServiceProvider implements CheckService {
    
    // Integer types
    byte b;
    short s;
    int i;
    long l;
    
    // Floatting types
    double d;
    float f;
    
    // Character
    char c;
    
    // Boolean
    boolean bool;
    
    // Integer arrays 
    byte[] bs;
    short[] ss;
    int[] is;
    long[] ls;
    
    double[] ds;
    float[] fs;
    
    char[] cs;
    
    boolean[] bools;
    
    String string;
    String[] strings;
    
    int upB, upS, upI, upL, upD, upF, upC, upBool, upBs, upSs, upIs, upLs, upDs, upFs, upCs, upBools, upString, upStrings;
    

    public boolean check() {
        return true;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("b", new Byte(b));
        props.put("s", new Short(s));
        props.put("i", new Integer(i));
        props.put("l", new Long(l));
        props.put("d", new Double(d));
        props.put("f", new Float(f));
        props.put("c", new Character(c));
        props.put("bool", new Boolean(bool));
        
        props.put("bs", bs);
        props.put("ss", ss);
        props.put("is", is);
        props.put("ls", ls);
        props.put("ds", ds);
        props.put("fs", fs);
        props.put("cs", cs);
        props.put("bools", bools);
        
        props.put("upb", new Integer(upB));
        props.put("ups", new Integer(upS));
        props.put("upi", new Integer(upI));
        props.put("upl", new Integer(upL));
        props.put("upd", new Integer(upD));
        props.put("upf", new Integer(upF));
        props.put("upc", new Integer(upC));
        props.put("upbool", new Integer(upBool));
        
        props.put("upbs", new Integer(upBs));
        props.put("upss", new Integer(upSs));
        props.put("upis", new Integer(upIs));
        props.put("upls", new Integer(upLs));
        props.put("upds", new Integer(upDs));
        props.put("upfs", new Integer(upFs));
        props.put("upcs", new Integer(upCs));
        props.put("upbools", new Integer(upBools));
        
        props.put("string", string);
        props.put("strings", strings);
        props.put("upstring", new Integer(upString));
        props.put("upstrings", new Integer(upStrings));
        
        return props;
    }
    
    public void updateB(byte bb) {
        b = bb;
        upB++;
    }
    
    public void updateS(short bb) {
        s = bb;
        upS++;
    }
    
    public void updateI(int bb) {
        i = bb;
        upI++;
    }
    
    public void updateL(long bb) {
        l = bb;
        upL++;
    }
    
    public void updateD(double bb) {
        d = bb;
        upD++;
    }
    
    public void updateF(float bb) {
        f = bb;
        upF++;
    }
    
    public void updateC(char bb) {
        c = bb;
        upC++;
    }
    
    public void updateBool(boolean bb) {
        bool = bb;
        upBool++;
    }
    
    public void updateBs(byte[] bb) {
        bs = bb;
        upBs++;
    }
    
    public void updateSs(short[] bb) {
        ss = bb;
        upSs++;
    }
    
    public void updateIs(int[] bb) {
        is = bb;
        upIs++;
    }
    
    public void updateLs(long[] bb) {
        ls = bb;
        upLs++;
    }
    
    public void updateDs(double[] bb) {
        ds = bb;
        upDs++;
    }
    
    public void updateFs(float[] bb) {
        fs = bb;
        upFs++;
    }
    
    public void updateCs(char[] bb) {
        cs = bb;
        upCs++;
    }
    
    public void updateBools(boolean[] bb) {
        bools = bb;
        upBools++;
    }
    
    public void updateStrings(String[] bb) {
        strings = bb;
        upStrings++;
    }
    
    public void updateString(String bb) {
        string = bb;
        upString++;
    }

}
