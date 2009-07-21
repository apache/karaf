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

package org.apache.felix.sigil.ui.eclipse.ui.util;


import java.util.Collection;

import org.apache.felix.sigil.model.IModelElement;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;


public class ResourceReviewDialog<T extends IModelElement> extends TitleAreaDialog
{

    private String title;
    private Collection<T> resources;

    private TableViewer viewer;


    public ResourceReviewDialog( Shell parentShell, String title, Collection<T> resources )
    {
        super( parentShell );
        this.title = title;
        this.resources = resources;
    }


    public Collection<T> getResources()
    {
        return resources;
    }


    @Override
    protected Control createDialogArea( Composite parent )
    {
        setTitle( title );

        // Create controls
        Composite container = ( Composite ) super.createDialogArea( parent );
        Composite composite = new Composite( container, SWT.NONE );
        Table table = new Table( composite, SWT.BORDER | SWT.VIRTUAL );

        final Button remove = new Button( composite, SWT.PUSH );
        remove.setText( "Remove" );
        remove.setEnabled( false );

        remove.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                handleRemove();
            }
        } );

        viewer = new TableViewer( table );
        viewer.setContentProvider( new DefaultTableProvider()
        {
            public Object[] getElements( Object inputElement )
            {
                return toArray( inputElement );
            }
        } );

        viewer.setInput( resources );
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                remove.setEnabled( !event.getSelection().isEmpty() );
            }
        } );

        // layout
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        composite.setLayout( new GridLayout( 2, false ) );
        GridData tableLayoutData = new GridData( SWT.FILL, SWT.FILL, true, true, 1, 4 );
        tableLayoutData.heightHint = 150;
        table.setLayoutData( tableLayoutData );

        return container;
    }


    private void handleRemove()
    {
        ISelection s = viewer.getSelection();
        if ( !s.isEmpty() )
        {
            IStructuredSelection sel = ( IStructuredSelection ) s;
            resources.remove( sel.getFirstElement() );
            viewer.refresh();
        }
    }
}
