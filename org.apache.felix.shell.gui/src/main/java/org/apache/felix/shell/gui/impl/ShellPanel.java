/*
 * Oscar Shell GUI
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.apache.felix.shell.gui.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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