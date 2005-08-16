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
package org.apache.osgi.framework.installer;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

public class PropertyPanel extends JPanel
{
    private List m_propList = null;
    private Map m_propToCompMap = null;

    public PropertyPanel(List paramList)
    {
        super();
        m_propList = paramList;
        m_propToCompMap = new HashMap();
        layoutComponents();
    }

    public void setEnabled(boolean b)
    {
        for (int i = 0; i < m_propList.size(); i++)
        {
            Property prop = (Property) m_propList.get(i);
            JComponent comp = (JComponent) m_propToCompMap.get(prop.getName());
            comp.setEnabled(b);
        }
    }

    public List getProperties()
    {
        return m_propList;
    }

    protected void layoutComponents()
    {
        // Create the field panel for entering query variables.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        setLayout(grid);

        for (int i = 0; i < m_propList.size(); i++)
        {
            Property prop = (Property) m_propList.get(i);
            JLabel label = null;
            JComponent component = null;

            // Add label.
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.gridheight = 1;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            grid.setConstraints(label = new JLabel(prop.getName()), gbc);
            add(label);

            gbc.gridx = 1;
            gbc.gridy = i;
            gbc.gridheight = 1;
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.WEST;
            grid.setConstraints(component = prop.getEditor(), gbc);
            add(component);

            m_propToCompMap.put(prop.getName(), component);
        }
    }
}