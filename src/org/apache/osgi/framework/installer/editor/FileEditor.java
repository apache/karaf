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
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import org.apache.osgi.framework.installer.Property;
import org.apache.osgi.framework.installer.StringProperty;

public class FileEditor extends JPanel
{
    private StringProperty m_prop = null;
    private JTextField m_textField = null;
    private JButton m_browseButton = null;
    private boolean m_isDirectory = false;

    public FileEditor(StringProperty prop, boolean isDirectory)
    {
        super();
        m_prop = prop;
        m_isDirectory = isDirectory;
        init();
    }

    public Property getProperty()
    {
        return m_prop;
    }

    public void setEnabled(boolean b)
    {
        m_textField.setEnabled(b);
        m_browseButton.setEnabled(b);
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
        m_textField = new JTextField(30);
        m_textField.setText(m_prop.getStringValue());
        grid.setConstraints(m_textField, gbc);
        add(m_textField);

        // Add button.
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        m_browseButton = new JButton("Browse...");
        m_browseButton.setMargin(new Insets(1, 1, 1, 1));
        grid.setConstraints(m_browseButton, gbc);
        add(m_browseButton);

        // Add focus listener.
        m_textField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent event)
            {
            }
            public void focusLost(FocusEvent event)
            {
                if (!event.isTemporary())
                {
                    // Set the new value.
                    m_prop.setStringValue(normalizeValue(m_textField.getText()));

                }
            }
        });

        // Add action listener.
        m_browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                JFileChooser fileDlg = new JFileChooser();
                if (m_isDirectory)
                {
                    fileDlg.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fileDlg.setDialogTitle("Please select a directory...");
                }
                else
                {
                    fileDlg.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileDlg.setDialogTitle("Please select a file...");
                }
                fileDlg.setApproveButtonText("Select");
                if (fileDlg.showOpenDialog(FileEditor.this) ==
                    JFileChooser.APPROVE_OPTION)
                {
                    m_textField.setText(fileDlg.getSelectedFile().getAbsolutePath());
                    m_prop.setStringValue(normalizeValue(m_textField.getText()));
                }
            }
        });
    }

    private String normalizeValue(String value)
    {
        // Make sure that directories never end with a slash,
        // for consistency.
        if (m_isDirectory)
        {
            if (value.endsWith(File.separator))
            {
                value = value.substring(0, value.length() - 1);
            }
        }
        return value;
    }
}