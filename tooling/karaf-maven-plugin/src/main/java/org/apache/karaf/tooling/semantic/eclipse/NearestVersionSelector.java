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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ConflictContext;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ConflictItem;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.VersionSelector;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.collection.UnsolvableVersionConflictException;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.PathRecordingDependencyVisitor;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;


/**
 * A version selector for use with {@link ConflictResolver} that resolves version conflicts using a nearest-wins
 * strategy. If there is no single node that satisfies all encountered version ranges, the selector will fail.
 */
public final class NearestVersionSelector
    extends VersionSelector
{

    /**
     * Creates a new instance of this version selector.
     */
    public NearestVersionSelector()
    {
    }

    @Override
    public void selectVersion( ConflictContext context )
        throws RepositoryException
    {
        ConflictGroup group = new ConflictGroup();
        for ( ConflictItem item : context.getItems() )
        {
            DependencyNode node = item.getNode();
            VersionConstraint constraint = node.getVersionConstraint();

            boolean backtrack = false;
            boolean hardConstraint = !constraint.getRanges().isEmpty(); // XXX

            if ( hardConstraint )
            {
                if ( group.constraints.add( constraint ) )
                {
                    if ( group.winner != null && !constraint.containsVersion( group.winner.getNode().getVersion() ) )
                    {
                        backtrack = true;
                    }
                }
            }

            if ( isAcceptable( group, node.getVersion() ) )
            {
                group.candidates.add( item );

                if ( backtrack )
                {
                    backtrack( group, context );
                }
                else if ( group.winner == null || isNearer( item, group.winner ) )
                {
                    group.winner = item;
                }
            }
            else if ( backtrack )
            {
                backtrack( group, context );
            }
        }
        context.setWinner( group.winner );
    }

    private void backtrack( ConflictGroup group, ConflictContext context )
        throws UnsolvableVersionConflictException
    {
        group.winner = null;

        for ( Iterator<ConflictItem> it = group.candidates.iterator(); it.hasNext(); )
        {
            ConflictItem candidate = it.next();

            if ( !isAcceptable( group, candidate.getNode().getVersion() ) )
            {
                it.remove();
            }
            else if ( group.winner == null || isNearer( candidate, group.winner ) )
            {
                group.winner = candidate;
            }
        }

        if ( group.winner == null )
        {
            throw newFailure( context );
        }
    }

    private boolean isAcceptable( ConflictGroup group, Version version )
    {
        for ( VersionConstraint constraint : group.constraints )
        {
            if ( !constraint.containsVersion( version ) )
            {
                return false;
            }
        }
        return true;
    }

    private boolean isNearer( ConflictItem item1, ConflictItem item2 )
    {
        if ( item1.isSibling( item2 ) )
        {
            return item1.getNode().getVersion().compareTo( item2.getNode().getVersion() ) > 0;
        }
        else
        {
            return item1.getDepth() < item2.getDepth();
        }
    }

    private UnsolvableVersionConflictException newFailure( final ConflictContext context )
    {
        DependencyFilter filter = new DependencyFilter()
        {
            public boolean accept( DependencyNode node, List<DependencyNode> parents )
            {
                return context.isIncluded( node );
            }
        };
        PathRecordingDependencyVisitor visitor = new PathRecordingDependencyVisitor( filter );
        context.getRoot().accept( visitor );
        return new UnsolvableVersionConflictException( visitor.getPaths(), "bada-boom" ); // XXX
    }

    static final class ConflictGroup
    {

        final Collection<VersionConstraint> constraints;

        final Collection<ConflictItem> candidates;

        ConflictItem winner;

        public ConflictGroup()
        {
            constraints = new HashSet<VersionConstraint>();
            candidates = new ArrayList<ConflictItem>( 64 );
        }

        @Override
        public String toString()
        {
            return String.valueOf( winner );
        }

    }

}
