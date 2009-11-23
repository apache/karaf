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

package org.apache.felix.sigil.eclipse.runtime.config;

import java.net.URL;


import org.apache.felix.sigil.common.runtime.BundleForm;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.runtime.LaunchHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author dave
 *
 */
public class OSGiLaunchConfigurationTab extends AbstractLaunchConfigurationTab
{

    private Text formText;
    private String formLocation;

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName()
    {
        return "OSGi";
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl( Composite parent )
    {
        Composite configurationView = new Composite(parent, SWT.NONE);
        new Label(configurationView, SWT.NONE).setText("Form");

        // components
        formText = new Text(configurationView, SWT.BORDER);

        // layout
        formText.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );
        
        formText.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                updateLocation();
            }
        });
        
        Button browse = new Button(configurationView, SWT.PUSH);
        browse.setText("Browse");
        
        browse.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FormSelectionDialog dialog =
                    new FormSelectionDialog(getShell());
                if ( dialog.open() == Window.OK ) {
                    formLocation = dialog.getFormFile().getFullPath().toOSString();
                    formText.setText(formLocation);
                    updateLocation();
                }
            }
        });
        
        
        configurationView.setLayout( new GridLayout( 3, false ) );

        setControl(configurationView);
    }

    private void updateLocation()
    {
        String loc = formText.getText();
        if ( loc.trim().length() > 0 ) {
            try
            {
                URL url = LaunchHelper.toURL(loc);
                SigilCore.log("Resolving " + url);
                BundleForm.create(url);
                setErrorMessage(null);
                setDirty(true);
            }
            catch (Exception e)
            {
                SigilCore.warn("Failed to resolve bundle form", e);
                setErrorMessage("Invalid form file " + e.getMessage() );
            }
        }
        else {
            setErrorMessage("Missing form file");
        }
        updateLaunchConfigurationDialog();
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom( ILaunchConfiguration config )
    {
        try
        {
            formLocation = config.getAttribute(OSGiLaunchConfigurationConstants.FORM_FILE_LOCATION, "");
            formText.setText(formLocation);
        }
        catch (CoreException e)
        {
            SigilCore.error("Failed to initialise launch configuration view", e);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply( ILaunchConfigurationWorkingCopy config )
    {
        config.setAttribute(OSGiLaunchConfigurationConstants.FORM_FILE_LOCATION, formLocation);
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, OSGiLaunchConfigurationConstants.CLASSPATH_PROVIDER );
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, OSGiLaunchConfigurationConstants.CLASSPATH_PROVIDER);    
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults( ILaunchConfigurationWorkingCopy config )
    {
        config.setAttribute(OSGiLaunchConfigurationConstants.FORM_FILE_LOCATION, (String) null);
    }
}
