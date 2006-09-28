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

import java.awt.Font;

import javax.swing.*;

public class ScrollableOutputArea extends JScrollPane
{
    private JTextArea m_textArea = null;

    public ScrollableOutputArea()
    {
        super();
        m_textArea = new JTextArea();
        initialize();
    }

    public ScrollableOutputArea(int rows, int columns)
    {
        super();
        m_textArea = new JTextArea(rows, columns);
        initialize();
    }

    private void initialize()
    {
        setViewportView(m_textArea);
        m_textArea.setLineWrap(true);
        m_textArea.setAutoscrolls(true);
        m_textArea.setEnabled(true);
        m_textArea.setEditable(false);
        m_textArea.setFont(new Font("Monospaced", 0, 12));
        setAutoscrolls(true);
    }

    public void setText(String s)
    {
        m_textArea.setText(s);
    }

    public void addText(String text)
    {
        m_textArea.append(text);
        if (m_textArea.isDisplayable())
        {
            try
            {
                m_textArea.setCaretPosition(Integer.MAX_VALUE); // Scroll to end of window
            }
            catch (Exception e)
            {
                // Just for safety
            }
        }
        validate();
        JScrollBar sb = getVerticalScrollBar();
        if ((sb != null) && (sb.isVisible()))
        {
            sb.setValue(Integer.MAX_VALUE);
        }
    }
}