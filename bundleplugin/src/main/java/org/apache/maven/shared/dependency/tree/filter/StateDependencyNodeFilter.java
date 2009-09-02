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

import org.apache.maven.shared.dependency.tree.DependencyNode;

/**
 * A dependency node filter that accepts nodes depending on their state.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: StateDependencyNodeFilter.java 661727 2008-05-30 14:21:49Z bentmann $
 * @since 1.1
 */
public class StateDependencyNodeFilter implements DependencyNodeFilter
{
    // constants --------------------------------------------------------------

    /**
     * A dependency node filter that only accepts included nodes.
     */
    public static final StateDependencyNodeFilter INCLUDED = new StateDependencyNodeFilter( DependencyNode.INCLUDED );

    // fields -----------------------------------------------------------------

    /**
     * The state of dependency nodes to accept.
     * 
     * @see DependencyNode
     */
    private final int state;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node filter that only accepts dependency nodes of the specified state.
     * 
     * @param state
     *            the state of dependency nodes to accept
     * @throws IllegalArgumentException
     *             if the specified state is invalid
     */
    public StateDependencyNodeFilter( int state )
    {
        if ( state < DependencyNode.INCLUDED || state > DependencyNode.OMITTED_FOR_CYCLE )
        {
            throw new IllegalArgumentException( "Unknown state: " + state );
        }

        this.state = state;
    }

    // DependencyNodeFilter methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean accept( DependencyNode node )
    {
        return node.getState() == state;
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the dependency node state that this filter accepts.
     * 
     * @return the dependency node state that this filter accepts
     */
    public int getState()
    {
        return state;
    }
}
