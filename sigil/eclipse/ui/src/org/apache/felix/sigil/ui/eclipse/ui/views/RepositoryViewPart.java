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

package org.apache.felix.sigil.ui.eclipse.ui.views;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.util.ModelHelper;
import org.apache.felix.sigil.model.ICompoundModelElement;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryChangeListener;
import org.apache.felix.sigil.repository.IRepositoryVisitor;
import org.apache.felix.sigil.repository.RepositoryChangeEvent;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.apache.felix.sigil.ui.eclipse.ui.util.DefaultTreeContentProvider;
import org.apache.felix.sigil.ui.eclipse.ui.util.ModelLabelProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;


public class RepositoryViewPart extends ViewPart implements IRepositoryChangeListener
{

    public class FindUsersAction extends Action
    {
        @Override
        public String getText()
        {
            return "Find Uses";
        }


        @Override
        public void run()
        {
            ISelection s = treeViewer.getSelection();
            if ( !s.isEmpty() )
            {
                IStructuredSelection sel = ( IStructuredSelection ) s;
                IModelElement e = ( IModelElement ) sel.getFirstElement();
                List<IModelElement> users = ModelHelper.findUsers( e );
                String msg = null;
                if ( users.isEmpty() )
                {
                    msg = "No users of " + e;
                }
                else
                {
                    StringBuilder b = new StringBuilder();
                    for ( IModelElement u : users )
                    {
                        ISigilBundle bndl = u.getAncestor( ISigilBundle.class );
                        b.append( bndl );
                        b.append( "->" );
                        b.append( u );
                        b.append( "\n" );
                    }
                    msg = b.toString();
                }
                MessageDialog.openInformation( getViewSite().getShell(), "Information", msg );
            }
        }
    }

    class RepositoryAction extends Action
    {
        final IBundleRepository rep;
        final IRepositoryModel model;


        public RepositoryAction( IBundleRepository rep )
        {
            this.rep = rep;
            this.model = SigilCore.getRepositoryConfiguration().findRepository( rep.getId() );
        }


        @Override
        public void run()
        {
            treeViewer.setInput( rep );
            createMenu();
        }


        @Override
        public String getText()
        {
            String name = model.getName();
            if ( treeViewer.getInput() == rep )
            {
                name = "> " + name;
            }
            return name;
        }


        @Override
        public ImageDescriptor getImageDescriptor()
        {
            Image img = model.getType().getIcon();
            if ( img == null )
            {
                return ImageDescriptor.createFromFile( RepositoryViewPart.class, "/icons/repository.gif" );
            }
            else
            {
                return ImageDescriptor.createFromImage( img );
            }
        }
    }

    class RefreshAction extends Action
    {
        @Override
        public void run()
        {
            IBundleRepository rep = ( IBundleRepository ) treeViewer.getInput();
            if ( rep != null )
            {
                rep.refresh();
                treeViewer.refresh();
            }
        }


        @Override
        public String getText()
        {
            return "Refresh";
        }


        @Override
        public ImageDescriptor getImageDescriptor()
        {
            return ImageDescriptor.createFromFile( RepositoryViewPart.class, "/icons/bundle-refresh.gif" );
        }

    }

    private TreeViewer treeViewer;


    @Override
    public void createPartControl( Composite parent )
    {
        createBody( parent );
        createMenu();
        SigilCore.getGlobalRepositoryManager().addRepositoryChangeListener( this );
    }


    @Override
    public void dispose()
    {
        SigilCore.getGlobalRepositoryManager().removeRepositoryChangeListener( this );
        super.dispose();
    }


    private void createMenu()
    {
        createTopMenu();
        createLocalMenu();
    }


    private void createLocalMenu()
    {
        /*MenuManager menuMgr = new MenuManager();
        menuMgr.add( new FindUsersAction() );
        Menu menu = menuMgr.createContextMenu(treeViewer.getControl());

        treeViewer.getControl().setMenu(menu);
        getViewSite().registerContextMenu(menuMgr, treeViewer); */
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager toolBar = bars.getToolBarManager();
        toolBar.add( new RefreshAction() );
    }


    private void createTopMenu()
    {
        IActionBars bars = getViewSite().getActionBars();
        IMenuManager menu = bars.getMenuManager();
        menu.removeAll();
        for ( final IBundleRepository rep : SigilCore.getGlobalRepositoryManager().getRepositories() )
        {
            if ( treeViewer.getInput() == null )
            {
                treeViewer.setInput( rep );
            }

            RepositoryAction action = new RepositoryAction( rep );
            menu.add( action );
        }
    }


    private void createBody( Composite parent )
    {
        // components
        Composite control = new Composite( parent, SWT.NONE );
        Tree tree = new Tree( control, SWT.NONE );

        // layout
        control.setLayout( new GridLayout( 1, false ) );
        tree.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 1, 1 ) );

        // viewer
        treeViewer = new TreeViewer( tree );
        treeViewer.setContentProvider( new DefaultTreeContentProvider()
        {
            public Object[] getChildren( Object parentElement )
            {
                if ( parentElement instanceof ICompoundModelElement )
                {
                    ICompoundModelElement model = ( ICompoundModelElement ) parentElement;
                    return model.children();
                }

                return null;
            }


            public Object getParent( Object element )
            {
                if ( element instanceof IModelElement )
                {
                    IModelElement model = ( IModelElement ) element;
                    return model.getParent();
                }

                return null;
            }


            public boolean hasChildren( Object element )
            {
                if ( element instanceof ICompoundModelElement )
                {
                    ICompoundModelElement model = ( ICompoundModelElement ) element;
                    return model.children().length > 0;
                }
                return false;
            }


            public Object[] getElements( Object inputElement )
            {
                IBundleRepository rep = ( IBundleRepository ) inputElement;
                return getBundles( rep );
            }
        } );

        treeViewer.setComparator( new ModelElementComparator() );

        treeViewer.setLabelProvider( new ModelLabelProvider() );

        treeViewer.addDragSupport( DND.DROP_LINK, new Transfer[]
            { LocalSelectionTransfer.getTransfer() }, new DragSourceAdapter()
        {
            @Override
            public void dragFinished( DragSourceEvent event )
            {
                // TODO Auto-generated method stub
                super.dragFinished( event );
            }


            @Override
            public void dragSetData( DragSourceEvent event )
            {
                // TODO Auto-generated method stub
                super.dragSetData( event );
            }


            @SuppressWarnings("unchecked")
            @Override
            public void dragStart( DragSourceEvent event )
            {
                if ( treeViewer.getSelection().isEmpty() )
                {
                    IStructuredSelection sel = ( IStructuredSelection ) treeViewer.getSelection();
                    for ( Iterator<IModelElement> i = sel.iterator(); i.hasNext(); )
                    {
                        IModelElement e = i.next();
                        if ( e instanceof ISigilBundle )
                        {
                            event.data = e;
                        }
                        else
                        {
                            event.doit = false;
                        }
                    }
                }
                else
                {
                    event.doit = false;
                }
            }
        } );
    }


    @Override
    public void setFocus()
    {
    }


    public void repositoryChanged( RepositoryChangeEvent event )
    {
        switch ( event.getType() )
        {
            case ADDED:
                createTopMenu();
                break;
            case CHANGED:
                if ( event.getRepository() == treeViewer.getInput() )
                {
                    SigilUI.runInUI( new Runnable()
                    {
                        public void run()
                        {
                            treeViewer.refresh();
                        }
                    } );
                }
                break;
            case REMOVED:
                if ( event.getRepository() == treeViewer.getInput() )
                {
                    treeViewer.setInput( null );
                }
                createTopMenu();
        }
    }


    private Object[] getBundles( IBundleRepository repository )
    {
        final LinkedList<ISigilBundle> bundles = new LinkedList<ISigilBundle>();
        repository.accept( new IRepositoryVisitor()
        {
            public boolean visit( ISigilBundle bundle )
            {
                bundles.add( bundle );
                return true;
            }
        } );
        return bundles.toArray();
    }
}
