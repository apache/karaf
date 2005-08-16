/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.osgi.framework.installer.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.apache.osgi.framework.installer.BooleanProperty;
import org.apache.osgi.framework.installer.Property;

public class BooleanEditor extends JPanel
{
    private BooleanProperty m_prop = null;
    private JRadioButton m_trueButton = null;
    private JRadioButton m_falseButton = null;
    private String m_trueString = null;
    private String m_falseString = null;

    public BooleanEditor(BooleanProperty prop)
    {
        this(prop, "true", "false");
    }

    public BooleanEditor(BooleanProperty prop, String trueString, String falseString)
    {
        m_prop = prop;
        m_trueString = trueString;
        m_falseString = falseString;
        init();
    }

    public Property getProperty()
    {
        return m_prop;
    }

    public void setEnabled(boolean b)
    {
        m_trueButton.setEnabled(b);
        m_falseButton.setEnabled(b);
    }

    protected void init()
    {
        add(m_trueButton = new JRadioButton(m_trueString));
        add(m_falseButton = new JRadioButton(m_falseString));
        ButtonGroup group = new ButtonGroup();
        group.add(m_trueButton);
        group.add(m_falseButton);
        if (m_prop.getBooleanValue())
        {
            m_trueButton.setSelected(true);
        }
        else
        {
            m_falseButton.setSelected(true);
        }

        // Add action listeners.
        m_trueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                m_prop.setBooleanValue(true);
            }
        });
        m_falseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                m_prop.setBooleanValue(false);
            }
        });
    }
}