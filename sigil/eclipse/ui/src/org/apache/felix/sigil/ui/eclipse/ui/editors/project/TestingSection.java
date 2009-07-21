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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;


import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilSection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;


public class TestingSection extends SigilSection
{

    public TestingSection( SigilPage page, Composite parent, ISigilProjectModel project ) throws CoreException
    {
        super( page, parent, project );
    }


    protected void createSection( Section section, FormToolkit toolkit )
    {
        setTitle( "Testing" );

        Composite body = createTableWrapBody( 1, toolkit );

        toolkit.createLabel( body, "Test this project by launching a separate Newton application:" );

        Hyperlink launch = toolkit.createHyperlink( body, "Launch a newton container", SWT.NULL );
        launch.setHref( "launchShortcut.run.org.cauldron.sigil.launching.shortcut" );
        launch.addHyperlinkListener( this );

        Hyperlink debug = toolkit.createHyperlink( body, "Debug a newton container", SWT.NULL );
        debug.setHref( "launchShortcut.debug.org.cauldron.sigil.launching.shortcut" );
        debug.addHyperlinkListener( this );
    }


    public void linkActivated( HyperlinkEvent e )
    {
        String href = ( String ) e.getHref();
        if ( href.startsWith( "launchShortcut." ) ) { //$NON-NLS-1$
            href = href.substring( 15 );
            int index = href.indexOf( '.' );
            if ( index < 0 )
                return; // error.  Format of href should be launchShortcut.<mode>.<launchShortcutId>
            String mode = href.substring( 0, index );
            String id = href.substring( index + 1 );

            //getEditor().doSave(null);

            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IConfigurationElement[] elements = registry
                .getConfigurationElementsFor( "org.eclipse.debug.ui.launchShortcuts" ); //$NON-NLS-1$
            for ( int i = 0; i < elements.length; i++ )
            {
                if ( id.equals( elements[i].getAttribute( "id" ) ) ) //$NON-NLS-1$
                    try
                    {
                        ILaunchShortcut shortcut = ( ILaunchShortcut ) elements[i].createExecutableExtension( "class" ); //$NON-NLS-1$
                        // preLaunch();
                        shortcut.launch( new StructuredSelection( getLaunchObject() ), mode );
                    }
                    catch ( CoreException e1 )
                    {
                        e1.printStackTrace();
                    }
            }
        }
    }


    private Object getLaunchObject()
    {
        return getProjectModel().getProject();
    }


    public void linkEntered( HyperlinkEvent e )
    {
    }


    public void linkExited( HyperlinkEvent e )
    {
    }
}
