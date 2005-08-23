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
package org.apache.felix.framework.installer.editor;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.felix.framework.installer.*;

public class BooleanStringEditor extends JPanel
{
    private Property m_prop = null;
    private JCheckBox m_includeButton = null;
    private JTextField m_textField = null;

    public BooleanStringEditor(Property prop)
    {
        if ((prop instanceof BooleanProperty) && (prop instanceof StringProperty))
        {
            m_prop = prop;
        }
        else
        {
            throw new IllegalArgumentException(
                "Property must implement both boolean and string property interfaces.");
        }
        init();
    }

    public Property getProperty()
    {
        return m_prop;
    }

    public void setEnabled(boolean b)
    {
        m_includeButton.setEnabled(b);
        m_textField.setEnabled(b && m_includeButton.isSelected());
    }

    protected void init()
    {
        // Set layout.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 2, 0, 2);
        setLayout(grid);

        // Add button.
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        m_includeButton = new JCheckBox("");
        grid.setConstraints(m_includeButton, gbc);
        add(m_includeButton);

        // Add field.
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        m_textField = new JTextField(30);
        m_textField.setText(((StringProperty) m_prop).getStringValue());
        grid.setConstraints(m_textField, gbc);
        add(m_textField);
        m_textField.setEnabled(false);

        // Add action listener.
        m_includeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                if (m_includeButton.isSelected())
                {
                    ((BooleanProperty) m_prop).setBooleanValue(true);
                    m_textField.setEnabled(true);
                }
                else
                {
                    ((BooleanProperty) m_prop).setBooleanValue(false);
                    m_textField.setEnabled(false);
                }
            }
        });

        // Add focus listener.
        m_textField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent event)
            {
            }
            public void focusLost(FocusEvent event)
            {
                if (!event.isTemporary())
                {
                    ((StringProperty) m_prop).setStringValue(m_textField.getText());
                }
            }
        });

        // Currently, the button is not selected. If the property
        // is true, then click once to select button.        
        if (((BooleanProperty) m_prop).getBooleanValue())
        {
            m_includeButton.doClick();
        }
    }
}