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

import org.apache.felix.ipojo.test.composite.service.Toto;


public class TotoProviderGlue implements Toto {
    
    Toto m_toto;
    
    private int i = 0;
    public static int toto = 0;
    public static int toto_2 = 0;
    public static int toto_3 = 0;
    public static int toto_4 = 0;
    public static int toto1 = 0;
    
    public int count() {
        return i;
    }
    
    public void toto() {
        toto++;
        m_toto.toto();
    }

    public void toto(int i, int j) {
        toto_2++;
        m_toto.toto(i, j);
    }

    public String toto(String a) {
        toto_3++;
        return a;
    }

    public String toto(String[] a) {
        toto_4++;
        return "toto";
    }

    public void toto1(String j) {
        i++;
        toto1++;
        m_toto.toto1(j);
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("i", new Integer(i));
        props.put("gtoto", new Integer(toto));
        props.put("gtoto_2", new Integer(toto_2));
        props.put("gtoto_3", new Integer(toto_3));
        props.put("gtoto_4", new Integer(toto_4));
        props.put("gtoto1", new Integer(toto1));
        props.put("glue", "glue");
        Properties p2 = m_toto.getProps();
        props.putAll(p2);
        return props;
    }

}
