/*******************************************************************************
 * Copyright (c) 2012, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.apache.karaf.tooling.semantic.eclipse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.ConfigUtils;
import org.sonatype.aether.util.graph.DefaultDependencyNode;
import org.sonatype.aether.util.graph.transformer.TransformationContextKeys;


/**
 * A dependency graph transformer that resolves version and scope conflicts among dependencies. For a given set of
 * conflicting nodes, one node will be chosen as the winner and the other nodes are removed from the dependency graph.
 * The exact rules by which a winning node and its effective scope are determined are controlled by user-supplied
 * implementations of {@link VersionSelector}, {@link ScopeSelector}, {@link OptionalitySelector} and
 * {@link ScopeDeriver}.
 * <p>
 * By default, this graph transformer will turn the dependency graph into a tree without duplicate artifacts. Using the
 * configuration property {@link #CONFIG_PROP_VERBOSE}, a verbose mode can be enabled where the graph is still turned
 * into a tree but all nodes participating in a conflict are retained. The nodes that were rejected during conflict
 * resolution have no children and link back to the winner node via the {@link #NODE_DATA_WINNER} key in their custom
 * data. Additionally, the keys {@link #NODE_DATA_ORIGINAL_SCOPE} and {@link #NODE_DATA_ORIGINAL_OPTIONALITY} are used
 * to store the original scope and optionality of each node. Obviously, the resulting dependency tree is not suitable
 * for artifact resolution unless a filter is employed to exclude the duplicate dependencies.
 * <p>
 * This transformer will query the keys {@link TransformationContextKeys#CONFLICT_IDS},
 * {@link TransformationContextKeys#SORTED_CONFLICT_IDS}, {@link TransformationContextKeys#CYCLIC_CONFLICT_IDS} for
 * existing information about conflict ids. In absence of this information, it will automatically invoke the
 * {@link ConflictIdSorter} to calculate it.
 */
public final class ConflictResolver
    implements DependencyGraphTransformer
{

    /**
     * The key in the repository session's {@link RepositorySystemSession#getConfigProperties() configuration
     * properties} used to store a {@link Boolean} flag controlling the transformer's verbose mode.
     */
    public static final String CONFIG_PROP_VERBOSE = "aether.conflictResolver.verbose";

    /**
     * The key in the dependency node's {@link DependencyNode#getData() custom data} under which a reference to the
     * {@link DependencyNode} which has won the conflict is stored.
     */
    public static final String NODE_DATA_WINNER = "conflict.winner";

    /**
     * The key in the dependency node's {@link DependencyNode#getData() custom data} under which the scope of the
     * dependency before scope derivation and conflict resolution is stored.
     */
    public static final String NODE_DATA_ORIGINAL_SCOPE = "conflict.originalScope";

    /**
     * The key in the dependency node's {@link DependencyNode#getData() custom data} under which the optional flag of
     * the dependency before derivation and conflict resolution is stored.
     */
    public static final String NODE_DATA_ORIGINAL_OPTIONALITY = "conflict.originalOptionality";

    private final VersionSelector versionSelector;

    private final ScopeSelector scopeSelector;

    private final ScopeDeriver scopeDeriver;

    private final OptionalitySelector optionalitySelector;

    /**
     * Creates a new conflict resolver instance with the specified hooks.
     * 
     * @param versionSelector The version selector to use, must not be {@code null}.
     * @param scopeSelector The scope selector to use, must not be {@code null}.
     * @param optionalitySelector The optionality selector ot use, must not be {@code null}.
     * @param scopeDeriver The scope deriver to use, must not be {@code null}.
     */
    public ConflictResolver( VersionSelector versionSelector, ScopeSelector scopeSelector,
                             OptionalitySelector optionalitySelector, ScopeDeriver scopeDeriver )
    {
        if ( versionSelector == null )
        {
            throw new IllegalArgumentException( "version selector not specified" );
        }
        this.versionSelector = versionSelector;
        if ( scopeSelector == null )
        {
            throw new IllegalArgumentException( "scope selector not specified" );
        }
        this.scopeSelector = scopeSelector;
        if ( scopeDeriver == null )
        {
            throw new IllegalArgumentException( "scope deriver not specified" );
        }
        this.scopeDeriver = scopeDeriver;
        if ( optionalitySelector == null )
        {
            throw new IllegalArgumentException( "optionality selector not specified" );
        }
        this.optionalitySelector = optionalitySelector;
    }

    public DependencyNode transformGraph( DependencyNode node, DependencyGraphTransformationContext context )
        throws RepositoryException
    {
        List<?> sortedConflictIds = (List<?>) context.get( TransformationContextKeys.SORTED_CONFLICT_IDS );
        if ( sortedConflictIds == null )
        {
            ConflictIdSorter sorter = new ConflictIdSorter();
            sorter.transformGraph( node, context );

            sortedConflictIds = (List<?>) context.get( TransformationContextKeys.SORTED_CONFLICT_IDS );
        }

        @SuppressWarnings( "unchecked" )
        Map<String, Object> stats = (Map<String, Object>) context.get( ContextKeys.STATS ); // XXX
        long time1 = System.currentTimeMillis();

        @SuppressWarnings( "unchecked" )
        Collection<Collection<?>> conflictIdCycles =
            (Collection<Collection<?>>) context.get( TransformationContextKeys.CYCLIC_CONFLICT_IDS );
        if ( conflictIdCycles == null )
        {
            throw new RepositoryException( "conflict id cycles have not been identified" );
        }

        Map<?, ?> conflictIds = (Map<?, ?>) context.get( TransformationContextKeys.CONFLICT_IDS );
        if ( conflictIds == null )
        {
            throw new RepositoryException( "conflict groups have not been identified" );
        }

        Map<Object, Collection<Object>> cyclicPredecessors = new HashMap<Object, Collection<Object>>();
        for ( Collection<?> cycle : conflictIdCycles )
        {
            for ( Object conflictId : cycle )
            {
                Collection<Object> predecessors = cyclicPredecessors.get( conflictId );
                if ( predecessors == null )
                {
                    predecessors = new HashSet<Object>();
                    cyclicPredecessors.put( conflictId, predecessors );
                }
                predecessors.addAll( cycle );
            }
        }

        State state = new State( node, conflictIds, sortedConflictIds.size(), context );
        for ( Iterator<?> it = sortedConflictIds.iterator(); it.hasNext(); )
        {
            Object conflictId = it.next();

            // reset data structures for next graph walk
            state.prepare( conflictId, cyclicPredecessors.get( conflictId ) );

            // find nodes with the current conflict id and while walking the graph (more deeply), nuke leftover losers
            gatherConflictItems( node, state );

            // now that we know the min depth of the parents, update depth of conflict items
            state.finish();

            // earlier runs might have nuked all parents of the current conflict id, so it might not exist anymore
            if ( !state.items.isEmpty() )
            {
                ConflictContext ctx = state.conflictCtx;
                state.versionSelector.selectVersion( ctx );
                if ( ctx.winner == null )
                {
                    throw new RepositoryException( "conflict resolver did not select winner among " + state.items );
                }
                DependencyNode winner = ctx.winner.node;

                state.scopeSelector.selectScope( ctx );
                if ( state.verbose )
                {
                    winner.setData( NODE_DATA_ORIGINAL_SCOPE, winner.getDependency().getScope() );
                }
                winner.setScope( ctx.scope );

                state.optionalitySelector.selectOptionality( ctx );
                if ( state.verbose )
                {
                    winner.setData( NODE_DATA_ORIGINAL_OPTIONALITY, winner.getDependency().isOptional() );
                }
                // winner.setOptional( ctx.optional ); // XXX

                removeLosers( state );
            }

            // record the winner so we can detect leftover losers during future graph walks
            state.winner();

            // in case of cycles, trigger final graph walk to ensure all leftover losers are gone
            if ( !it.hasNext() && !conflictIdCycles.isEmpty() && state.conflictCtx.winner != null )
            {
                DependencyNode winner = state.conflictCtx.winner.node;
                state.prepare( state, null );
                gatherConflictItems( winner, state );
            }
        }

        if ( stats != null )
        {
            long time2 = System.currentTimeMillis();
            stats.put( "ConflictResolver.totalTime", time2 - time1 );
            stats.put( "ConflictResolver.conflictItemCount", state.totalConflictItems );
        }

        return node;
    }

    private boolean gatherConflictItems( DependencyNode node, State state )
        throws RepositoryException
    {
        Object conflictId = state.conflictIds.get( node );
        if ( state.currentId.equals( conflictId ) )
        {
            // found it, add conflict item (if not already done earlier by another path)
            state.add( node );
            // we don't recurse here so we might miss losers beneath us, those will be nuked during future walks below
        }
        else if ( state.loser( node, conflictId ) )
        {
            // found a leftover loser (likely in a cycle) of an already processed conflict id, tell caller to nuke it
            return false;
        }
        else if ( state.push( node, conflictId ) )
        {
            // found potential parent, no cycle and not visisted before with the same derived scope, so recurse
            for ( Iterator<DependencyNode> it = node.getChildren().iterator(); it.hasNext(); )
            {
                DependencyNode child = it.next();
                if ( !gatherConflictItems( child, state ) )
                {
                    it.remove();
                }
            }
            state.pop();
        }
        return true;
    }

    private void removeLosers( State state )
    {
        ConflictItem winner = state.conflictCtx.winner;
        List<DependencyNode> previousParent = null;
        ListIterator<DependencyNode> childIt = null;
        boolean conflictVisualized = false;
        for ( ConflictItem item : state.items )
        {
            if ( item == winner )
            {
                continue;
            }
            if ( item.parent != previousParent )
            {
                childIt = item.parent.listIterator();
                previousParent = item.parent;
                conflictVisualized = false;
            }
            while ( childIt.hasNext() )
            {
                DependencyNode child = childIt.next();
                if ( child == item.node )
                {
                    if ( state.verbose && !conflictVisualized && item.parent != winner.parent )
                    {
                        conflictVisualized = true;
                        DependencyNode loser = new DefaultDependencyNode( child );
                        loser.setData( NODE_DATA_WINNER, winner.node );
                        loser.setData( NODE_DATA_ORIGINAL_SCOPE, loser.getDependency().getScope() );
                        loser.setData( NODE_DATA_ORIGINAL_OPTIONALITY, loser.getDependency().isOptional() );
                        loser.setScope( item.getScopes().iterator().next() );
                        // loser.setChildren( Collections.<DependencyNode> emptyList() ); // XXX
                        childIt.set( loser );
                    }
                    else
                    {
                        childIt.remove();
                    }
                    break;
                }
            }
        }
        // there might still be losers beneath the winner (e.g. in case of cycles)
        // those will be nuked during future graph walks when we include the winner in the recursion
    }

    final class NodeInfo
    {

        /**
         * The smallest depth at which the node was seen, used for "the" depth of its conflict items.
         */
        int minDepth;

        /**
         * The set of derived scopes the node was visited with, used to check whether an already seen node needs to be
         * revisited again in context of another scope. To conserve memory, we start with {@code String} and update to
         * {@code Set<String>} if needed.
         */
        Object derivedScopes;

        /**
         * The set of derived optionalities the node was visited with, used to check whether an already seen node needs
         * to be revisited again in context of another optionality. To conserve memory, encoded as bit field (bit 0 ->
         * optional=false, bit 1 -> optional=true).
         */
        int derivedOptionalities;

        /**
         * The conflict items which are immediate children of the node, used to easily update those conflict items after
         * a new parent scope/optionality was encountered.
         */
        List<ConflictItem> children;

        static final int CHANGE_SCOPE = 0x01;

        static final int CHANGE_OPTIONAL = 0x02;

        private static final int OPT_FALSE = 0x01;

        private static final int OPT_TRUE = 0x02;

        NodeInfo( int depth, String derivedScope, boolean optional )
        {
            minDepth = depth;
            derivedScopes = derivedScope;
            derivedOptionalities = optional ? OPT_TRUE : OPT_FALSE;
        }

        @SuppressWarnings( "unchecked" )
        int update( int depth, String derivedScope, boolean optional )
        {
            if ( depth < minDepth )
            {
                minDepth = depth;
            }
            int changes;
            if ( derivedScopes.equals( derivedScope ) )
            {
                changes = 0;
            }
            else if ( derivedScopes instanceof Collection )
            {
                changes = ( (Collection<String>) derivedScopes ).add( derivedScope ) ? CHANGE_SCOPE : 0;
            }
            else
            {
                Collection<String> scopes = new HashSet<String>();
                scopes.add( (String) derivedScopes );
                scopes.add( derivedScope );
                derivedScopes = scopes;
                changes = CHANGE_SCOPE;
            }
            int bit = optional ? OPT_TRUE : OPT_FALSE;
            if ( ( derivedOptionalities & bit ) == 0 )
            {
                derivedOptionalities |= bit;
                changes |= CHANGE_OPTIONAL;
            }
            return changes;
        }

        void add( ConflictItem item )
        {
            if ( children == null )
            {
                children = new ArrayList<ConflictItem>( 1 );
            }
            children.add( item );
        }

    }

    final class State
    {

        /**
         * The conflict id currently processed.
         */
        Object currentId;

        /**
         * Stats counter.
         */
        int totalConflictItems;

        /**
         * Flag whether we should keep losers in the graph to enable visualization/troubleshooting of conflicts.
         */
        final boolean verbose;

        /**
         * A mapping from conflict id to winner node, helps to recognize nodes that have their effective
         * scope&optionality set or are leftovers from previous removals.
         */
        final Map<Object, DependencyNode> resolvedIds;

        /**
         * The set of conflict ids which could apply to ancestors of nodes with the current conflict id, used to avoid
         * recursion early on. This is basically a superset of the key set of resolvedIds, the additional ids account
         * for cyclic dependencies.
         */
        final Collection<Object> potentialAncestorIds;

        /**
         * The output from the conflict marker
         */
        final Map<?, ?> conflictIds;

        /**
         * The conflict items we have gathered so far for the current conflict id.
         */
        final List<ConflictItem> items;

        /**
         * The (conceptual) mapping from nodes to extra infos, technically keyed by the node's child list which better
         * captures the identity of a node since we're basically concerned with effects towards children.
         */
        final Map<List<DependencyNode>, NodeInfo> infos;

        /**
         * The set of nodes on the DFS stack to detect cycles, technically keyed by the node's child list to match the
         * dirty graph structure produced by the dependency collector for cycles.
         */
        final Map<List<DependencyNode>, Object> stack;

        /**
         * The stack of parent nodes.
         */
        final List<DependencyNode> parentNodes;

        /**
         * The stack of derived scopes for parent nodes.
         */
        final List<String> parentScopes;

        /**
         * The stack of derived optional flags for parent nodes.
         */
        final List<Boolean> parentOptionals;

        /**
         * The stack of node infos for parent nodes, may contain {@code null} which is used to disable creating new
         * conflict items when visiting their parent again (conflict items are meant to be unique by parent-node combo).
         */
        final List<NodeInfo> parentInfos;

        /**
         * The conflict context passed to the version/scope/optionality selectors, updated as we move along rather than
         * recreated to avoid tmp objects.
         */
        final ConflictContext conflictCtx;

        /**
         * The scope context passed to the scope deriver, updated as we move along rather than recreated to avoid tmp
         * objects.
         */
        final ScopeContext scopeCtx;

        /**
         * The effective version selector, i.e. after initialization.
         */
        final VersionSelector versionSelector;

        /**
         * The effective scope selector, i.e. after initialization.
         */
        final ScopeSelector scopeSelector;

        /**
         * The effective scope deriver, i.e. after initialization.
         */
        final ScopeDeriver scopeDeriver;

        /**
         * The effective optionality selector, i.e. after initialization.
         */
        final OptionalitySelector optionalitySelector;

        State( DependencyNode root, Map<?, ?> conflictIds, int conflictIdCount,
               DependencyGraphTransformationContext context )
            throws RepositoryException
        {
            this.conflictIds = conflictIds;
            verbose = ConfigUtils.getBoolean( context.getSession(), false, CONFIG_PROP_VERBOSE );
            potentialAncestorIds = new HashSet<Object>( conflictIdCount * 2 );
            resolvedIds = new HashMap<Object, DependencyNode>( conflictIdCount * 2 );
            items = new ArrayList<ConflictItem>( 256 );
            infos = new IdentityHashMap<List<DependencyNode>, NodeInfo>( 64 );
            stack = new IdentityHashMap<List<DependencyNode>, Object>( 64 );
            parentNodes = new ArrayList<DependencyNode>( 64 );
            parentScopes = new ArrayList<String>( 64 );
            parentOptionals = new ArrayList<Boolean>( 64 );
            parentInfos = new ArrayList<NodeInfo>( 64 );
            conflictCtx = new ConflictContext( root, conflictIds, items );
            scopeCtx = new ScopeContext( null, null );
            versionSelector = ConflictResolver.this.versionSelector.getInstance( root, context );
            scopeSelector = ConflictResolver.this.scopeSelector.getInstance( root, context );
            scopeDeriver = ConflictResolver.this.scopeDeriver.getInstance( root, context );
            optionalitySelector = ConflictResolver.this.optionalitySelector.getInstance( root, context );
        }

        void prepare( Object conflictId, Collection<Object> cyclicPredecessors )
        {
            currentId = conflictCtx.conflictId = conflictId;
            conflictCtx.winner = null;
            conflictCtx.scope = null;
            conflictCtx.optional = null;
            items.clear();
            infos.clear();
            if ( cyclicPredecessors != null )
            {
                potentialAncestorIds.addAll( cyclicPredecessors );
            }
        }

        void finish()
        {
            List<DependencyNode> previousParent = null;
            int previousDepth = 0;
            totalConflictItems += items.size();
            for ( int i = items.size() - 1; i >= 0; i-- )
            {
                ConflictItem item = items.get( i );
                if ( item.parent == previousParent )
                {
                    item.depth = previousDepth;
                }
                else if ( item.parent != null )
                {
                    previousParent = item.parent;
                    NodeInfo info = infos.get( previousParent );
                    previousDepth = info.minDepth + 1;
                    item.depth = previousDepth;
                }
            }
            potentialAncestorIds.add( currentId );
        }

        void winner()
        {
            resolvedIds.put( currentId, ( conflictCtx.winner != null ) ? conflictCtx.winner.node : null );
        }

        boolean loser( DependencyNode node, Object conflictId )
        {
            DependencyNode winner = resolvedIds.get( conflictId );
            return winner != null && winner != node;
        }

        boolean push( DependencyNode node, Object conflictId )
            throws RepositoryException
        {
            if ( conflictId == null )
            {
                if ( node.getDependency() != null )
                {
                    if ( node.getData().get( NODE_DATA_WINNER ) != null )
                    {
                        return false;
                    }
                    throw new RepositoryException( "missing conflict id for node " + node );
                }
            }
            else if ( !potentialAncestorIds.contains( conflictId ) )
            {
                return false;
            }

            List<DependencyNode> graphNode = node.getChildren();
            if ( stack.put( graphNode, Boolean.TRUE ) != null )
            {
                return false;
            }

            int depth = depth();
            String scope = deriveScope( node, conflictId );
            boolean optional = deriveOptional( node, conflictId );
            NodeInfo info = infos.get( graphNode );
            if ( info == null )
            {
                info = new NodeInfo( depth, scope, optional );
                infos.put( graphNode, info );
                parentInfos.add( info );
                parentNodes.add( node );
                parentScopes.add( scope );
                parentOptionals.add( optional );
            }
            else
            {
                int changes = info.update( depth, scope, optional );
                if ( changes == 0 )
                {
                    stack.remove( graphNode );
                    return false;
                }
                parentInfos.add( null ); // disable creating new conflict items, we update the existing ones below
                parentNodes.add( node );
                parentScopes.add( scope );
                parentOptionals.add( optional );
                if ( info.children != null )
                {
                    if ( ( changes & NodeInfo.CHANGE_SCOPE ) != 0 )
                    {
                        for ( int i = info.children.size() - 1; i >= 0; i-- )
                        {
                            ConflictItem item = info.children.get( i );
                            String childScope = deriveScope( item.node, null );
                            item.addScope( childScope );
                        }
                    }
                    if ( ( changes & NodeInfo.CHANGE_OPTIONAL ) != 0 )
                    {
                        for ( int i = info.children.size() - 1; i >= 0; i-- )
                        {
                            ConflictItem item = info.children.get( i );
                            boolean childOptional = deriveOptional( item.node, null );
                            item.addOptional( childOptional );
                        }
                    }
                }
            }

            return true;
        }

        void pop()
        {
            int last = parentInfos.size() - 1;
            parentInfos.remove( last );
            parentScopes.remove( last );
            parentOptionals.remove( last );
            DependencyNode node = parentNodes.remove( last );
            stack.remove( node.getChildren() );
        }

        void add( DependencyNode node )
            throws RepositoryException
        {
            DependencyNode parent = parent();
            if ( parent == null )
            {
                ConflictItem item = newConflictItem( parent, node );
                items.add( item );
            }
            else
            {
                NodeInfo info = parentInfos.get( parentInfos.size() - 1 );
                if ( info != null )
                {
                    ConflictItem item = newConflictItem( parent, node );
                    info.add( item );
                    items.add( item );
                }
            }
        }

        private ConflictItem newConflictItem( DependencyNode parent, DependencyNode node )
            throws RepositoryException
        {
            return new ConflictItem( parent, node, deriveScope( node, null ), deriveOptional( node, null ) );
        }

        private int depth()
        {
            return parentNodes.size();
        }

        private DependencyNode parent()
        {
            int size = parentNodes.size();
            return ( size <= 0 ) ? null : parentNodes.get( size - 1 );
        }

        private String deriveScope( DependencyNode node, Object conflictId )
            throws RepositoryException
        {
        	// XXX
//            if ( ( node.getManagedBits() & DependencyNode.MANAGED_SCOPE ) != 0
//                || ( conflictId != null && resolvedIds.containsKey( conflictId ) ) )
//            {
//                return scope( node.getDependency() );
//            }

            int depth = parentNodes.size();
            scopes( depth, node.getDependency() );
            if ( depth > 0 )
            {
                scopeDeriver.deriveScope( scopeCtx );
            }
            return scopeCtx.derivedScope;
        }

        private void scopes( int parent, Dependency child )
        {
            scopeCtx.parentScope = ( parent > 0 ) ? parentScopes.get( parent - 1 ) : null;
            scopeCtx.derivedScope = scopeCtx.childScope = scope( child );
        }

        private String scope( Dependency dependency )
        {
            return ( dependency != null ) ? dependency.getScope() : null;
        }

        private boolean deriveOptional( DependencyNode node, Object conflictId )
        {
            Dependency dep = node.getDependency();
            boolean optional = ( dep != null ) ? dep.isOptional() : false;
            // XXX
//            if ( optional || ( node.getManagedBits() & DependencyNode.MANAGED_OPTIONAL ) != 0
//                || ( conflictId != null && resolvedIds.containsKey( conflictId ) ) )
//            {
//                return optional;
//            }
            int depth = parentNodes.size();
            return ( depth > 0 ) ? parentOptionals.get( depth - 1 ) : false;
        }

    }

    /**
     * A context used to hold information that is relevant for deriving the scope of a child dependency.
     * 
     * @see ScopeDeriver
     * @noinstantiate This class is not intended to be instantiated by clients in production code, the constructor may
     *                change without notice and only exists to enable unit testing.
     */
    public static final class ScopeContext
    {

        String parentScope;

        String childScope;

        String derivedScope;

        /**
         * Creates a new scope context with the specified properties.
         * 
         * @param parentScope The scope of the parent dependency, may be {@code null}.
         * @param childScope The scope of the child dependency, may be {@code null}.
         * @noreference This class is not intended to be instantiated by clients in production code, the constructor may
         *              change without notice and only exists to enable unit testing.
         */
        public ScopeContext( String parentScope, String childScope )
        {
            this.parentScope = ( parentScope != null ) ? parentScope : "";
            derivedScope = this.childScope = ( childScope != null ) ? childScope : "";
        }

        /**
         * Gets the scope of the parent dependency. This is usually the scope that was derived by earlier invocations of
         * the scope deriver.
         * 
         * @return The scope of the parent dependency, never {@code null}.
         */
        public String getParentScope()
        {
            return parentScope;
        }

        /**
         * Gets the original scope of the child dependency. This is the scope that was declared in the artifact
         * descriptor of the parent dependency.
         * 
         * @return The original scope of the child dependency, never {@code null}.
         */
        public String getChildScope()
        {
            return childScope;
        }

        /**
         * Gets the derived scope of the child dependency. This is initially equal to {@link #getChildScope()} until the
         * scope deriver makes changes.
         * 
         * @return The derived scope of the child dependency, never {@code null}.
         */
        public String getDerivedScope()
        {
            return derivedScope;
        }

        /**
         * Sets the derived scope of the child dependency.
         * 
         * @param derivedScope The derived scope of the dependency, may be {@code null}.
         */
        public void setDerivedScope( String derivedScope )
        {
            this.derivedScope = ( derivedScope != null ) ? derivedScope : "";
        }

    }

    /**
     * A conflicting dependency.
     * 
     * @noinstantiate This class is not intended to be instantiated by clients in production code, the constructor may
     *                change without notice and only exists to enable unit testing.
     */
    public static final class ConflictItem
    {

        // nodes can share child lists, we care about the unique owner of a child node which is the child list
        final List<DependencyNode> parent;

        // only for debugging/toString() to help identify the parent node(s)
        final Artifact artifact;

        final DependencyNode node;

        int depth;

        // we start with String and update to Set<String> if needed
        Object scopes;

        // bit field of OPTIONAL_FALSE and OPTIONAL_TRUE
        int optionalities;

        /**
         * Bit flag indicating whether one or more paths consider the dependency non-optional.
         */
        public static final int OPTIONAL_FALSE = 0x01;

        /**
         * Bit flag indicating whether one or more paths consider the dependency optional.
         */
        public static final int OPTIONAL_TRUE = 0x02;

        ConflictItem( DependencyNode parent, DependencyNode node, String scope, boolean optional )
        {
            if ( parent != null )
            {
                this.parent = parent.getChildren();
                this.artifact = parent.getDependency().getArtifact(); // XXX
            }
            else
            {
                this.parent = null;
                this.artifact = null;
            }
            this.node = node;
            this.scopes = scope;
            this.optionalities = optional ? OPTIONAL_TRUE : OPTIONAL_FALSE;
        }

        /**
         * Creates a new conflict item with the specified properties.
         * 
         * @param parent The parent node of the conflicting dependency, may be {@code null}.
         * @param node The conflicting dependency, must not be {@code null}.
         * @param depth The zero-based depth of the conflicting dependency.
         * @param optionalities The optionalities the dependency was encountered with, encoded as a bit field consisting
         *            of {@link ConflictResolver.ConflictItem#OPTIONAL_TRUE} and
         *            {@link ConflictResolver.ConflictItem#OPTIONAL_FALSE}.
         * @param scopes The derived scopes of the conflicting dependency, must not be {@code null}.
         * @noreference This class is not intended to be instantiated by clients in production code, the constructor may
         *              change without notice and only exists to enable unit testing.
         */
        public ConflictItem( DependencyNode parent, DependencyNode node, int depth, int optionalities, String... scopes )
        {
            this.parent = ( parent != null ) ? parent.getChildren() : null;
            this.artifact = ( parent != null ) ? parent.getDependency().getArtifact() : null; // XXX
            this.node = node;
            this.depth = depth;
            this.optionalities = optionalities;
            this.scopes = Arrays.asList( scopes );
        }

        /**
         * Determines whether the specified conflict item is a sibling of this item.
         * 
         * @param item The other conflict item, must not be {@code null}.
         * @return {@code true} if the given item has the same parent as this item, {@code false} otherwise.
         */
        public boolean isSibling( ConflictItem item )
        {
            return parent == item.parent;
        }

        /**
         * Gets the dependency node involved in the conflict.
         * 
         * @return The involved dependency node, never {@code null}.
         */
        public DependencyNode getNode()
        {
            return node;
        }

        /**
         * Gets the dependency involved in the conflict, short for {@code getNode.getDependency()}.
         * 
         * @return The involved dependency, never {@code null}.
         */
        public Dependency getDependency()
        {
            return node.getDependency();
        }

        /**
         * Gets the zero-based depth at which the conflicting node occurs in the graph. As such, the depth denotes the
         * number of parent nodes. If actually multiple paths lead to the node, the return value denotes the smallest
         * possible depth.
         * 
         * @return The zero-based depth of the node in the graph.
         */
        public int getDepth()
        {
            return depth;
        }

        /**
         * Gets the derived scopes of the dependency. In general, the same dependency node could be reached via
         * different paths and each path might result in a different derived scope.
         * 
         * @see ScopeDeriver
         * @return The (read-only) set of derived scopes of the dependency, never {@code null}.
         */
        @SuppressWarnings( "unchecked" )
        public Collection<String> getScopes()
        {
            if ( scopes instanceof String )
            {
                return Collections.singleton( (String) scopes );
            }
            return (Collection<String>) scopes;
        }

        @SuppressWarnings( "unchecked" )
        void addScope( String scope )
        {
            if ( scopes instanceof Collection )
            {
                ( (Collection<String>) scopes ).add( scope );
            }
            else if ( !scopes.equals( scope ) )
            {
                Collection<Object> set = new HashSet<Object>();
                set.add( scopes );
                set.add( scope );
                scopes = set;
            }
        }

        /**
         * Gets the derived optionalities of the dependency. In general, the same dependency node could be reached via
         * different paths and each path might result in a different derived optionality.
         * 
         * @return A bit field consisting of {@link ConflictResolver.ConflictItem#OPTIONAL_FALSE} and/or
         *         {@link ConflictResolver.ConflictItem#OPTIONAL_TRUE} indicating the derived optionalities the
         *         dependency was encountered with.
         */
        public int getOptionalities()
        {
            return optionalities;
        }

        void addOptional( boolean optional )
        {
            optionalities |= optional ? OPTIONAL_TRUE : OPTIONAL_FALSE;
        }

        @Override
        public String toString()
        {
            return node + " @ " + depth + " < " + artifact;
        }

    }

    /**
     * A context used to hold information that is relevant for resolving version and scope conflicts.
     * 
     * @see VersionSelector
     * @see ScopeSelector
     * @noinstantiate This class is not intended to be instantiated by clients in production code, the constructor may
     *                change without notice and only exists to enable unit testing.
     */
    public static final class ConflictContext
    {

        final DependencyNode root;

        final Map<?, ?> conflictIds;

        final Collection<ConflictItem> items;

        Object conflictId;

        ConflictItem winner;

        String scope;

        Boolean optional;

        ConflictContext( DependencyNode root, Map<?, ?> conflictIds, Collection<ConflictItem> items )
        {
            this.root = root;
            this.conflictIds = conflictIds;
            this.items = Collections.unmodifiableCollection( items );
        }

        /**
         * Creates a new conflict context.
         * 
         * @param root The root node of the dependency graph, must not be {@code null}.
         * @param conflictId The conflict id for the set of conflicting dependencies in this context, must not be
         *            {@code null}.
         * @param conflictIds The mapping from dependency node to conflict id, must not be {@code null}.
         * @param items The conflict items in this context, must not be {@code null}.
         * @noreference This class is not intended to be instantiated by clients in production code, the constructor may
         *              change without notice and only exists to enable unit testing.
         */
        public ConflictContext( DependencyNode root, Object conflictId, Map<DependencyNode, Object> conflictIds,
                                Collection<ConflictItem> items )
        {
            this( root, conflictIds, items );
            this.conflictId = conflictId;
        }

        /**
         * Gets the root node of the dependency graph being transformed.
         * 
         * @return The root node of the dependeny graph, never {@code null}.
         */
        public DependencyNode getRoot()
        {
            return root;
        }

        /**
         * Determines whether the specified dependency node belongs to this conflict context.
         * 
         * @param node The dependency node to check, must not be {@code null}.
         * @return {@code true} if the given node belongs to this conflict context, {@code false} otherwise.
         */
        public boolean isIncluded( DependencyNode node )
        {
            return conflictId.equals( conflictIds.get( node ) );
        }

        /**
         * Gets the collection of conflict items in this context.
         * 
         * @return The (read-only) collection of conflict items in this context, never {@code null}.
         */
        public Collection<ConflictItem> getItems()
        {
            return items;
        }

        /**
         * Gets the conflict item which has been selected as the winner among the conflicting dependencies.
         * 
         * @return The winning conflict item or {@code null} if not set yet.
         */
        public ConflictItem getWinner()
        {
            return winner;
        }

        /**
         * Sets the conflict item which has been selected as the winner among the conflicting dependencies.
         * 
         * @param winner The winning conflict item, may be {@code null}.
         */
        public void setWinner( ConflictItem winner )
        {
            this.winner = winner;
        }

        /**
         * Gets the effective scope of the winning dependency.
         * 
         * @return The effective scope of the winning dependency or {@code null} if none.
         */
        public String getScope()
        {
            return scope;
        }

        /**
         * Sets the effective scope of the winning dependency.
         * 
         * @param scope The effective scope, may be {@code null}.
         */
        public void setScope( String scope )
        {
            this.scope = scope;
        }

        /**
         * Gets the effective optional flag of the winning dependency.
         * 
         * @return The effective optional flag or {@code null} if none.
         */
        public Boolean getOptional()
        {
            return optional;
        }

        /**
         * Sets the effective optional flag of the winning dependency.
         * 
         * @param optional The effective optional flag, may be {@code null}.
         */
        public void setOptional( Boolean optional )
        {
            this.optional = optional;
        }

        @Override
        public String toString()
        {
            return winner + " @ " + scope + " < " + items;
        }

    }

    /**
     * An extension point of {@link ConflictResolver} that determines the winner among conflicting dependencies. The
     * winning node (and its children) will be retained in the dependency graph, the other nodes will get removed. The
     * version selector does not need to deal with potential scope conflicts, these will be addressed afterwards by the
     * {@link ScopeSelector}. Implementations must be stateless.
     */
    public static abstract class VersionSelector
    {

        /**
         * Retrieves the version selector for use during the specified graph transformation. The conflict resolver calls
         * this method once per
         * {@link ConflictResolver#transformGraph(DependencyNode, DependencyGraphTransformationContext)} invocation to
         * allow implementations to prepare any auxiliary data that is needed for their operation. Given that
         * implementations need to be stateless, a new instance needs to be returned to hold such auxiliary data. The
         * default implementation simply returns the current instance which is appropriate for implementations which do
         * not require auxiliary data.
         * 
         * @param root The root node of the (possibly cyclic!) graph to transform, must not be {@code null}.
         * @param context The graph transformation context, must not be {@code null}.
         * @return The scope deriver to use for the given graph transformation, never {@code null}.
         * @throws RepositoryException If the instance could not be retrieved.
         */
        public VersionSelector getInstance( DependencyNode root, DependencyGraphTransformationContext context )
            throws RepositoryException
        {
            return this;
        }

        /**
         * Determines the winning node among conflicting dependencies. Implementations will usually iterate
         * {@link ConflictContext#getItems()}, inspect {@link ConflictItem#getNode()} and eventually call
         * {@link ConflictContext#setWinner(ConflictResolver.ConflictItem)} to deliver the winner. Failure to select a
         * winner will automatically fail the entire conflict resolution.
         * 
         * @param context The conflict context, must not be {@code null}.
         * @throws RepositoryException If the version selection failed.
         */
        public abstract void selectVersion( ConflictContext context )
            throws RepositoryException;

    }

    /**
     * An extension point of {@link ConflictResolver} that determines the effective scope of a dependency from a
     * potentially conflicting set of {@link ScopeDeriver derived scopes}. The scope selector gets invoked after the
     * {@link VersionSelector} has picked the winning node. Implementations must be stateless.
     */
    public static abstract class ScopeSelector
    {

        /**
         * Retrieves the scope selector for use during the specified graph transformation. The conflict resolver calls
         * this method once per
         * {@link ConflictResolver#transformGraph(DependencyNode, DependencyGraphTransformationContext)} invocation to
         * allow implementations to prepare any auxiliary data that is needed for their operation. Given that
         * implementations need to be stateless, a new instance needs to be returned to hold such auxiliary data. The
         * default implementation simply returns the current instance which is appropriate for implementations which do
         * not require auxiliary data.
         * 
         * @param root The root node of the (possibly cyclic!) graph to transform, must not be {@code null}.
         * @param context The graph transformation context, must not be {@code null}.
         * @return The scope selector to use for the given graph transformation, never {@code null}.
         * @throws RepositoryException If the instance could not be retrieved.
         */
        public ScopeSelector getInstance( DependencyNode root, DependencyGraphTransformationContext context )
            throws RepositoryException
        {
            return this;
        }

        /**
         * Determines the effective scope of the dependency given by {@link ConflictContext#getWinner()}.
         * Implementations will usually iterate {@link ConflictContext#getItems()}, inspect
         * {@link ConflictItem#getScopes()} and eventually call {@link ConflictContext#setScope(String)} to deliver the
         * effective scope.
         * 
         * @param context The conflict context, must not be {@code null}.
         * @throws RepositoryException If the scope selection failed.
         */
        public abstract void selectScope( ConflictContext context )
            throws RepositoryException;

    }

    /**
     * An extension point of {@link ConflictResolver} that determines the scope of a dependency in relation to the scope
     * of its parent. Implementations must be stateless.
     */
    public static abstract class ScopeDeriver
    {

        /**
         * Retrieves the scope deriver for use during the specified graph transformation. The conflict resolver calls
         * this method once per
         * {@link ConflictResolver#transformGraph(DependencyNode, DependencyGraphTransformationContext)} invocation to
         * allow implementations to prepare any auxiliary data that is needed for their operation. Given that
         * implementations need to be stateless, a new instance needs to be returned to hold such auxiliary data. The
         * default implementation simply returns the current instance which is appropriate for implementations which do
         * not require auxiliary data.
         * 
         * @param root The root node of the (possibly cyclic!) graph to transform, must not be {@code null}.
         * @param context The graph transformation context, must not be {@code null}.
         * @return The scope deriver to use for the given graph transformation, never {@code null}.
         * @throws RepositoryException If the instance could not be retrieved.
         */
        public ScopeDeriver getInstance( DependencyNode root, DependencyGraphTransformationContext context )
            throws RepositoryException
        {
            return this;
        }

        /**
         * Determines the scope of a dependency in relation to the scope of its parent. Implementors need to call
         * {@link ScopeContext#setDerivedScope(String)} to deliver the result of their calculation. If said method is
         * not invoked, the conflict resolver will assume the scope of the child dependency remains unchanged.
         * 
         * @param context The scope context, must not be {@code null}.
         * @throws RepositoryException If the scope deriviation failed.
         */
        public abstract void deriveScope( ScopeContext context )
            throws RepositoryException;

    }

    /**
     * An extension point of {@link ConflictResolver} that determines the effective optional flag of a dependency from a
     * potentially conflicting set of derived optionalities. The optionality selector gets invoked after the
     * {@link VersionSelector} has picked the winning node. Implementations must be stateless.
     */
    public static abstract class OptionalitySelector
    {

        /**
         * Retrieves the optionality selector for use during the specified graph transformation. The conflict resolver
         * calls this method once per
         * {@link ConflictResolver#transformGraph(DependencyNode, DependencyGraphTransformationContext)} invocation to
         * allow implementations to prepare any auxiliary data that is needed for their operation. Given that
         * implementations need to be stateless, a new instance needs to be returned to hold such auxiliary data. The
         * default implementation simply returns the current instance which is appropriate for implementations which do
         * not require auxiliary data.
         * 
         * @param root The root node of the (possibly cyclic!) graph to transform, must not be {@code null}.
         * @param context The graph transformation context, must not be {@code null}.
         * @return The optionality selector to use for the given graph transformation, never {@code null}.
         * @throws RepositoryException If the instance could not be retrieved.
         */
        public OptionalitySelector getInstance( DependencyNode root, DependencyGraphTransformationContext context )
            throws RepositoryException
        {
            return this;
        }

        /**
         * Determines the effective optional flag of the dependency given by {@link ConflictContext#getWinner()}.
         * Implementations will usually iterate {@link ConflictContext#getItems()}, inspect
         * {@link ConflictItem#getOptionalities()} and eventually call {@link ConflictContext#setOptional(Boolean)} to
         * deliver the effective optional flag.
         * 
         * @param context The conflict context, must not be {@code null}.
         * @throws RepositoryException If the optionality selection failed.
         */
        public abstract void selectOptionality( ConflictContext context )
            throws RepositoryException;

    }

}
