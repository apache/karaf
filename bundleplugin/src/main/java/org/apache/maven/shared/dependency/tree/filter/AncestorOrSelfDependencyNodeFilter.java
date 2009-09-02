package org.apache.maven.shared.dependency.tree.filter;

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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.shared.dependency.tree.DependencyNode;

/**
 * A dependency node filter than only accepts nodes that are ancestors of, or equal to, a given list of nodes.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: AncestorOrSelfDependencyNodeFilter.java 661727 2008-05-30 14:21:49Z bentmann $
 * @since 1.1
 */
public class AncestorOrSelfDependencyNodeFilter implements DependencyNodeFilter
{
    // fields -----------------------------------------------------------------

    /**
     * The list of nodes that this filter accepts ancestors-or-self of.
     */
    private final List descendantNodes;

    // constructors -----------------------------------------------------------

    public AncestorOrSelfDependencyNodeFilter( DependencyNode descendantNode )
    {
        this( Collections.singletonList( descendantNode ) );
    }

    /**
     * Creates a dependency node filter that only accepts nodes that are ancestors of, or equal to, the specified list
     * of nodes.
     * 
     * @param descendantNodes
     *            the list of nodes to accept ancestors-or-self of
     */
    public AncestorOrSelfDependencyNodeFilter( List descendantNodes )
    {
        this.descendantNodes = descendantNodes;
    }

    // DependencyNodeFilter methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean accept( DependencyNode node )
    {
        boolean accept = false;

        for ( Iterator iterator = descendantNodes.iterator(); !accept && iterator.hasNext(); )
        {
            DependencyNode descendantNode = (DependencyNode) iterator.next();

            if ( isAncestorOrSelf( node, descendantNode ) )
            {
                accept = true;
            }
        }

        return accept;
    }

    // private methods --------------------------------------------------------

    /**
     * Gets whether the first dependency node is an ancestor-or-self of the second.
     * 
     * @param ancestorNode
     *            the ancestor-or-self dependency node
     * @param descendantNode
     *            the dependency node to test
     * @return <code>true</code> if <code>ancestorNode</code> is an ancestor, or equal to,
     *         <code>descendantNode</code>
     */
    private boolean isAncestorOrSelf( DependencyNode ancestorNode, DependencyNode descendantNode )
    {
        boolean ancestor = false;

        while ( !ancestor && descendantNode != null )
        {
            ancestor = ancestorNode.equals( descendantNode );

            descendantNode = descendantNode.getParent();
        }

        return ancestor;
    }
}
