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

package org.apache.felix.sigil.ui.eclipse.ui.views.resolution;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IRepositoryManager;
import org.apache.felix.sigil.repository.IResolutionMonitor;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.apache.felix.sigil.repository.ResolutionException;
import org.apache.felix.sigil.repository.ResolutionMonitorAdapter;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.RadialLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;


public class BundleResolverView extends ViewPart
{

    private static final String SHOW_LINK_LABELS = "Show link labels";
    private static final String HIDE_LINK_LABELS = "Hide link labels";
    private static final String SHOW_LOCAL_LINKS = "Show local links";
    private static final String HIDE_LOCAL_LINKS = "Hide local links";
    private static final String SHOW_DEPENDENTS = "Show dependents";
    private static final String HIDE_DEPENDENTS = "Hide dependents";
    private static final String SHOW_SATISFIED = "Show satisfied bundles";
    private static final String HIDE_SATISFIED = "Hide satisfied bundles";
    private static final String SHOW_UNSATISFIED = "Show unsatisfied bundles";
    private static final String HIDE_UNSATISFIED = "Hide unsatisfied bundles";
    private static final String SHOW_OPTIONAL = "Show optional dependencies";
    private static final String HIDE_OPTIONAL = "Hide optional dependencies";

    public static final String LINK_LABELS = "link.labels";
    public static final String LOCAL_LINKS = "local.links";
    public static final String DEPENDENTS = "dependents";
    public static final String SATISFIED = "satisified";
    public static final String UNSATISFIED = "unsatisfied";
    public static final String OPTIONAL = "optional";

    private GraphViewer viewer;
    private IModelElement current;
    private Job job;
    private int lastX;
    private int lastY;

    private Map<String, Boolean> displayed = new HashMap<String, Boolean>();

    private class ToggleDisplayAction extends Action
    {
        private String key;
        private String showMsg;
        private String hideMsg;


        ToggleDisplayAction( String key, String showMsg, String hideMsg )
        {
            this.key = key;
            this.showMsg = showMsg;
            this.hideMsg = hideMsg;
            setText( BundleResolverView.this.isDisplayed( key ) ? hideMsg : showMsg );
        }


        @Override
        public void run()
        {
            BundleResolverView.this.setDisplayed( key, !BundleResolverView.this.isDisplayed( key ) );
            setText( BundleResolverView.this.isDisplayed( key ) ? hideMsg : showMsg );
        }
    }


    public void setInput( final IModelElement element )
    {
        if ( current == null || !current.equals( element ) )
        {
            SigilCore.log( "Set input " + element );
            current = element;
            redraw();
        }
    }


    public void setDisplayed( String key, boolean b )
    {
        displayed.put( key, b );

        if ( key == DEPENDENTS )
        {
            int style = LayoutStyles.NO_LAYOUT_NODE_RESIZING;
            viewer.setLayoutAlgorithm( b ? new TreeLayoutAlgorithm( style ) : new RadialLayoutAlgorithm( style ) );
            redraw();
        }
        else if ( key == OPTIONAL )
        {
            redraw();
        }
        else if ( key == SATISFIED || key == UNSATISFIED )
        {
            viewer.refresh();
        }
    }


    public boolean isDisplayed( String key )
    {
        return displayed.get( key );
    }


    @Override
    public void setFocus()
    {
    }


    @Override
    public void createPartControl( Composite parent )
    {
        init();
        createViewer( parent );
        createListeners();
        createMenu();
    }


    private void init()
    {
        displayed.put( LINK_LABELS, false );
        displayed.put( LOCAL_LINKS, false );
        displayed.put( DEPENDENTS, false );
        displayed.put( OPTIONAL, false );
        displayed.put( SATISFIED, true );
        displayed.put( UNSATISFIED, true );
    }


    public BundleGraph getBundlegraph()
    {
        return ( BundleGraph ) viewer.getInput();
    }


    GraphViewer getGraphViewer()
    {
        return viewer;
    }


    String getLinkText( Link link )
    {
        StringBuffer buf = new StringBuffer();

        for ( IModelElement e : link.getRequirements() )
        {
            if ( buf.length() > 0 )
            {
                buf.append( "\n" );
            }
            if ( e instanceof IPackageImport )
            {
                IPackageImport pi = ( IPackageImport ) e;
                buf.append( "import " + pi.getPackageName() + " : " + pi.getVersions() + ": "
                    + ( pi.isOptional() ? "optional" : "mandatory" ) );
            }
            else if ( e instanceof IRequiredBundle )
            {
                IRequiredBundle rb = ( IRequiredBundle ) e;
                buf.append( "required bundle " + rb.getSymbolicName() + " : " + rb.getVersions() + ": "
                    + ( rb.isOptional() ? "optional" : "mandatory" ) );
            }
        }

        return buf.toString();
    }


    private synchronized void redraw()
    {
        final IModelElement element = current;
        if ( job != null )
        {
            job.cancel();
        }

        job = new Job( "Resolving " + current )
        {
            @Override
            protected IStatus run( IProgressMonitor progress )
            {
                try
                {
                    resolve( element, progress );
                    return Status.OK_STATUS;
                }
                catch ( CoreException e )
                {
                    return e.getStatus();
                }
            }
        };
        job.schedule();
    }


    private void resolve( IModelElement element, IProgressMonitor progress ) throws CoreException
    {
        final BundleGraph graph = new BundleGraph();

        IResolutionMonitor monitor = new ResolutionMonitorAdapter( progress )
        {
            @Override
            public void startResolution( IModelElement requirement )
            {
                graph.startResolution( requirement );
            }


            @Override
            public void endResolution( IModelElement requirement, ISigilBundle provider )
            {
                graph.endResolution( requirement, provider );
            }
        };

        ISigilProjectModel project = findProject( element );
        IRepositoryManager repository = SigilCore.getRepositoryManager( project );

        int options = ResolutionConfig.IGNORE_ERRORS;

        if ( isDisplayed( DEPENDENTS ) )
        {
            options |= ResolutionConfig.INCLUDE_DEPENDENTS;
        }
        if ( isDisplayed( OPTIONAL ) )
        {
            options |= ResolutionConfig.INCLUDE_OPTIONAL;
        }

        ResolutionConfig config = new ResolutionConfig( options );

        try
        {
            repository.getBundleResolver().resolve( element, config, monitor );
        }
        catch ( ResolutionException e )
        {
            throw SigilCore.newCoreException( "Failed to resolve " + element, e );
        }

        SigilUI.runInUI( new Runnable()
        {
            public void run()
            {
                viewer.setInput( graph );
                addCustomUIElements();
            }
        } );
    }


    private static ISigilProjectModel findProject( IModelElement element )
    {
        if ( element == null )
        {
            return null;
        }
        if ( element instanceof ISigilProjectModel )
        {
            return ( ISigilProjectModel ) element;
        }

        return element.getAncestor( ISigilProjectModel.class );
    }


    @SuppressWarnings("unchecked")
    private void addCustomUIElements()
    {
        if ( !isDisplayed( LINK_LABELS ) )
        {
            for ( GraphConnection c : ( List<GraphConnection> ) viewer.getGraphControl().getConnections() )
            {
                if ( c.getData() instanceof Link )
                {
                    c.setTooltip( buildToolTip( ( Link ) c.getData() ) );
                }
            }
        }
    }


    private IFigure buildToolTip( Link link )
    {
        Label l = new Label();
        l.setText( getLinkText( link ) );
        return l;
    }


    private void createViewer( Composite parent )
    {
        parent.setLayout( new FillLayout() );
        viewer = new GraphViewer( parent, SWT.H_SCROLL | SWT.V_SCROLL );
        IContentProvider contentProvider = new BundleGraphContentProvider();
        viewer.setContentProvider( contentProvider );
        viewer.setLabelProvider( new BundleGraphLabelProvider( this ) );
        viewer.addFilter( new BundleGraphViewFilter( this ) );

        int style = LayoutStyles.NO_LAYOUT_NODE_RESIZING;
        viewer.setLayoutAlgorithm( isDisplayed( DEPENDENTS ) ? new TreeLayoutAlgorithm( style )
            : new RadialLayoutAlgorithm( style ) );
        viewer.addSelectionChangedListener( new BundleConnectionHighlighter( this ) );
        viewer.setInput( new BundleGraph() );
    }


    private void createMenu()
    {
        IActionBars action = getViewSite().getActionBars();
        action.getMenuManager().add( new ToggleDisplayAction( LINK_LABELS, SHOW_LINK_LABELS, HIDE_LINK_LABELS ) );
        action.getMenuManager().add( new ToggleDisplayAction( LOCAL_LINKS, SHOW_LOCAL_LINKS, HIDE_LOCAL_LINKS ) );
        action.getMenuManager().add( new ToggleDisplayAction( DEPENDENTS, SHOW_DEPENDENTS, HIDE_DEPENDENTS ) );
        action.getMenuManager().add( new ToggleDisplayAction( OPTIONAL, SHOW_OPTIONAL, HIDE_OPTIONAL ) );
        action.getMenuManager().add( new ToggleDisplayAction( SATISFIED, SHOW_SATISFIED, HIDE_SATISFIED ) );
        action.getMenuManager().add( new ToggleDisplayAction( UNSATISFIED, SHOW_UNSATISFIED, HIDE_UNSATISFIED ) );
        action.updateActionBars();
    }


    private void createListeners()
    {
        IPartService ps = ( IPartService ) getViewSite().getService( IPartService.class );
        ps.addPartListener( new EditorViewPartListener( this ) );
        viewer.getGraphControl().addControlListener( new ControlAdapter()
        {
            @Override
            public void controlResized( ControlEvent e )
            {
                Graph g = ( Graph ) e.getSource();
                int x = g.getSize().x;
                int y = g.getSize().y;
                if ( lastX != x || lastY != y )
                {
                    lastX = x;
                    lastY = y;
                    redraw();
                }
            }
        } );
    }
}
