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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a Maven project's dependency tree.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyTree.java 661727 2008-05-30 14:21:49Z bentmann $
 * @deprecated As of 1.1, replaced by the dependency tree root {@link DependencyNode}
 */
public class DependencyTree
{
    // fields -----------------------------------------------------------------

    private final DependencyNode rootNode;

    private final Collection nodes;

    // constructors -----------------------------------------------------------

    /**
     * Create a tree initialized to the arguments
     * 
     * @param rootNode
     * @param nodes
     */
    public DependencyTree( DependencyNode rootNode, Collection nodes )
    {
        this.rootNode = rootNode;
        this.nodes = nodes;
    }

    // public methods ---------------------------------------------------------

    public DependencyNode getRootNode()
    {
        return rootNode;
    }

    public Collection getNodes()
    {
        return nodes;
    }

    public List getArtifacts()
    {
        List artifacts = new ArrayList();

        Iterator it = getNodes().iterator();
        while ( it.hasNext() )
        {
            DependencyNode node = (DependencyNode) it.next();
            artifacts.add( node.getArtifact() );
        }

        return artifacts;
    }

    public String toString()
    {
        return getRootNode().toString();
    }

    /**
     * @see DependencyNode#iterator()
     */
    public Iterator iterator()
    {
        return getRootNode().iterator();
    }

    /**
     * @see DependencyNode#preorderIterator()
     */
    public Iterator preorderIterator()
    {
        return getRootNode().preorderIterator();
    }

    /**
     * @see DependencyNode#inverseIterator()
     */
    public Iterator inverseIterator()
    {
        return getRootNode().inverseIterator();
    }
}
