package org.apache.felix.sigil.eclipse.runtime.config;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultLabelProvider;
import org.apache.felix.sigil.ui.eclipse.ui.util.IElementDescriptor;
import org.apache.felix.sigil.ui.eclipse.ui.util.IFilter;
import org.apache.felix.sigil.ui.eclipse.ui.util.UIHelper;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;


public class FilteredModelView<T> extends Composite
{
    private ArrayList<T> selected = new ArrayList<T>();
    private ArrayList<T> elements = new ArrayList<T>();
    private IElementDescriptor<T> elementDescriptor = UIHelper.getDefaultElementDescriptor();
    private IFilter<T> filter = UIHelper.getDefaultFilter();
    private StructuredViewer viewer;
    private String txt = "";

    public FilteredModelView( Composite parent, int style )
    {
        super( parent, style );
        initLayout();
    }

    public List<T> getSelectedElements()
    {
        return selected;
    }


    public List<T> getElements()
    {
        return elements;
    }

    public void addElement( T element )
    {
        elements.add( element );
        refresh();
    }


    public void addElements( Collection<T> elements )
    {
        this.elements.addAll( elements );
        refresh();
    }


    public void removeElement( T element )
    {
        elements.remove( element );
        refresh();
    }


    public void removeElements( Collection<T> elements )
    {
        this.elements.removeAll( elements );
        refresh();
    }

    public void refresh()
    {
        SigilUI.runInUI( new Runnable() {

            public void run()
            {
                viewer.refresh();
            }
        });
    }

    private void initLayout()
    {
        Text bundleTxt = createSelectionBox( this );

        Control view = createViewBox( this );

        // layout
        bundleTxt.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        view.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        setLayout( new GridLayout( 1, false ) );
    }


    private Control createViewBox( Composite parent )
    {
        Table table = new Table( this, SWT.MULTI );

        viewer = createViewer( table );

        viewer.setContentProvider( new ArrayContentProvider() );

        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            @SuppressWarnings("unchecked")
            public void selectionChanged( SelectionChangedEvent event )
            {
                if ( event.getSelection().isEmpty() )
                {
                    selected.clear();
                }
                else
                {
                    IStructuredSelection sel = ( IStructuredSelection ) event.getSelection();
                    selected.addAll( sel.toList() );
                }
            }
        } );
        
        viewer.setInput( elements );

        viewer.setFilters( new ViewerFilter[]
            { new ViewerFilter()
            {
                @SuppressWarnings("unchecked")
                @Override
                public boolean select( Viewer viewer, Object parentElement, Object element )
                {
                    if ( filter.select( ( T ) element ) )
                    {
                        String name = elementDescriptor.getName( ( T ) element );
                        return name.startsWith( txt );
                    }
                    else
                    {
                        return false;
                    }
                }
            } } );

        return table;
    }


    protected StructuredViewer createViewer( Table table )
    {
        TableViewer tableViewer = new TableViewer( table );
        
        tableViewer.setLabelProvider( new DefaultLabelProvider() {

            public Image getImage( Object arg0 )
            {
                return null;
            }

            @SuppressWarnings("unchecked")
            public String getText( Object element )
            {
                return elementDescriptor.getLabel( ( T ) element );
            }
            
        });

        return tableViewer;
    }


    private Text createSelectionBox( Composite parent )
    {
        final Text txtSelection = new Text( parent, SWT.SEARCH );

        txtSelection.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyReleased( KeyEvent e )
            {
                txt = txtSelection.getText();
                refresh();
            }
        } );
        return txtSelection;
    }


    /*
     *         ControlDecoration selectionDecor = new ControlDecoration( txtSelection, SWT.LEFT | SWT.TOP );
        FieldDecoration proposalDecor = FieldDecorationRegistry.getDefault().getFieldDecoration(
            FieldDecorationRegistry.DEC_CONTENT_PROPOSAL );
        selectionDecor.setImage( proposalDecor.getImage() );
        selectionDecor.setDescriptionText( proposalDecor.getDescription() );

        ExclusionContentProposalProvider<T> proposalProvider = new ExclusionContentProposalProvider<T>(
            elements, filter, elementDescriptor );

        ContentProposalAdapter proposalAdapter = new ContentProposalAdapter( txtSelection, new TextContentAdapter(),
            proposalProvider, null, null );
        
        proposalAdapter.addContentProposalListener( new IContentProposalListener() {
            public void proposalAccepted( IContentProposal proposal )
            {
                WrappedContentProposal<T> valueProposal = (org.apache.felix.sigil.ui.eclipse.ui.util.WrappedContentProposal<T> ) proposal;
                T selected = valueProposal.getElement();
                selection = new ArrayList<E>( 1 );
                selection.add( selected );
            }            
        });
        
        proposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );

     */

    public void setElementDescriptor( IElementDescriptor<T> elementDescriptor )
    {
        if ( elementDescriptor == null )
        {
            elementDescriptor = UIHelper.getDefaultElementDescriptor();
        }
        this.elementDescriptor = elementDescriptor;
    }
    
    public IElementDescriptor<T> getElementDescriptor() {
        return elementDescriptor;
    }


    public void setFilter( IFilter<T> filter )
    {
        if ( filter == null )
        {
            filter = UIHelper.getDefaultFilter();
        }
        this.filter = filter;
    }


}
