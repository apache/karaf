/*******************************************************************************
 * Copyright (c) 2010, 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.apache.karaf.tooling.semantic.eclipse;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.transformer.TransformationContextKeys;


/**
 * A dependency graph transformer that identifies conflicting dependencies. When this transformer has executed, the
 * transformation context holds a {@code Map<DependencyNode, Object>} where dependency nodes that belong to the same
 * conflict group will have an equal conflict identifier. This map is stored using the key
 * {@link TransformationContextKeys#CONFLICT_IDS}.
 */
public final class ConflictMarker
    implements DependencyGraphTransformer
{

    /**
     * After the execution of this method, every DependencyNode with an attached dependency is member of one conflict
     * group.
     * 
     * @see DependencyGraphTransformer#transformGraph(DependencyNode, DependencyGraphTransformationContext)
     */
    public DependencyNode transformGraph( DependencyNode node, DependencyGraphTransformationContext context )
        throws RepositoryException
    {
        @SuppressWarnings( "unchecked" )
        Map<String, Object> stats = (Map<String, Object>) context.get( ContextKeys.STATS );
        long time1 = System.currentTimeMillis();

        Map<DependencyNode, Object> nodes = new IdentityHashMap<DependencyNode, Object>( 1024 );
        Map<Object, ConflictGroup> groups = new HashMap<Object, ConflictGroup>( 1024 );

        analyze( node, nodes, groups, new int[] { 0 } );

        long time2 = System.currentTimeMillis();

        Map<DependencyNode, Object> conflictIds = mark( nodes.keySet(), groups );

        context.put( TransformationContextKeys.CONFLICT_IDS, conflictIds );

        if ( stats != null )
        {
            long time3 = System.currentTimeMillis();
            stats.put( "ConflictMarker.analyzeTime", time2 - time1 );
            stats.put( "ConflictMarker.markTime", time3 - time2 );
            stats.put( "ConflictMarker.nodeCount", nodes.size() );
        }

        return node;
    }

    private void analyze( DependencyNode node, Map<DependencyNode, Object> nodes, Map<Object, ConflictGroup> groups,
                          int[] counter )
    {
        if ( nodes.put( node, Boolean.TRUE ) != null )
        {
            return;
        }

        Set<Object> keys = getKeys( node );
        if ( !keys.isEmpty() )
        {
            ConflictGroup group = null;
            boolean fixMappings = false;

            for ( Object key : keys )
            {
                ConflictGroup g = groups.get( key );

                if ( group != g )
                {
                    if ( group == null )
                    {
                        Set<Object> newKeys = merge( g.keys, keys );
                        if ( newKeys == g.keys )
                        {
                            group = g;
                            break;
                        }
                        else
                        {
                            group = new ConflictGroup( newKeys, counter[0]++ );
                            fixMappings = true;
                        }
                    }
                    else if ( g == null )
                    {
                        fixMappings = true;
                    }
                    else
                    {
                        Set<Object> newKeys = merge( g.keys, group.keys );
                        if ( newKeys == g.keys )
                        {
                            group = g;
                            fixMappings = false;
                            break;
                        }
                        else if ( newKeys != group.keys )
                        {
                            group = new ConflictGroup( newKeys, counter[0]++ );
                            fixMappings = true;
                        }
                    }
                }
            }

            if ( group == null )
            {
                group = new ConflictGroup( keys, counter[0]++ );
                fixMappings = true;
            }
            if ( fixMappings )
            {
                for ( Object key : group.keys )
                {
                    groups.put( key, group );
                }
            }
        }

        for ( DependencyNode child : node.getChildren() )
        {
            analyze( child, nodes, groups, counter );
        }
    }

    private Set<Object> merge( Set<Object> keys1, Set<Object> keys2 )
    {
        int size1 = keys1.size();
        int size2 = keys2.size();

        if ( size1 < size2 )
        {
            if ( keys2.containsAll( keys1 ) )
            {
                return keys2;
            }
        }
        else
        {
            if ( keys1.containsAll( keys2 ) )
            {
                return keys1;
            }
        }

        Set<Object> keys = new HashSet<Object>();
        keys.addAll( keys1 );
        keys.addAll( keys2 );
        return keys;
    }

    private Set<Object> getKeys( DependencyNode node )
    {
        Set<Object> keys;

        Dependency dependency = node.getDependency();

        if ( dependency == null )
        {
            keys = Collections.emptySet();
        }
        else
        {
            Object key = toKey( dependency.getArtifact() );

            if ( node.getRelocations().isEmpty() && node.getAliases().isEmpty() )
            {
                keys = Collections.singleton( key );
            }
            else
            {
                keys = new HashSet<Object>();
                keys.add( key );

                for ( Artifact relocation : node.getRelocations() )
                {
                    key = toKey( relocation );
                    keys.add( key );
                }

                for ( Artifact alias : node.getAliases() )
                {
                    key = toKey( alias );
                    keys.add( key );
                }
            }
        }

        return keys;
    }

    private Map<DependencyNode, Object> mark( Collection<DependencyNode> nodes, Map<Object, ConflictGroup> groups )
    {
        Map<DependencyNode, Object> conflictIds = new IdentityHashMap<DependencyNode, Object>( nodes.size() + 1 );

        for ( DependencyNode node : nodes )
        {
            Dependency dependency = node.getDependency();
            if ( dependency != null )
            {
                Object key = toKey( dependency.getArtifact() );
                conflictIds.put( node, groups.get( key ).index );
            }
        }

        return conflictIds;
    }

    private static Object toKey( Artifact artifact )
    {
        return new Key( artifact );
    }

    static class ConflictGroup
    {

        final Set<Object> keys;

        final int index;

        public ConflictGroup( Set<Object> keys, int index )
        {
            this.keys = keys;
            this.index = index;
        }

        @Override
        public String toString()
        {
            return String.valueOf( keys );
        }

    }

    static class Key
    {

        private final Artifact artifact;

        public Key( Artifact artifact )
        {
            this.artifact = artifact;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( obj == this )
            {
                return true;
            }
            else if ( !( obj instanceof Key ) )
            {
                return false;
            }
            Key that = (Key) obj;
            return artifact.getArtifactId().equals( that.artifact.getArtifactId() )
                && artifact.getGroupId().equals( that.artifact.getGroupId() )
                && artifact.getExtension().equals( that.artifact.getExtension() )
                && artifact.getClassifier().equals( that.artifact.getClassifier() );
        }

        @Override
        public int hashCode()
        {
            int hash = 17;
            hash = hash * 31 + artifact.getArtifactId().hashCode();
            hash = hash * 31 + artifact.getGroupId().hashCode();
            hash = hash * 31 + artifact.getClassifier().hashCode();
            hash = hash * 31 + artifact.getExtension().hashCode();
            return hash;
        }

        @Override
        public String toString()
        {
            return artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getClassifier() + ':'
                + artifact.getExtension();
        }

    }

}
