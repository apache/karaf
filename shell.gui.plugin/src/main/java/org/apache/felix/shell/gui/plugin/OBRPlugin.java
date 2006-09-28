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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import org.apache.felix.shell.gui.Plugin;
import org.osgi.framework.*;
import org.osgi.service.obr.*;

public class OBRPlugin extends JPanel implements Plugin
{
    private static final String DEPLOY_BUTTON = "Deploy";
    private static final String START_BUTTON = "Deploy & start";

    private BundleContext m_context = null;
    private ServiceReference m_repoRef = null;
    private RepositoryAdmin m_repoAdmin = null;

    private SimpleTreeNode m_rootNode = null;
    private CreateRootRunnable m_createRootRunnable = new CreateRootRunnable();
    private SetRootRunnable m_setRootRunnable = new SetRootRunnable();

    private JButton m_addRepoButton = null;
    private JButton m_removeRepoButton = null;
    private JButton m_refreshRepoButton = null;
    private JTree m_bundleTree = null;
    private JButton m_deployButton = null;
    private JButton m_startButton = null;
    private JButton m_infoButton = null;
    private ScrollableOutputArea m_soa = null;
    private JButton m_clearButton = null;

    private PrintStream m_out = null;

    // Plugin interface methods.

    public String getName()
    {
        return "OBR";
    }
    
    public Component getGUI()
    {
        return this;
    }
    
    // Implementation.
    
    public OBRPlugin(BundleContext context)
    {
        m_context = context;
        m_out = new PrintStream(
            new OutputAreaStream(
                m_soa = new ScrollableOutputArea(5, 30)));

        // Create the gui.
        createUserInterface();

        synchronized (this)
        {
            // Listen for registering/unregistering bundle repository services.
            ServiceListener sl = new ServiceListener() {
                public void serviceChanged(ServiceEvent event)
                {
                    synchronized (OBRPlugin.this)
                    {
                        // Ignore additional services if we already have one.
                        if ((event.getType() == ServiceEvent.REGISTERED)
                            && (m_repoRef != null))
                        {
                            return;
                        }
                        // Initialize the service if we don't have one.
                        else if ((event.getType() == ServiceEvent.REGISTERED)
                            && (m_repoRef == null))
                        {
                            lookupService();
                        }
                        // Unget the service if it is unregistering.
                        else if ((event.getType() == ServiceEvent.UNREGISTERING)
                            && event.getServiceReference().equals(m_repoRef))
                        {
                            m_context.ungetService(m_repoRef);
                            m_repoRef = null;
                            m_repoAdmin = null;
                            // Try to get another service.
                            lookupService();
                        }
                    }
                }
            };

            try
            {
                m_context.addServiceListener(sl,
                    "(objectClass=" + RepositoryAdmin.class.getName() + ")");
            }
            catch (InvalidSyntaxException ex)
            {
                System.err.println("OBRPlugin: " + ex);
            }

            // Now try to manually initialize the shell service
            // since one might already be available.
            lookupService();
        }
    }

    private synchronized void lookupService()
    {
        if (m_repoAdmin != null)
        {
            return;
        }
        m_repoRef = m_context.getServiceReference(RepositoryAdmin.class.getName());
        if (m_repoRef == null)
        {
        }
        else
        {
            m_repoAdmin = (RepositoryAdmin) m_context.getService(m_repoRef);
        }

        // Update the model.
        initializeRootNode();
    }

    private void createEventListeners()
    {
        // Create action listeners.
        m_addRepoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                synchronized (OBRPlugin.this)
                {
                    if (m_repoAdmin == null)
                    {
                        return;
                    }

                    String s = JOptionPane.showInputDialog(
                        OBRPlugin.this, "Enter repository URL:");

                    if (s != null)
                    {
                        try
                        {
                            m_repoAdmin.addRepository(new URL(s));
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }

                    // Update the table.
                    initializeRootNode();
                }
            }
        });

        m_removeRepoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                synchronized (OBRPlugin.this)
                {
                    if (m_repoAdmin == null)
                    {
                        return;
                    }

                    TreePath[] paths = m_bundleTree.getSelectionPaths();
                    for (int i = 0; i < paths.length; i++)
                    {
                        SimpleTreeNode node = (SimpleTreeNode) paths[i].getLastPathComponent();
                        if (node.getObject() instanceof Repository)
                        {
                            m_repoAdmin.removeRepository(
                                ((Repository)
                                    ((SimpleTreeNode)
                                        paths[i].getLastPathComponent()).getObject()).getURL());
                        }
                    }

                    // Update the table.
                    initializeRootNode();
                }
            }
        });

        m_refreshRepoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                synchronized (OBRPlugin.this)
                {
                    if (m_repoAdmin == null)
                    {
                        return;
                    }

                    TreePath[] paths = m_bundleTree.getSelectionPaths();
                    for (int i = 0; i < paths.length; i++)
                    {
                        SimpleTreeNode node = (SimpleTreeNode) paths[i].getLastPathComponent();
                        if (node.getObject() instanceof Repository)
                        {
                            try
                            {
                                // Adding the repository again causes it to be reparsed.
                                m_repoAdmin.addRepository(
                                    ((Repository) node.getObject()).getURL());
                            } catch (Exception ex)
                            {
                                ex.printStackTrace();
                            }
                        }
                    }

                    // Update the table.
                    initializeRootNode();
                }
            }
        });

        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                boolean start = event.getActionCommand().equals(START_BUTTON);

                synchronized (OBRPlugin.this)
                {
                    if (m_repoAdmin == null)
                    {
                        return;
                    }

                    Resolver resolver = m_repoAdmin.resolver();
                    TreePath[] paths = m_bundleTree.getSelectionPaths();
                    for (int i = 0; i < paths.length; i++)
                    {
                        SimpleTreeNode node = (SimpleTreeNode) paths[i].getLastPathComponent();
                        if (node.getObject() instanceof Resource)
                        {
                            resolver.add((Resource) node.getObject());
                        }
                    }

                    if ((resolver.getAddedResources() != null) &&
                        (resolver.getAddedResources().length > 0))
                    {
                        if (resolver.resolve())
                        {
                            m_out.println("Target resource(s):");
                            printUnderline(m_out, 19);
                            Resource[] resources = resolver.getAddedResources();
                            for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
                            {
                                m_out.println("   " + resources[resIdx].getPresentationName()
                                    + " (" + resources[resIdx].getVersion() + ")");
                            }
                            resources = resolver.getRequiredResources();
                            if ((resources != null) && (resources.length > 0))
                            {
                                m_out.println("\nRequired resource(s):");
                                printUnderline(m_out, 21);
                                for (int resIdx = 0; resIdx < resources.length; resIdx++)
                                {
                                    m_out.println("   " + resources[resIdx].getPresentationName()
                                        + " (" + resources[resIdx].getVersion() + ")");
                                }
                            }
                            resources = resolver.getOptionalResources();
                            if ((resources != null) && (resources.length > 0))
                            {
                                m_out.println("\nOptional resource(s):");
                                printUnderline(m_out, 21);
                                for (int resIdx = 0; resIdx < resources.length; resIdx++)
                                {
                                    m_out.println("   " + resources[resIdx].getPresentationName()
                                        + " (" + resources[resIdx].getVersion() + ")");
                                }
                            }

                            try
                            {
                                m_out.print("\nDeploying...");
                                resolver.deploy(start);
                                m_out.println("done.");
                            }
                            catch (IllegalStateException ex)
                            {
                                m_out.println(ex);
                            }
                        }
                        else
                        {
                            Requirement[] reqs = resolver.getUnsatisfiedRequirements();
                            if ((reqs != null) && (reqs.length > 0))
                            {
                                m_out.println("Unsatisfied requirement(s):");
                                printUnderline(m_out, 27);
                                for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
                                {
                                    m_out.println("   " + reqs[reqIdx].getFilter());
                                    Resource[] resources = resolver.getResources(reqs[reqIdx]);
                                    for (int resIdx = 0; resIdx < resources.length; resIdx++)
                                    {
                                        m_out.println("      " + resources[resIdx].getPresentationName());
                                    }
                                }
                            }
                            else
                            {
                                m_out.println("Could not resolve targets.");
                            }
                        }

                        m_out.println("");
                    }
                }
            }
        };

        m_deployButton.addActionListener(al);
        m_startButton.addActionListener(al);

        m_infoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                synchronized (OBRPlugin.this)
                {
                    if (m_repoAdmin == null)
                    {
                        return;
                    }
                    TreePath[] paths = m_bundleTree.getSelectionPaths();
                    for (int i = 0; i < paths.length; i++)
                    {
                        if (i != 0)
                        {
                            m_out.println("");
                        }
                        printInfo(m_out,
                            ((SimpleTreeNode) paths[i].getLastPathComponent()).getObject());
                    }
                    m_out.println("");
                }
            }
        });

        m_clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                synchronized (OBRPlugin.this)
                {
                    m_soa.setText("");
                }
            }
        });

        m_bundleTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e)
            {
                if (m_repoAdmin == null)
                {
                    return;
                }
                TreePath[] paths = m_bundleTree.getSelectionPaths();
                boolean repoOnly = true;
                if (paths != null)
                {
                    for (int i = 0; repoOnly && (i < paths.length); i++)
                    {
                        SimpleTreeNode node = (SimpleTreeNode) paths[i].getLastPathComponent();
                        if (!(node.getObject() instanceof Repository))
                        {
                            repoOnly = false;
                        }
                    }
                }
                m_removeRepoButton.setEnabled((paths != null) && repoOnly);
                m_refreshRepoButton.setEnabled((paths != null) && repoOnly);
                m_infoButton.setEnabled((paths != null) && (paths.length > 0));
            }
        });
    }

    private void printInfo(PrintStream out, Object obj)
    {
        if (obj != null)
        {
            if (obj instanceof Repository)
            {
                Repository repo = (Repository) obj;
                out.println(repo.getName());
                out.println("   URL = " + repo.getURL());
                out.println("   Modified = " + new Date(repo.getLastModified()));
            }
            else if (obj instanceof Resource)
            {
                Resource res = (Resource) obj;
                out.println(res.getPresentationName());

                // Print properties.
                Map map = res.getProperties();
                Iterator iter = map.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry) iter.next();
                    out.println("   " + entry.getKey() + " = " + entry.getValue());
                }

                // Print requirements.
                Requirement[] reqs = res.getRequirements();
                for (int i = 0; (reqs != null) && (i < reqs.length); i++)
                {
                    if (i == 0)
                    {
                        out.println("   requirements:");
                    }
                    out.println("      " + reqs[i].getFilter());
                }

                // Print capabilities.
                Capability[] caps = res.getCapabilities();
                for (int i = 0; (caps != null) && (i < caps.length); i++)
                {
                    if (i == 0)
                    {
                        out.println("   capabilities:");
                    }
                    out.println("      " + caps[i].getName() + " = " + caps[i].getProperties());
                }
            }
        }
    }

    private void createUserInterface()
    {
        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, createTree(), createConsole());
        split.setResizeWeight(1.0);
        split.setDividerSize(5);
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        createEventListeners();
    }

    private JPanel createTree()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createRepoPanel(), BorderLayout.NORTH);
        panel.add(createResourcePanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRepoPanel()
    {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Repositories"));
        panel.add(m_addRepoButton = new JButton("Add"));
        m_addRepoButton.setMnemonic('A');
        panel.add(m_removeRepoButton = new JButton("Remove"));
        m_removeRepoButton.setMnemonic('R');
        panel.add(m_refreshRepoButton = new JButton("Refresh"));
        m_refreshRepoButton.setMnemonic('f');
        m_removeRepoButton.setEnabled(false);
        m_refreshRepoButton.setEnabled(false);
        return panel;
    }

    private JPanel createResourcePanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Resources"));
        JScrollPane scroll = null;
        panel.add(
            scroll = new JScrollPane(
                m_bundleTree = new JTree(new SimpleTreeNode(null, null))), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        // Set table scroll pane to reasonable size.
        scroll.setPreferredSize(new Dimension(100, 100));
        m_bundleTree.setMinimumSize(new Dimension(0, 0));

        // We don't need to see the root.
        m_bundleTree.setRootVisible(false);
        m_bundleTree.setShowsRootHandles(true);

        return panel;
    }

    private JPanel createButtonPanel()
    {
        JPanel panel = new JPanel();
        panel.add(m_deployButton = new JButton(DEPLOY_BUTTON));
        panel.add(m_startButton = new JButton(START_BUTTON));
        panel.add(m_infoButton = new JButton("Info"));
        m_deployButton.setMnemonic('D');
        m_startButton.setMnemonic('S');
        m_infoButton.setMnemonic('I');
        m_infoButton.setEnabled(false);
        return panel;
    }

    private JPanel createConsole()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(m_soa, BorderLayout.CENTER);
        panel.add(m_clearButton = new JButton("Clear"), BorderLayout.EAST);
        m_clearButton.setMnemonic('C');

        return panel;
    }

    private static void printUnderline(PrintStream out, int length)
    {
        for (int i = 0; i < length; i++)
        {
            out.print('-');
        }
        out.println("");
    }

    private void initializeRootNode()
    {
        synchronized (m_createRootRunnable)
        {
            new Thread(m_createRootRunnable).start();
        }
    }

    private class CreateRootRunnable implements Runnable
    {
        public void run()
        {
            synchronized (OBRPlugin.this)
            {
                // HACK ALERT: This next if statement is a hack to force
                // the OBR service to retrieve its repository files on
                // this thread, rather than the Swing thread. This hack
                // assumes that this GUI is working with Felix' OBR service,
                // which defers retrieving repository URLs until needed.
                if (m_repoAdmin != null)
                {
                    m_repoAdmin.listRepositories();
                }

                // Create the new root node and then set it.
                m_rootNode = new SimpleTreeNode(null, m_repoAdmin);
                try
                {
                    SwingUtilities.invokeAndWait(m_setRootRunnable);
                }
                catch (Exception ex)
                {
                    // Ignore.
                }
            }
        }
    }

    private class SetRootRunnable implements Runnable
    {
        public void run()
        {
            ((DefaultTreeModel) m_bundleTree.getModel()).setRoot(m_rootNode);
        }
    }

    private static class SimpleTreeNode implements TreeNode
    {
        private TreeNode m_parent = null;
        private Object m_obj = null;
        private TreeNode[] m_children = null;
        private String m_toString = null;

        public SimpleTreeNode(TreeNode parent, Object obj)
        {
            m_parent = parent;
            m_obj = obj;
        }

        public Object getObject()
        {
            return m_obj;
        }

        public TreeNode getChildAt(int index)
        {
            if (m_children == null)
            {
                initialize();
            }

            if ((m_children != null) && (index >= 0) && (index < m_children.length))
            {
                return m_children[index];
            }

            return null;
        }

        public int getChildCount()
        {
            if (m_children == null)
            {
                initialize();
            }
            return (m_children == null) ? 0 : m_children.length;
        }

        public TreeNode getParent()
        {
            return m_parent;
        }

        public int getIndex(TreeNode node)
        {
            if (m_children == null)
            {
                initialize();
            }
            for (int i = 0; (m_children != null) && (i < m_children.length); i++)
            {
                if (m_children[i] == node)
                {
                    return i;
                }
            }
            return -1;
        }

        public boolean getAllowsChildren()
        {
            return true;
        }

        public boolean isLeaf()
        {
            return (getChildCount() == 0);
        }

        public Enumeration children()
        {
            return null;
        }

        private void initialize()
        {
            // The current node might be the root repository admin.
            if ((m_obj != null) && (m_obj instanceof RepositoryAdmin))
            {
                Object[] objs = ((RepositoryAdmin) m_obj).listRepositories();
                if (objs != null)
                {
                    m_children = new TreeNode[objs.length];
                    for (int i = 0; i < objs.length; i++)
                    {
                        m_children[i] = new SimpleTreeNode(this, objs[i]);
                    }
                }
            }
            else if (m_obj instanceof Repository)
            {
                Object[] objs = ((Repository) m_obj).getResources();
                if (objs != null)
                {
                    m_children = new TreeNode[objs.length];
                    for (int i = 0; i < objs.length; i++)
                    {
                        m_children[i] = new SimpleTreeNode(this, objs[i]);
                    }
                }
            }
        }

        public String toString()
        {
            if (m_toString == null)
            {
                if (m_obj instanceof RepositoryAdmin)
                {
                    m_toString = "ROOT";
                }
                else if (m_obj instanceof Repository)
                {
                    m_toString = ((Repository) m_obj).getName();
                }
                else if (m_obj instanceof Resource)
                {
                    m_toString = ((Resource) m_obj).getPresentationName()
                        + " (" + ((Resource) m_obj).getVersion() + ")";
                }
                else
                {
                    m_toString = (m_obj != null) ? m_obj.toString() : "null";
                }
            }
            return m_toString;
        }
    }
}