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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * {@link Iterator} for {@link DependencyNode} implementing a preoder traversal.
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id: DependencyTreePreorderIterator.java 661727 2008-05-30 14:21:49Z bentmann $
 */
public class DependencyTreePreorderIterator
    implements Iterator
{
    private Stack nodesToProcess = new Stack();

    public DependencyTreePreorderIterator( DependencyNode rootNode )
    {
        nodesToProcess.push( rootNode );
    }

    public boolean hasNext()
    {
        return !nodesToProcess.isEmpty();
    }

    public Object next()
    {
        if ( !hasNext() )
        {
            throw new NoSuchElementException();
        }
        DependencyNode currentNode = (DependencyNode) nodesToProcess.pop();
        List children = currentNode.getChildren();
        if ( children != null )
        {
            for ( int i = children.size() - 1; i >= 0; i-- )
            {
                nodesToProcess.push( children.get( i ) );
            }
        }
        return currentNode;
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
