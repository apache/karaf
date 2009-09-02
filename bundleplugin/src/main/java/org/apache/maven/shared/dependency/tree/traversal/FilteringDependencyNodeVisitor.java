package org.apache.maven.shared.dependency.tree.traversal;

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
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;

/**
 * A dependency node visitor that filters nodes and delegates to another visitor.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: FilteringDependencyNodeVisitor.java 661727 2008-05-30 14:21:49Z bentmann $
 * @since 1.1
 */
public class FilteringDependencyNodeVisitor implements DependencyNodeVisitor
{
    // fields -----------------------------------------------------------------

    /**
     * The dependency node visitor to delegate to.
     */
    private final DependencyNodeVisitor visitor;

    /**
     * The dependency node filter to apply before delegation.
     */
    private final DependencyNodeFilter filter;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node visitor that delegates nodes that are accepted by the specified filter to the specified
     * visitor.
     * 
     * @param visitor
     *            the dependency node visitor to delegate to
     * @param filter
     *            the dependency node filter to apply before delegation
     */
    public FilteringDependencyNodeVisitor( DependencyNodeVisitor visitor, DependencyNodeFilter filter )
    {
        this.visitor = visitor;
        this.filter = filter;
    }

    // DependencyNodeVisitor methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean visit( DependencyNode node )
    {
        boolean visit;

        if ( filter.accept( node ) )
        {
            visit = visitor.visit( node );
        }
        else
        {
            visit = true;
        }

        return visit;
    }

    /**
     * {@inheritDoc}
     */
    public boolean endVisit( DependencyNode node )
    {
        boolean visit;

        if ( filter.accept( node ) )
        {
            visit = visitor.endVisit( node );
        }
        else
        {
            visit = true;
        }

        return visit;
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the dependency node visitor that this visitor delegates to.
     * 
     * @return the dependency node visitor
     */
    public DependencyNodeVisitor getDependencyNodeVisitor()
    {
        return visitor;
    }

    /**
     * Gets the dependency node filter that this visitor applies before delegation.
     * 
     * @return the dependency node filter
     */
    public DependencyNodeFilter getDependencyNodeFilter()
    {
        return filter;
    }
}
