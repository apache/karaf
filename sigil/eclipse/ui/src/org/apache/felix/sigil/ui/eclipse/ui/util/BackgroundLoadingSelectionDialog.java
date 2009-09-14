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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.IJobRunnable;


public class BackgroundLoadingSelectionDialog<E> extends TitleAreaDialog implements IAccumulator<E>
{

    private final ILabelProvider DEFAULT_LABEL_PROVIDER = new LabelProvider()
    {
        @SuppressWarnings("unchecked")
        public String getText( Object element )
        {
            String result;
            if ( element instanceof WrappedContentProposal<?> )
            {
                WrappedContentProposal<E> contentProposal = (org.apache.felix.sigil.ui.eclipse.ui.util.WrappedContentProposal<E> ) element;
                result = contentProposal.getLabel();
            }
            else
            {
                result = descriptor.getLabel( ( E ) element );
            }
            return result;
        }
    };

    private final IElementDescriptor<E> DEFAULT_DESCRIPTOR = new IElementDescriptor<E>()
    {
        public String getLabel( E element )
        {
            return getName( element );
        }


        public String getName( E element )
        {
            return element == null ? "null" : element.toString();
        }
    };

    private final String selectionLabel;
    private IFilter<? super E> filter;
    private IElementDescriptor<? super E> descriptor = DEFAULT_DESCRIPTOR;
    private ILabelProvider labelProvider = DEFAULT_LABEL_PROVIDER;
    private final boolean multi;

    private final List<E> elements;

    private List<E> selection = null;
    private String selectedName = null;

    private TableViewer viewer = null;
    private Comparator<? super E> comparator;

    private HashMap<String, IJobRunnable> background = new HashMap<String, IJobRunnable>();


    public BackgroundLoadingSelectionDialog( Shell parentShell, String selectionLabel, boolean multi )
    {
        super( parentShell );
        elements = new ArrayList<E>();
        this.selectionLabel = selectionLabel;
        this.multi = multi;
    }


    public void setFilter( IFilter<? super E> filter )
    {
        this.filter = filter;
    }


    public void setDescriptor( final IElementDescriptor<? super E> descriptor )
    {
        if ( descriptor != null )
        {
            this.descriptor = descriptor;
        }
        else
        {
            this.descriptor = DEFAULT_DESCRIPTOR;
        }
    }


    public IElementDescriptor<? super E> getDescriptor()
    {
        return descriptor;
    }


    public void setComparator( Comparator<? super E> comparator )
    {
        this.comparator = comparator;
    }


    public void setLabelProvider( ILabelProvider labelProvider )
    {
        if ( labelProvider != null )
        {
            this.labelProvider = labelProvider;
        }
        else
        {
            this.labelProvider = DEFAULT_LABEL_PROVIDER;
        }
    }


    public void addBackgroundJob( String name, IJobRunnable job )
    {
        background.put( name, job );
    }


    @Override
    public int open()
    {
        Job[] jobs = scheduleJobs();
        try
        {
            return super.open();
        }
        finally
        {
            for ( Job j : jobs )
            {
                j.cancel();
            }
        }
    }


    private Job[] scheduleJobs()
    {
        if ( background.isEmpty() )
        {
            return new Job[]
                {};
        }
        else
        {
            ArrayList<Job> jobs = new ArrayList<Job>( background.size() );
            for ( Map.Entry<String, IJobRunnable> e : background.entrySet() )
            {
                final IJobRunnable run = e.getValue();
                Job job = new Job( e.getKey() )
                {
                    @Override
                    protected IStatus run( IProgressMonitor monitor )
                    {
                        return run.run( monitor );
                    }
                };
                job.schedule();
            }

            return jobs.toArray( new Job[jobs.size()] );
        }
    }


    @Override
    protected Control createDialogArea( Composite parent )
    {
        // Create Controls
        Composite container = ( Composite ) super.createDialogArea( parent );
        Composite composite = new Composite( container, SWT.NONE );

        new Label( composite, SWT.NONE ).setText( selectionLabel );

        ContentProposalAdapter proposalAdapter = null;
        Text txtSelection = null;

        Table table = null;
        if ( multi )
        {
            table = new Table( composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER );
            viewer = new TableViewer( table );
            viewer.setContentProvider( new ArrayContentProvider() );
            viewer.addFilter( new ViewerFilter()
            {
                public boolean select( Viewer viewer, Object parentElement, Object element )
                {
                    @SuppressWarnings("unchecked")
                    E castedElement = ( E ) element;
                    return filter == null || filter.select( castedElement );
                }
            } );
            if ( comparator != null )
            {
                viewer.setSorter( new ViewerSorter()
                {
                    @Override
                    public int compare( Viewer viewer, Object o1, Object o2 )
                    {
                        @SuppressWarnings("unchecked")
                        E e1 = ( E ) o1;
                        @SuppressWarnings("unchecked")
                        E e2 = ( E ) o2;
                        return comparator.compare( e1, e2 );
                    }
                } );
            }
            synchronized ( elements )
            {
                viewer.setInput( elements );
            }

            if ( labelProvider != null )
            {
                viewer.setLabelProvider( labelProvider );
            }
        }
        else
        {
            txtSelection = new Text( composite, SWT.BORDER );
            ControlDecoration selectionDecor = new ControlDecoration( txtSelection, SWT.LEFT | SWT.TOP );
            FieldDecoration proposalDecor = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_CONTENT_PROPOSAL );
            selectionDecor.setImage( proposalDecor.getImage() );
            selectionDecor.setDescriptionText( proposalDecor.getDescription() );

            ExclusionContentProposalProvider<E> proposalProvider = new ExclusionContentProposalProvider<E>( elements,
                filter, descriptor );

            proposalAdapter = new ContentProposalAdapter( txtSelection, new TextContentAdapter(), proposalProvider,
                null, null );
            proposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );
            if ( labelProvider != null )
            {
                proposalAdapter.setLabelProvider( labelProvider );
            }

            if ( selectedName != null )
            {
                txtSelection.setText( selectedName );
            }
        }
        updateSelection();
        updateButtons();

        // Hookup listeners
        if ( proposalAdapter != null )
        {
            proposalAdapter.addContentProposalListener( new IContentProposalListener()
            {
                public void proposalAccepted( IContentProposal proposal )
                {
                    @SuppressWarnings("unchecked")
                    WrappedContentProposal<E> valueProposal = (org.apache.felix.sigil.ui.eclipse.ui.util.WrappedContentProposal<E> ) proposal;
                    E selected = valueProposal.getElement();
                    selection = new ArrayList<E>( 1 );
                    selection.add( selected );

                    elementSelected( selected );

                    updateButtons();
                }
            } );
        }
        if ( txtSelection != null )
        {
            txtSelection.addModifyListener( new ModifyListener()
            {
                public void modifyText( ModifyEvent e )
                {
                    selectedName = ( ( Text ) e.widget ).getText();
                    updateButtons();
                }
            } );
        }
        if ( viewer != null )
        {
            viewer.addSelectionChangedListener( new ISelectionChangedListener()
            {
                public void selectionChanged( SelectionChangedEvent event )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) event.getSelection();
                    selection = new ArrayList<E>( sel.size() );
                    for ( Iterator<?> iter = sel.iterator(); iter.hasNext(); )
                    {
                        @SuppressWarnings("unchecked")
                        E element = ( E ) iter.next();
                        selection.add( element );
                    }
                    updateButtons();
                }
            } );
            viewer.addOpenListener( new IOpenListener()
            {
                public void open( OpenEvent event )
                {
                    if ( canComplete() )
                    {
                        setReturnCode( IDialogConstants.OK_ID );
                        close();
                    }
                }
            } );
        }

        // Layout
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        if ( multi )
        {
            composite.setLayout( new GridLayout( 1, false ) );
            GridData layoutTable = new GridData( SWT.FILL, SWT.FILL, true, true );
            layoutTable.heightHint = 200;
            table.setLayoutData( layoutTable );
        }
        else
        {
            composite.setLayout( new GridLayout( 2, false ) );
            txtSelection.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        }

        return container;
    }


    protected void elementSelected( E selection )
    {
    }


    @Override
    protected Control createButtonBar( Composite parent )
    {
        Control bar = super.createButtonBar( parent );
        updateButtons();
        return bar;
    }


    /**
     * Can be called from any thread
     */
    protected final void updateButtons()
    {
        Runnable updateButtonsRunnable = new Runnable()
        {
            public void run()
            {
                Shell shell = getShell();
                if ( shell != null && !shell.isDisposed() )
                {
                    Button okButton = getButton( IDialogConstants.OK_ID );
                    if ( okButton != null && !okButton.isDisposed() )
                    {
                        okButton.setEnabled( canComplete() );
                    }
                }
            }
        };
        Shell shell = getShell();
        if ( shell != null )
        {
            onUIThread( shell, updateButtonsRunnable );
        }
    }


    /**
     * Subclasses may override but must call super.canComplete
     * @return
     */
    protected synchronized boolean canComplete()
    {
        boolean result = false;

        if ( selection != null )
        {
            if ( multi )
            {
                result = selection.size() > 0;
            }
            else
            {
                E sel = getSelectedElement();
                result = sel != null && descriptor.getName( sel ).equals( selectedName );
            }
        }

        return result;
    }


    public final void addElement( E added )
    {
        addElements( Collections.singleton( added ) );
    }


    /**
     * Can be called from any thread
     */
    public final void addElements( Collection<? extends E> added )
    {
        final LinkedList<E> toAdd = new LinkedList<E>();
        synchronized ( elements )
        {
            for ( E e : added )
            {
                if ( !elements.contains( e ) )
                {
                    elements.add( e );
                    toAdd.add( e );
                }
            }
            Collections.sort( elements, comparator );
        }
        if ( viewer != null )
        {
            onUIThread( viewer.getControl(), new Runnable()
            {
                public void run()
                {
                    if ( !viewer.getControl().isDisposed() )
                    {
                        viewer.add( toAdd.toArray() );
                        viewer.refresh();
                    }
                }
            } );
        }
        else
        {

        }
        updateSelection();
        updateButtons();
    }


    protected void updateSelection()
    {
        onUIThread( getShell(), new Runnable()
        {
            public void run()
            {
                if ( selectedName != null )
                {
                    ArrayList<E> newSelection = new ArrayList<E>();
                    synchronized ( elements )
                    {
                        for ( E e : elements )
                        {
                            if ( selectedName.equals( descriptor.getName( e ) ) )
                            {
                                newSelection.add( e );
                                break;
                            }
                        }
                    }
                    selection = newSelection;
                }
                else
                {
                    selection = Collections.emptyList();
                }
                if ( viewer != null && !viewer.getControl().isDisposed() )
                {
                    viewer.setSelection( selection.isEmpty() ? StructuredSelection.EMPTY : new StructuredSelection(
                        selection ) );
                }
            }
        } );
    }


    private static final void onUIThread( Control control, Runnable r )
    {
        if ( control != null && !control.isDisposed() )
        {
            try
            {
                Display display = control.getDisplay();
                if ( Thread.currentThread() == display.getThread() )
                {
                    // We are on the UI thread already, just do the work
                    r.run();
                }
                else
                {
                    // Not on the UI thread, need to bung over the runnable
                    display.asyncExec( r );
                }
            }
            catch ( SWTError e )
            {
                if ( e.code == SWT.ERROR_WIDGET_DISPOSED )
                {
                    // ignore
                }
                else
                {
                    throw e;
                }
            }
        }
    }


    public String getSelectedName()
    {
        return selectedName;
    }


    public void setSelectedName( String selectedName )
    {
        this.selectedName = selectedName;
        boolean change = false;
        if ( selectedName == null )
        {
            if ( selection != null && !selection.isEmpty() )
            {
                change = true;
            }
        }
        else
        {
            if ( selection == null )
            {
                change = true;
            }
            else if ( selection.size() != 1 || !descriptor.getLabel( selection.get( 0 ) ).equals( selectedName ) )
            {
                change = true;
            }
        }

        if ( change )
        {
            updateSelection();
            updateButtons();
        }
    }


    public List<E> getSelectedElements()
    {
        return selection;
    }


    public E getSelectedElement()
    {
        E result;
        if ( selection == null || selection.isEmpty() )
        {
            result = null;
        }
        else
        {
            result = selection.get( 0 );
        }
        return result;
    }
}
