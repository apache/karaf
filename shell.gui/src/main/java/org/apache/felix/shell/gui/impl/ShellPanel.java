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
package org.apache.felix.shell.gui.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.felix.shell.gui.Plugin;

public class ShellPanel extends JPanel implements PropertyChangeListener
{
    private Activator m_activator = null;
    private JPanel m_emptyPanel = null;
    private JList m_pluginList = null;
    private Plugin m_selectedPlugin = null;
    private Runnable m_runnable = null;

    public ShellPanel(Activator activator)
    {
        m_activator = activator;
        m_activator.addPropertyChangeListener(this);

        setLayout(new BorderLayout());
        JScrollPane scroll = null;
        add(scroll = new JScrollPane(m_pluginList = new JList(new SimpleListModel())), BorderLayout.WEST);
        scroll.setPreferredSize(new Dimension(150, scroll.getPreferredSize().height));
        add(m_emptyPanel = new JPanel(), BorderLayout.CENTER);

        createEventListeners();
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getPropertyName().equals(Activator.PLUGIN_LIST_PROPERTY))
        {
            if (m_runnable == null)
            {
                m_runnable = new PropertyChangeRunnable();
            }
            SwingUtilities.invokeLater(m_runnable);
        }
    }

    private void createEventListeners()
    {
        m_pluginList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event)
            {
                if (!event.getValueIsAdjusting())
                {
                    if (m_pluginList.getSelectedIndex() >= 0)
                    {
                        // Remove the current GUI.
                        if (m_selectedPlugin != null)
                        {
                            remove(m_selectedPlugin.getGUI());
                        }
                        else
                        {
                            remove(m_emptyPanel);
                        }

                        // Get the selected plugin GUI.
                        m_selectedPlugin =
                            m_activator.getPlugin(m_pluginList.getSelectedIndex());
                        if (m_selectedPlugin != null)
                        {
                            // Display the selected plugin GUI.
                            add(m_selectedPlugin.getGUI(), BorderLayout.CENTER);
                        }
                        else
                        {
                            // Display the empty panel.
                            add(m_emptyPanel, BorderLayout.CENTER);
                        }

                        revalidate();
                        repaint();
                    }
                }
            }
        });
    }

    private class SimpleListModel extends AbstractListModel
        implements ListDataListener
    {
        private SimpleListModel()
        {
        }

        public int getSize()
        {
            return m_activator.getPluginCount();
        }

        public Object getElementAt(int index)
        {
            return m_activator.getPlugin(index).getName();
        }

        public void intervalAdded(ListDataEvent event)
        {
            fireIntervalAdded(this, event.getIndex0(), event.getIndex1());
        }

        public void intervalRemoved(ListDataEvent event)
        {
            fireIntervalRemoved(this, event.getIndex0(), event.getIndex1());
        }

        public void contentsChanged(ListDataEvent event)
        {
            fireContentsChanged(this, event.getIndex0(), event.getIndex1());
        }

        public void update()
        {
            fireContentsChanged(this, 0, -1);
        }
    }

    private class PropertyChangeRunnable implements Runnable
    {
        public void run()
        {
            ((SimpleListModel) m_pluginList.getModel()).update();

            // Check to see if the selected component has been
            // removed, if so, then reset the selected component
            // to be an empty panel.
            if ((m_selectedPlugin != null) &&
                !m_activator.pluginExists(m_selectedPlugin))
            {
                m_pluginList.clearSelection();
                remove(m_selectedPlugin.getGUI());
                m_selectedPlugin = null;
                add(m_emptyPanel, BorderLayout.CENTER);
                revalidate();
                repaint();
            }

            if ((m_selectedPlugin == null) && (m_activator.getPluginCount() > 0))
            {
                m_pluginList.setSelectedIndex(0);
            }
        }
    }
}