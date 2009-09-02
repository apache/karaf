package org.apache.maven.shared.dependency.tree;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.ResolutionListenerForDepMgmt;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.codehaus.plexus.logging.Logger;

/**
 * An artifact resolution listener that constructs a dependency tree.
 * 
 * @author Edwin Punzalan
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyTreeResolutionListener.java 661727 2008-05-30 14:21:49Z bentmann $
 */
public class DependencyTreeResolutionListener implements ResolutionListener, ResolutionListenerForDepMgmt
{
    // fields -----------------------------------------------------------------
    
    /**
     * The log to write debug messages to.
     */
    private final Logger logger;

    /**
     * The parent dependency nodes of the current dependency node.
     */
    private final Stack parentNodes;

    /**
     * A map of dependency nodes by their attached artifact.
     */
    private final Map nodesByArtifact;

    /**
     * The root dependency node of the computed dependency tree.
     */
    private DependencyNode rootNode;

    /**
     * The dependency node currently being processed by this listener.
     */
    private DependencyNode currentNode;
    
    /**
     * Map &lt; String replacementId, String premanaged version >
     */
    private Map managedVersions = new HashMap();

    /**
     * Map &lt; String replacementId, String premanaged scope >
     */
    private Map managedScopes = new HashMap();

    // constructors -----------------------------------------------------------

    /**
     * Creates a new dependency tree resolution listener that writes to the specified log.
     * 
     * @param logger
     *            the log to write debug messages to
     */
    public DependencyTreeResolutionListener( Logger logger )
    {
        this.logger = logger;
        
        parentNodes = new Stack();
        nodesByArtifact = new IdentityHashMap();
        rootNode = null;
        currentNode = null;
    }

    // ResolutionListener methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void testArtifact( Artifact artifact )
    {
        log( "testArtifact: artifact=" + artifact );
    }

    /**
     * {@inheritDoc}
     */
    public void startProcessChildren( Artifact artifact )
    {
        log( "startProcessChildren: artifact=" + artifact );
        
        if ( !currentNode.getArtifact().equals( artifact ) )
        {
            throw new IllegalStateException( "Artifact was expected to be " + currentNode.getArtifact() + " but was "
                            + artifact );
        }

        parentNodes.push( currentNode );
    }

    /**
     * {@inheritDoc}
     */
    public void endProcessChildren( Artifact artifact )
    {
        DependencyNode node = (DependencyNode) parentNodes.pop();

        log( "endProcessChildren: artifact=" + artifact );
        
        if ( node == null )
        {
            throw new IllegalStateException( "Parent dependency node was null" );
        }

        if ( !node.getArtifact().equals( artifact ) )
        {
            throw new IllegalStateException( "Parent dependency node artifact was expected to be " + node.getArtifact()
                            + " but was " + artifact );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void includeArtifact( Artifact artifact )
    {
        log( "includeArtifact: artifact=" + artifact );
        
        DependencyNode existingNode = getNode( artifact );

        /*
         * Ignore duplicate includeArtifact calls since omitForNearer can be called prior to includeArtifact on the same
         * artifact, and we don't wish to include it twice.
         */
        if ( existingNode == null && isCurrentNodeIncluded() )
        {
            DependencyNode node = addNode( artifact );

            /*
             * Add the dependency management information cached in any prior manageArtifact calls, since includeArtifact
             * is always called after manageArtifact.
             */
            flushDependencyManagement( node );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void omitForNearer( Artifact omitted, Artifact kept )
    {
        log( "omitForNearer: omitted=" + omitted + " kept=" + kept );
        
        if ( !omitted.getDependencyConflictId().equals( kept.getDependencyConflictId() ) )
        {
            throw new IllegalArgumentException( "Omitted artifact dependency conflict id "
                            + omitted.getDependencyConflictId() + " differs from kept artifact dependency conflict id "
                            + kept.getDependencyConflictId() );
        }

        if ( isCurrentNodeIncluded() )
        {
            DependencyNode omittedNode = getNode( omitted );

            if ( omittedNode != null )
            {
                removeNode( omitted );
            }
            else
            {
                omittedNode = createNode( omitted );

                currentNode = omittedNode;
            }

            omittedNode.omitForConflict( kept );
            
            /*
             * Add the dependency management information cached in any prior manageArtifact calls, since omitForNearer
             * is always called after manageArtifact.
             */
            flushDependencyManagement( omittedNode );
            
            DependencyNode keptNode = getNode( kept );
            
            if ( keptNode == null )
            {
                addNode( kept );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateScope( Artifact artifact, String scope )
    {
        log( "updateScope: artifact=" + artifact + ", scope=" + scope );
        
        DependencyNode node = getNode( artifact );

        if ( node == null )
        {
            // updateScope events can be received prior to includeArtifact events
            node = addNode( artifact );
        }

        node.setOriginalScope( artifact.getScope() );
    }

    /**
     * {@inheritDoc}
     */
    public void manageArtifact( Artifact artifact, Artifact replacement )
    {
        // TODO: remove when ResolutionListenerForDepMgmt merged into ResolutionListener
        
        log( "manageArtifact: artifact=" + artifact + ", replacement=" + replacement );
        
        if ( replacement.getVersion() != null )
        {
            manageArtifactVersion( artifact, replacement );
        }
        
        if ( replacement.getScope() != null )
        {
            manageArtifactScope( artifact, replacement );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void omitForCycle( Artifact artifact )
    {
        log( "omitForCycle: artifact=" + artifact );
        
        if ( isCurrentNodeIncluded() )
        {
            DependencyNode node = createNode( artifact );

            node.omitForCycle();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateScopeCurrentPom( Artifact artifact, String scopeIgnored )
    {
        log( "updateScopeCurrentPom: artifact=" + artifact + ", scopeIgnored=" + scopeIgnored );
        
        DependencyNode node = getNode( artifact );

        if ( node == null )
        {
            // updateScopeCurrentPom events can be received prior to includeArtifact events
            node = addNode( artifact );
            // TODO remove the node that tried to impose its scope and add some info
        }

        node.setFailedUpdateScope( scopeIgnored );
    }

    /**
     * {@inheritDoc}
     */
    public void selectVersionFromRange( Artifact artifact )
    {
        log( "selectVersionFromRange: artifact=" + artifact );

        DependencyNode node = getNode( artifact );

        /*
         * selectVersionFromRange is called before includeArtifact
         */
        if ( node == null && isCurrentNodeIncluded() )
        {
            node = addNode( artifact );
        }

        node.setVersionSelectedFromRange( artifact.getVersionRange() );
        node.setAvailableVersions( artifact.getAvailableVersions() );
    }

    /**
     * {@inheritDoc}
     */
    public void restrictRange( Artifact artifact, Artifact replacement, VersionRange versionRange )
    {
        log( "restrictRange: artifact=" + artifact + ", replacement=" + replacement + ", versionRange=" + versionRange );
        
        // TODO: track range restriction in node (MNG-3093)
    }
    
    // ResolutionListenerForDepMgmt methods -----------------------------------
    
    /**
     * {@inheritDoc}
     */
    public void manageArtifactVersion( Artifact artifact, Artifact replacement )
    {
        log( "manageArtifactVersion: artifact=" + artifact + ", replacement=" + replacement );
        
        /*
         * DefaultArtifactCollector calls manageArtifact twice: first with the change; then subsequently with no change.
         * We ignore the second call when the versions are equal.
         */
        if ( isCurrentNodeIncluded() && !replacement.getVersion().equals( artifact.getVersion() ) )
        {
            /*
             * Cache management information and apply in includeArtifact, since DefaultArtifactCollector mutates the
             * artifact and then calls includeArtifact after manageArtifact.
             */
            managedVersions.put( getRangeId( replacement ), artifact.getVersion() );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void manageArtifactScope( Artifact artifact, Artifact replacement )
    {
        log( "manageArtifactScope: artifact=" + artifact + ", replacement=" + replacement );
        
        /*
         * DefaultArtifactCollector calls manageArtifact twice: first with the change; then subsequently with no change.
         * We ignore the second call when the scopes are equal.
         */
        if ( isCurrentNodeIncluded() && !replacement.getScope().equals( artifact.getScope() ) )
        {
            /*
             * Cache management information and apply in includeArtifact, since DefaultArtifactCollector mutates the
             * artifact and then calls includeArtifact after manageArtifact.
             */
            managedScopes.put( getRangeId( replacement ), artifact.getScope() );
        }
    }
    
    // public methods ---------------------------------------------------------

    /**
     * Gets a list of all dependency nodes in the computed dependency tree.
     * 
     * @return a list of dependency nodes
     * @deprecated As of 1.1, use a {@link CollectingDependencyNodeVisitor} on the root dependency node
     */
    public Collection getNodes()
    {
        return Collections.unmodifiableCollection( nodesByArtifact.values() );
    }

    /**
     * Gets the root dependency node of the computed dependency tree.
     * 
     * @return the root node
     */
    public DependencyNode getRootNode()
    {
        return rootNode;
    }

    // private methods --------------------------------------------------------
    
    /**
     * Writes the specified message to the log at debug level with indentation for the current node's depth.
     * 
     * @param message
     *            the message to write to the log
     */
    private void log( String message )
    {
        int depth = parentNodes.size();

        StringBuffer buffer = new StringBuffer();

        for ( int i = 0; i < depth; i++ )
        {
            buffer.append( "  " );
        }

        buffer.append( message );

        logger.debug( buffer.toString() );
    }

    /**
     * Creates a new dependency node for the specified artifact and appends it to the current parent dependency node.
     * 
     * @param artifact
     *            the attached artifact for the new dependency node
     * @return the new dependency node
     */
    private DependencyNode createNode( Artifact artifact )
    {
        DependencyNode node = new DependencyNode( artifact );

        if ( !parentNodes.isEmpty() )
        {
            DependencyNode parent = (DependencyNode) parentNodes.peek();

            parent.addChild( node );
        }

        return node;
    }
    
    /**
     * Creates a new dependency node for the specified artifact, appends it to the current parent dependency node and
     * puts it into the dependency node cache.
     * 
     * @param artifact
     *            the attached artifact for the new dependency node
     * @return the new dependency node
     */
    // package protected for unit test
    DependencyNode addNode( Artifact artifact )
    {
        DependencyNode node = createNode( artifact );

        DependencyNode previousNode = (DependencyNode) nodesByArtifact.put( node.getArtifact(), node );
        
        if ( previousNode != null )
        {
            throw new IllegalStateException( "Duplicate node registered for artifact: " + node.getArtifact() );
        }
        
        if ( rootNode == null )
        {
            rootNode = node;
        }

        currentNode = node;
        
        return node;
    }

    /**
     * Gets the dependency node for the specified artifact from the dependency node cache.
     * 
     * @param artifact
     *            the artifact to find the dependency node for
     * @return the dependency node, or <code>null</code> if the specified artifact has no corresponding dependency
     *         node
     */
    private DependencyNode getNode( Artifact artifact )
    {
        return (DependencyNode) nodesByArtifact.get( artifact );
    }

    /**
     * Removes the dependency node for the specified artifact from the dependency node cache.
     * 
     * @param artifact
     *            the artifact to remove the dependency node for
     */
    private void removeNode( Artifact artifact )
    {
        DependencyNode node = (DependencyNode) nodesByArtifact.remove( artifact );

        if ( !artifact.equals( node.getArtifact() ) )
        {
            throw new IllegalStateException( "Removed dependency node artifact was expected to be " + artifact
                            + " but was " + node.getArtifact() );
        }
    }

    /**
     * Gets whether the all the ancestors of the dependency node currently being processed by this listener have an
     * included state.
     * 
     * @return <code>true</code> if all the ancestors of the current dependency node have a state of
     *         <code>INCLUDED</code>
     */
    private boolean isCurrentNodeIncluded()
    {
        boolean included = true;

        for ( Iterator iterator = parentNodes.iterator(); included && iterator.hasNext(); )
        {
            DependencyNode node = (DependencyNode) iterator.next();

            if ( node.getState() != DependencyNode.INCLUDED )
            {
                included = false;
            }
        }

        return included;
    }

    /**
     * Updates the specified node with any dependency management information cached in prior <code>manageArtifact</code>
     * calls.
     * 
     * @param node
     *            the node to update
     */
    private void flushDependencyManagement( DependencyNode node )
    {
        Artifact artifact = node.getArtifact();
        String premanagedVersion = (String) managedVersions.get( getRangeId( artifact ) );
        String premanagedScope = (String) managedScopes.get( getRangeId( artifact ) );
        
        if ( premanagedVersion != null || premanagedScope != null )
        {
            if ( premanagedVersion != null )
            {
                node.setPremanagedVersion( premanagedVersion );
            }
            
            if ( premanagedScope != null )
            {
                node.setPremanagedScope( premanagedScope );
            }
            
            premanagedVersion = null;
            premanagedScope = null;
        }
    }

    private static String getRangeId( Artifact artifact )
    {
        return artifact.getDependencyConflictId() + ":" + artifact.getVersionRange();
    }
}
