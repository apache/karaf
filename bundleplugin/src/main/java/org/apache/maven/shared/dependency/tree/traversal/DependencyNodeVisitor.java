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

/**
 * Defines a hierarchical visitor for processing dependency node trees.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyNodeVisitor.java 661727 2008-05-30 14:21:49Z bentmann $
 * @since 1.1
 */
public interface DependencyNodeVisitor
{
    /**
     * Starts the visit to the specified dependency node.
     * 
     * @param node
     *            the dependency node to visit
     * @return <code>true</code> to visit the specified dependency node's children, <code>false</code> to skip the
     *         specified dependency node's children and proceed to its next sibling
     */
    boolean visit( DependencyNode node );

    /**
     * Ends the visit to to the specified dependency node.
     * 
     * @param node
     *            the dependency node to visit
     * @return <code>true</code> to visit the specified dependency node's next sibling, <code>false</code> to skip
     *         the specified dependency node's next siblings and proceed to its parent
     */
    boolean endVisit( DependencyNode node );
}
