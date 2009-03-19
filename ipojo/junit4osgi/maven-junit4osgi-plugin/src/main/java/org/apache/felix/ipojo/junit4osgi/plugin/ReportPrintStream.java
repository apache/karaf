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
package org.apache.felix.ipojo.junit4osgi.plugin;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Print stream dispatching on a given one and storing written data
 * in a output stream.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ReportPrintStream extends PrintStream {

    private PrintStream m_stream;
    
    private boolean m_duplicate;

    public ReportPrintStream(OutputStream out, PrintStream def, boolean hideOutput) {
        super(out);
        m_stream = def;
        m_duplicate = ! hideOutput;
    }

    public void println() {
        if (m_duplicate) { m_stream.println(); }
        super.println();
    }


    public void println(boolean x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }


    public void println(char x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }


    public void println(char[] x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }


    public void println(double x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }


    public void println(float x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }


    public void println(int x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }



    public void println(long x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }


    public void println(Object x) {
        if (m_duplicate) { m_stream.println(x); }
        super.println(x);
    }


    public void println(String s) {
        if (m_duplicate) { m_stream.println(s); }
        super.println(s);
    }

}
