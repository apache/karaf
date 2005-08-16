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

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.osgi.framework.installer.Property;
import org.apache.osgi.framework.installer.StringProperty;

public class StringEditor extends JPanel
{
    private StringProperty m_prop = null;
    private JTextField m_textField = null;

    public StringEditor(StringProperty prop)
    {
        m_prop = prop;
        init();
    }

    public Property getProperty()
    {
        return m_prop;
    }

    public void setEnabled(boolean b)
    {
        m_textField.setEnabled(b);
    }

    protected void init()
    {
        // Set layout.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 2, 0, 2);
        setLayout(grid);

        // Add field.
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        m_textField = new JTextField(20);
        m_textField.setText(m_prop.getStringValue());
        grid.setConstraints(m_textField, gbc);
        add(m_textField);

        // Add focus listener.
        m_textField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent event)
            {
            }
            public void focusLost(FocusEvent event)
            {
                if (!event.isTemporary())
                {
                    m_prop.setStringValue(m_textField.getText());
                }
            }
        });
    }
}
