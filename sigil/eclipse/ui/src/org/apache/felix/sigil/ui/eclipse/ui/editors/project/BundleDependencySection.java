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


import java.util.Set;

import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilSection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


public abstract class BundleDependencySection extends SigilSection
{

    protected ProjectTableViewer viewer;
    private Button add;
    private Button edit;
    private Button remove;


    public BundleDependencySection( SigilPage page, Composite parent, ISigilProjectModel project,
        Set<IModelElement> unresolvedElements ) throws CoreException
    {
        super( page, parent, project );
        viewer.setUnresolvedElements( unresolvedElements );
    }


    public BundleDependencySection( SigilPage page, Composite parent, ISigilProjectModel project ) throws CoreException
    {
        this( page, parent, project, null );
    }


    protected abstract String getTitle();


    protected abstract Label createLabel( Composite parent, FormToolkit toolkit );


    protected abstract IContentProvider getContentProvider();


    protected abstract void handleAdd();


    protected abstract void handleEdit();


    protected abstract void handleRemoved();


    @Override
    public void refresh()
    {
        super.refresh();
        viewer.refresh();
    }


    protected ISelection getSelection()
    {
        return viewer.getSelection();
    }


    @Override
    protected void createSection( Section section, FormToolkit toolkit )
    {
        setTitle( getTitle() );

        Composite body = createGridBody( 2, false, toolkit );

        Label label = createLabel( body, toolkit );

        label.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, true, 2, 1 ) );

        Table bundleTable = toolkit.createTable( body, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.BORDER );
        GridData data = new GridData( GridData.FILL_BOTH );
        data.heightHint = 600;
        bundleTable.setLayoutData( data );
        bundleTable.setLinesVisible( false );
        createButtons( body, toolkit );
        createViewer( bundleTable );
    }


    protected void createButtons( Composite body, FormToolkit toolkit )
    {
        Composite buttons = toolkit.createComposite( body );
        TableWrapLayout layout = new TableWrapLayout();
        layout.numColumns = 1;
        layout.topMargin = 0;
        layout.leftMargin = 0;
        layout.rightMargin = 0;
        layout.bottomMargin = 0;
        buttons.setLayout( layout );
        GridData data = new GridData();
        data.verticalAlignment = SWT.TOP;
        buttons.setLayoutData( data );

        add = toolkit.createButton( buttons, "Add", SWT.NULL );
        add.setLayoutData( new TableWrapData( TableWrapData.FILL ) );
        add.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleAdd();
            }
        } );

        edit = toolkit.createButton( buttons, "Edit", SWT.NULL );
        edit.setLayoutData( new TableWrapData( TableWrapData.FILL ) );
        edit.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleEdit();
            }
        } );

        remove = toolkit.createButton( buttons, "Remove", SWT.NULL );
        remove.setLayoutData( new TableWrapData( TableWrapData.FILL ) );
        remove.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                handleRemoved();
            }
        } );

        setSelected( false );
    }


    protected void createViewer( Table bundleTable )
    {
        viewer = new ProjectTableViewer( bundleTable );
        viewer.setContentProvider( getContentProvider() );
        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                setSelected( !event.getSelection().isEmpty() );
            }
        } );
    }


    private void setSelected( boolean selected )
    {
        edit.setEnabled( selected );
        remove.setEnabled( selected );
    }

}