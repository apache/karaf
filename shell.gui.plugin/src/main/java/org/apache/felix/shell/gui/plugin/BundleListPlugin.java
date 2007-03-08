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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import org.apache.felix.shell.gui.Plugin;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class BundleListPlugin extends JPanel implements Plugin
{
    private BundleContext m_context = null;
    private JTextField m_urlField = null;
    private JButton m_installButton = null;
    private JTable m_bundleTable = null;
    private JButton m_startButton = null;
    private JButton m_stopButton = null;
    private JButton m_updateButton = null;
    private JButton m_refreshButton = null;
    private JButton m_uninstallButton = null;
    private JButton m_shutdownButton = null;

    // Plugin interface methods.

    public String getName()
    {
        return "Bundle List";    
    }
    
    public Component getGUI()
    {
        return this;
    }
    
    // Implementation.
    
    public BundleListPlugin(BundleContext context)
    {
        m_context = context;

        // Create user interface components.
        setLayout(new BorderLayout());
        add(createURLPanel(), BorderLayout.NORTH);
        add(new JScrollPane(m_bundleTable = new JTable()), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        // Set table model to display bundles.
        m_bundleTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        m_bundleTable.setModel(new SimpleTableModel());
        m_bundleTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        m_bundleTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        m_bundleTable.getColumnModel().getColumn(2).setPreferredWidth(350);

        createEventListeners();
    }

    private JPanel createURLPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("URL"), BorderLayout.WEST);
        panel.add(m_urlField = new JTextField(20), BorderLayout.CENTER);
        panel.add(m_installButton = new JButton("Install"), BorderLayout.EAST);
        m_installButton.setMnemonic('I');
        return panel;
    }

    private JPanel createButtonPanel()
    {
        JPanel panel = new JPanel();
        panel.add(m_startButton = new JButton("Start"));
        panel.add(m_stopButton = new JButton("Stop"));
        panel.add(m_updateButton = new JButton("Update"));
        panel.add(m_refreshButton = new JButton("Refresh"));
        panel.add(m_uninstallButton = new JButton("Uninstall"));
        panel.add(m_shutdownButton = new JButton("Shutdown"));
        m_startButton.setMnemonic('S');
        m_stopButton.setMnemonic('p');
        m_updateButton.setMnemonic('a');
        m_refreshButton.setMnemonic('R');
        m_uninstallButton.setMnemonic('U');
        m_shutdownButton.setMnemonic('d');
        return panel;
    }

    private void createEventListeners()
    {
        // Listen for bundle events in order to update
        // the GUI bundle list.
        BundleListener bl = new BundleListener() {
            public void bundleChanged(BundleEvent event)
            {
                ((SimpleTableModel) m_bundleTable.getModel()).update();
            }
        };
        m_context.addBundleListener(bl);

        // Create action listeners.
        m_installButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                if (m_urlField.getText().length() > 0)
                {
                    try
                    {
                        m_context.installBundle(m_urlField.getText(), null);
                    }
                    catch (BundleException ex)
                    {
                        JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(BundleListPlugin.this),
                            ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        m_startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                int[] rows = m_bundleTable.getSelectedRows();
                for (int i = 0; i < rows.length; i++)
                {
                    try
                    {
                        m_context.getBundles()[rows[i]].start();
                    }
                    catch (BundleException ex)
                    {
                        JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(BundleListPlugin.this),
                            ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        m_stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                int[] rows = m_bundleTable.getSelectedRows();
                for (int i = 0; i < rows.length; i++)
                {
                    try
                    {
                        m_context.getBundles()[rows[i]].stop();
                    }
                    catch (BundleException ex)
                    {
                        JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(BundleListPlugin.this),
                            ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        m_updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                int[] rows = m_bundleTable.getSelectedRows();
                for (int i = 0; i < rows.length; i++)
                {
                    try
                    {
                        m_context.getBundles()[rows[i]].update();
                    }
                    catch (BundleException ex)
                    {
                        JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(BundleListPlugin.this),
                            ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        m_refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                // Get package admin service.
                ServiceReference ref = m_context.getServiceReference(
                    PackageAdmin.class.getName());
                if (ref == null)
                {
                    JOptionPane.showMessageDialog(
                        JOptionPane.getFrameForComponent(BundleListPlugin.this),
                        "Unable to obtain PackageAdmin service.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                PackageAdmin pa = (PackageAdmin) m_context.getService(ref);
                if (pa == null)
                {
                    JOptionPane.showMessageDialog(
                        JOptionPane.getFrameForComponent(BundleListPlugin.this),
                        "Unable to obtain PackageAdmin service.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                pa.refreshPackages(null);
            }
        });

        m_uninstallButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                int[] rows = m_bundleTable.getSelectedRows();
                // We need to uninstall in reverse order, otherwise
                // the index will get messed up.
                for (int i = rows.length - 1; i >= 0; i--)
                {
                    try
                    {
                        m_context.getBundles()[rows[i]].uninstall();
                    }
                    catch (BundleException ex)
                    {
                        JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(BundleListPlugin.this),
                            ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        m_shutdownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                Bundle systembundle = m_context.getBundle(0);
                try
                {
                    systembundle.stop();
                }
                catch (Exception ex)
                {
                    System.out.println(ex.toString());
                    ex.printStackTrace(System.out);
                }
            }
        });
    }

    private class SimpleTableModel extends AbstractTableModel
    {
        public int getRowCount()
        {
            return (m_context.getBundles() == null)
                ? 0 : m_context.getBundles().length;
        }

        public int getColumnCount()
        {
            return 3;
        }

        public String getColumnName(int column)
        {
            if (column == 0)
            {
                return "Id";
            }
            else if (column == 1)
            {
                return "State";
            }
            else if (column == 2)
            {
                return "Location";
            }
            return "";
        }

        public Class getColumnClass(int column)
        {
            if (column == 0)
            {
                return Long.class;
            }

            return String.class;
        }

        public boolean isCellEditable(int row, int column)
        {
            return false;
        }

        public Object getValueAt(int row, int column)
        {
            if (column == 0)
            {
                return new Long(m_context.getBundles()[row].getBundleId());
            }
            else if (column == 1)
            {
                return getStateString(m_context.getBundles()[row].getState());
            }
            else if (column == 2)
            {
                String name = (String)
                    m_context.getBundles()[row].getHeaders().get(Constants.BUNDLE_NAME);
                name = (name == null)
                    ? m_context.getBundles()[row].getLocation() : name;
                return name;
            }
            return null;
        }

        public void update()
        {
            fireTableDataChanged();
        }

        private String getStateString(int state)
        {
            switch (state)
            {
                case Bundle.INSTALLED:
                    return "Installed";
                case Bundle.RESOLVED:
                    return "Resolved";
                case Bundle.STARTING:
                    return "Starting";
                case Bundle.ACTIVE:
                    return "Active";
                case Bundle.STOPPING:
                    return "Stopping";
            }
            return "[unknown]";
        }
    }
}