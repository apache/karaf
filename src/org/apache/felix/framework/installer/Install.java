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
package org.apache.felix.framework.installer;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import org.apache.felix.framework.installer.artifact.*;
import org.apache.felix.framework.installer.editor.BooleanEditor;
import org.apache.felix.framework.installer.editor.FileEditor;
import org.apache.felix.framework.installer.property.*;
import org.apache.felix.framework.util.FelixConstants;

public class Install extends JFrame
{
    private static transient final String PROPERTY_FILE = "property.xml";
    private static transient final String ARTIFACT_FILE = "artifact.xml";

    public static transient final String JAVA_DIR = "Java directory";
    public static transient final String INSTALL_DIR = "Install directory";

    private PropertyPanel m_propPanel = null;
    private JButton m_okayButton = null;
    private JButton m_cancelButton = null;
    private JLabel m_statusLabel = null;

    private java.util.List m_propList = null;
    private java.util.List m_artifactList = null;

    public Install()
        throws Exception
    {
        super("Install");

        // Load properties before resources, because resources
        // refer to properties.
        m_propList = loadPropertyList();
        m_artifactList = loadArtifactList();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(
            m_propPanel = new PropertyPanel(m_propList), BorderLayout.CENTER);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(true);
        centerWindow(this);

        // Make window closeable.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event)
            {
                doCancel();
            }
        });
    }

    public java.util.List loadPropertyList()
    {
        String installDir = System.getProperty("user.home");
        if (!installDir.endsWith(File.separator))
        {
            installDir = installDir + File.separator;
        }

        Property prop = null;

        // Eventually these should be read from a file.
        java.util.List list = new ArrayList();

        // Add the impl choice property.
        prop = new BooleanPropertyImpl("Shell", true);
        prop.setEditor(new BooleanEditor((BooleanProperty) prop, "Text", "GUI"));
        list.add(prop);

        // Add the java directory property.
        prop = new StringPropertyImpl(JAVA_DIR, System.getProperty("java.home"));
        prop.setEditor(new FileEditor((StringProperty) prop, true));
        list.add(prop);

        // Add the installation directory property.
        prop = new StringPropertyImpl(INSTALL_DIR, installDir + "Felix");
        prop.setEditor(new FileEditor((StringProperty) prop, true));
        list.add(prop);

        // Add the documentation URL property.
        prop = new BooleanStringPropertyImpl(
            "User documentation",
            true,
            "http://download.forge.objectweb.org/oscar/oscar-doc-"
            + FelixConstants.FELIX_VERSION_VALUE + ".jar");
        list.add(prop);

        // Add the documentation URL property.
        prop = new BooleanStringPropertyImpl(
            "API documentation",
            true,
            "http://download.forge.objectweb.org/oscar/oscar-api-"
            + FelixConstants.FELIX_VERSION_VALUE + ".jar");
        list.add(prop);

        return list;
    }

    public java.util.List loadArtifactList() throws Exception
    {
        // Eventually I will changed these to be read from a file.
        java.util.List list = new ArrayList();
        list.add(
            new ArtifactHolder(
                (BooleanProperty) getProperty("User documentation"),
                new URLJarArtifact(
                    (StringProperty) getProperty("User documentation"))));
        list.add(
            new ArtifactHolder(
                (BooleanProperty) getProperty("API documentation"),
                new URLJarArtifact(
                    (StringProperty) getProperty("API documentation"))));
        list.add(
            new ArtifactHolder(
                new ResourceJarArtifact(
                    new StringPropertyImpl("sourceName", "package.jar"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "src.jar"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "LICENSE.txt"))));
        list.add(
            new ArtifactHolder(
                (BooleanProperty) getProperty("Shell"),
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "config.properties.text"),
                    new StringPropertyImpl("destName", "config.properties"),
                    new StringPropertyImpl("destDir", "lib"))));
        list.add(
            new ArtifactHolder(
                new NotBooleanPropertyImpl((BooleanProperty) getProperty("Shell")),
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "config.properties.gui"),
                    new StringPropertyImpl("destName", "config.properties"),
                    new StringPropertyImpl("destDir", "lib"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "example.policy"))));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "felix.bat"),
                    new StringPropertyImpl("destName" , "felix.bat"),
                    new StringPropertyImpl("destDir", ""),
                    true)));
        list.add(
            new ArtifactHolder(
                new ResourceFileArtifact(
                    new StringPropertyImpl("sourceName", "felix.sh"),
                    new StringPropertyImpl("destName" , "felix.sh"),
                    new StringPropertyImpl("destDir", ""),
                    true)));

        return list;
    }

    private Property getProperty(String name)
    {
        for (int i = 0; i < m_propList.size(); i++)
        {
            Property prop = (Property) m_propList.get(i);
            if (prop.getName().equals(name))
            {
                return prop;
            }
        }
        return null;
    }

    protected void doOkay()
    {
        m_propPanel.setEnabled(false);
        m_okayButton.setEnabled(false);
        m_cancelButton.setEnabled(false);
        new Thread(new InstallRunnable()).start();
    }

    protected void doCancel()
    {
        System.exit(0);
    }

    protected JPanel createButtonPanel()
    {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Create and set layout.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        buttonPanel.setLayout(grid);

        // Create labels and fields.
        c.insets = new Insets(2, 2, 2, 2);

        // Okay button.
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        grid.setConstraints(m_okayButton = new JButton("OK"), c);
        buttonPanel.add(m_okayButton);
        m_okayButton.setDefaultCapable(true);
        getRootPane().setDefaultButton(m_okayButton);

        // Cancel button.
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        grid.setConstraints(m_cancelButton = new JButton("Cancel"), c);
        buttonPanel.add(m_cancelButton);

        // Add action listeners.
        m_okayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                doOkay();
            }
        });

        m_cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                doCancel();
            }
        });

        // Status label.
        m_statusLabel = new JLabel("Felix installation");
        m_statusLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        // Complete panel.
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(m_statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    public static void centerWindow(Component window)
    {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dim = toolkit.getScreenSize();
        int screenWidth = dim.width;
        int screenHeight = dim.height;
        int x = (screenWidth - window.getSize().width) / 2;
        int y = (screenHeight - window.getSize().height) / 2;
        window.setLocation(x, y);
    }

    public static void main(String[] argv) throws Exception
    {
        String msg = "<html>"
            + "<center><h1>Felix " + FelixConstants.FELIX_VERSION_VALUE + "</h1></center>"
            + "You can download example bundles at the Felix impl prompt by<br>"
            + "using the <b><tt>obr</tt></b> command to access the OSGi Bundle Repository;<br>"
            + "type <b><tt>obr help</tt></b> at the Felix impl prompt for details."
            + "</html>";
        JLabel label = new JLabel(msg);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        final JDialog dlg = new JDialog((Frame) null, "Felix Install", true);
        dlg.getContentPane().setLayout(new BorderLayout(10, 10));
        dlg.getContentPane().add(label, BorderLayout.CENTER);
        JPanel panel = new JPanel();
        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                dlg.hide();
            }
        });
        panel.add(button);
        dlg.getContentPane().add(panel, BorderLayout.SOUTH);
        // For spacing purposes...
        dlg.getContentPane().add(new JPanel(), BorderLayout.NORTH);
        dlg.getContentPane().add(new JPanel(), BorderLayout.EAST);
        dlg.getContentPane().add(new JPanel(), BorderLayout.WEST);
        dlg.pack();
        centerWindow(dlg);
        dlg.show();

        Install obj = new Install();
        obj.setVisible(true);
    }

    class InstallRunnable implements Runnable
    {
        public void run()
        {
            Map propMap = new HashMap();
            for (int i = 0; i < m_propList.size(); i++)
            {
                Property prop = (Property) m_propList.get(i);
                propMap.put(prop.getName(), prop);
            }

            String installDir = ((StringProperty) propMap.get(INSTALL_DIR)).getStringValue();

            // Make sure the install directory ends with separator char.
            if (!installDir.endsWith(File.separator))
            {
                installDir = installDir + File.separator;
            }

            // Make sure the install directory exists and
            // that is actually a directory.
            File file = new File(installDir);
            if (!file.exists())
            {
                if (!file.mkdirs())
                {
                    JOptionPane.showMessageDialog(Install.this,
                        "Unable to create install directory.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(-1);
                }
            }
            else if (!file.isDirectory())
            {
                JOptionPane.showMessageDialog(Install.this,
                    "The selected install location is not a directory.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }

            // Status updater runnable.
            StatusRunnable sr = new StatusRunnable();

            // Loop through and process resources.
            for (int i = 0; i < m_artifactList.size(); i++)
            {
                ArtifactHolder ah = (ArtifactHolder) m_artifactList.get(i);
                if (ah.isIncluded())
                {
                    if (!ah.getArtifact().process(sr, propMap))
                    {
                        JOptionPane.showMessageDialog(Install.this,
                            "An error occurred while processing the resources.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(-1);
                    }
                }
            }

            System.exit(0);
        }
    }

    class StatusRunnable implements Status, Runnable
    {
        private String text = null;

        public void setText(String s)
        {
            text = s;
            try {
                SwingUtilities.invokeAndWait(this);
            } catch (Exception ex) {
                // Ignore.
            }
        }

        public void run()
        {
            m_statusLabel.setText(text);
        }
    }

    // Re-usable static member for ResourceHolder inner class.
    private static BooleanProperty m_trueProp =
        new BooleanPropertyImpl("mandatory", true);

    class ArtifactHolder
    {
        private BooleanProperty m_isIncluded = null;
        private Artifact m_artifact = null;
        
        public ArtifactHolder(Artifact artifact)
        {
            this(m_trueProp, artifact);
        }
        
        public ArtifactHolder(BooleanProperty isIncluded, Artifact artifact)
        {
            m_isIncluded = isIncluded;
            m_artifact = artifact;
        }
        
        public boolean isIncluded()
        {
            return m_isIncluded.getBooleanValue();
        }
        
        public Artifact getArtifact()
        {
            return m_artifact;
        }
    }
}
