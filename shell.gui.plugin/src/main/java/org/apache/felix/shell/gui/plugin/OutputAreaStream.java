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
package org.apache.felix.shell.gui.plugin;

import java.io.OutputStream;

class OutputAreaStream extends OutputStream
{
    private ScrollableOutputArea m_soa;

    public OutputAreaStream(ScrollableOutputArea soa)
    {
        m_soa = soa;
    }

    public void write(byte[] b)
    {
        String tmp = new String(b);
        m_soa.addText(tmp);
    }

    public void write(byte[] b, int off, int len)
    {
        String tmp = new String(b, off, len);
        m_soa.addText(tmp);
    }

    public void write(int b)
    {
        byte[] ba = { (byte) b };
        m_soa.addText(new String(ba));
    }
}