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


import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.ui.eclipse.ui.util.IElementDescriptor;
import org.apache.felix.sigil.ui.eclipse.ui.util.IFilter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;


/**
 * @author dave
 *
 */
public class OSGiConfigurationView extends Composite
{
    private Job job;
    private FilteredModelView<ISigilBundle> available;
    private FilteredModelView<ISigilBundle> selected;

    /**
     * @param parent
     * @param osGiLaunchConfigurationTab
     */
    public OSGiConfigurationView( Composite parent, OSGiLaunchConfigurationTab osGiLaunchConfigurationTab )
    {
        super( parent, SWT.NONE );
        initLayout();
    }


    @Override
    public void dispose()
    {
        if ( job != null ) {
            job.cancel();
        }
        
        super.dispose();
    }


    /**
     * 
     */
    private void initLayout()
    {
        // components
        Composite left = new Composite( this, SWT.NONE );
        Composite middle = new Composite(this, SWT.NONE);
        Composite right = new Composite(this, SWT.NONE);

        available = new FilteredModelView<ISigilBundle>(left, SWT.NONE);
        selected = new FilteredModelView<ISigilBundle>(right, SWT.NONE);
        
        Button addBtn = new Button( middle, SWT.PUSH );
        addBtn.setText( "->" );
        Button removeBtn = new Button( middle, SWT.PUSH );
        removeBtn.setText( "<-" );
        
        // customisations
        addBtn.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                handleAdd();
            }
        } );
        
        removeBtn.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                handleRemove();
            }
        } );
        
        IElementDescriptor<ISigilBundle> descriptor = new IElementDescriptor<ISigilBundle>()
        {
            public String getLabel( ISigilBundle element )
            {
                return element.getSymbolicName() + " version " + element.getVersion();
            }


            public String getName( ISigilBundle element )
            {
                return element.getSymbolicName();
            }
        };
        
        available.setElementDescriptor( descriptor );
        selected.setElementDescriptor( descriptor );
        
        available.setFilter( new IFilter<ISigilBundle>() {
            public boolean select( ISigilBundle element )
            {
                return !selected.getElements().contains( element );
            }
        } );
        
        // layout
        available.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        selected.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        
        left.setLayout( new GridLayout( 1, false ) );
        middle.setLayout( new GridLayout( 1, false ) );
        right.setLayout( new GridLayout( 1, false ) );

        left.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        middle.setLayoutData( new GridData( SWT.FILL, SWT.FILL, false, true ) );
        right.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
        
        setLayout( new GridLayout( 3, false ) );
        
        startSearch(available);
    }

    private void startSearch(final FilteredModelView<ISigilBundle> view) {
        job = new Job( "Finding bundles" )
        {
            @Override
            protected IStatus run( final IProgressMonitor monitor )
            {
                SigilCore.getGlobalRepositoryManager().visit( new IModelWalker()
                {
                    public boolean visit( IModelElement element )
                    {
                        if ( element instanceof ISigilBundle )
                        {
                            ISigilBundle sb = ( ISigilBundle ) element;
                            
                            view.addElement( sb );
                            
                            return false;
                        }
                        else
                        {
                            return !monitor.isCanceled();
                        }
                    }
                } );

                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        };
        job.schedule();
    }


    private void handleAdd()
    {
        selected.addElements( available.getSelectedElements() );
        available.refresh();
    }

    private void handleRemove()
    {
        selected.removeElements( selected.getSelectedElements() );
        available.refresh();
    }


}
