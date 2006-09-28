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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.PrintStream;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.felix.shell.ShellService;
import org.apache.felix.shell.gui.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ShellPlugin extends JPanel implements Plugin
{
    private BundleContext m_context = null;
    private PrintStream m_out = null;

    private JTextField m_commandField = new JTextField();
    private String[] m_history = new String[25];
    private int m_historyCount = -1;
    private int m_current = 0; // current history counter

    private ScrollableOutputArea m_soa = new ScrollableOutputArea();

    // Plugin interface methods.

    public String getName()
    {
        return "Shell";    
    }
    
    public Component getGUI()
    {
        return this;
    }
    
    // Implementation.
    
    public ShellPlugin(BundleContext context)
    {
        m_context = context;
        m_out = new PrintStream(new OutputAreaStream(m_soa));
        initialize();
    }

    private void initialize()
    {
        setLayout(new BorderLayout());
        m_commandField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event)
            {
                commandFieldKeyPressed(event);
            }
        });
        m_commandField.setFont(new java.awt.Font("Monospaced", 0, 12));
        add(m_commandField, BorderLayout.SOUTH);
        add(m_soa, BorderLayout.CENTER);
    }

    void processCommand(String line)
    {
        if (line == null)
        {
            return;
        }

        line = line.trim();

        if (line.length() == 0)
        {
            return;
        }

        // Get shell service.
        ServiceReference ref = m_context.getServiceReference(
            org.apache.felix.shell.ShellService.class.getName());
        if (ref == null)
        {
            m_out.println("No shell service is available.");
            return;
        }

        ShellService shell = (ShellService) m_context.getService(ref);

        // Print the command line in the output window.
        m_out.println("-> " + line);

        try {
            shell.executeCommand(line, m_out, m_out);
        } catch (Exception ex) {
            m_out.println(ex.toString());
            ex.printStackTrace(m_out);
        }

        m_context.ungetService(ref);
    }

    private void addToHistory(String command)
    {
        m_historyCount++;
        if (m_historyCount >= m_history.length)
        {
            m_historyCount = m_history.length - 1;
            for (int i = 0; i < m_history.length - 1; i++)
            {
                m_history[i] = m_history[i + 1];
            }
        }
        m_history[m_historyCount] = new String(command);
    }

    private String getFromHistory(int num)
    {
        if (num < 0)
        {
            return ("");
        }
        else if (num > m_historyCount)
        {
            return("");
        }
        return m_history[num];
    }

    protected void commandFieldKeyPressed(KeyEvent event)
    {
        String command = null;
        int c = event.getKeyCode();
        // Cursor up.
        if (c == 38)
        {
            m_current--;
            if (m_current < 0)
            {
                m_current = 0;
            }
            m_commandField.setText(getFromHistory(m_current));
            m_commandField.setCaretPosition(m_commandField.getText().length());
        }
        // Cursor down.
        else if (c == 40)
        {
            m_current++;
            if (m_current > m_historyCount)
            {
                m_current = m_historyCount + 1;
            }
            m_commandField.setText(getFromHistory(m_current));
            m_commandField.setCaretPosition(m_commandField.getText().length());
        }
        else if (c == 10)
        {
            command = m_commandField.getText();
            if (!command.equals(""))
            {
                addToHistory(command);
                m_current = m_historyCount + 1;
                processCommand(command);
            }
            m_commandField.setText("");
        }
    }
}