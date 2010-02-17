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


import org.apache.felix.sigil.common.osgi.VersionTable;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;


public class PackageExportDialog extends ResourceSelectDialog
{

    private Text versionText;
    private Version version;


    public PackageExportDialog( Shell parentShell, String title, IContentProvider content, ViewerFilter filter,
        Object scope )
    {
        super( parentShell, content, filter, scope, title, "Package Name:", true );
    }


    @Override
    protected void createCustom( Composite body )
    {
        Label l = new Label( body, SWT.NONE );
        l.setText( "Version:" );
        versionText = new Text( body, SWT.BORDER );
        versionText.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased( KeyEvent e )
            {
                try
                {
                    version = VersionTable.getVersion( versionText.getText() );
                    setErrorMessage( null );
                }
                catch ( IllegalArgumentException ex )
                {
                    setErrorMessage( "Invalid version" );
                }
            }
        } );
        if ( version != null )
        {
            versionText.setText( version.toString() );
        }
    }


    public Version getVersion()
    {
        return version;
    }


    public void setVersion( Version version )
    {
        this.version = version;
    }
}
