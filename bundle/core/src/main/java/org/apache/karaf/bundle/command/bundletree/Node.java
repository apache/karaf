/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.bundle.command.bundletree;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represent a node in a {@link Tree}
 */
public class Node<T> {
    
    private final T value;
    private Node<T> parent;
    private List<Node<T>> children = new LinkedList<>();

    /**
     * Create a new node. Only meant for wrapper use,
     * new nodes should be added using the {@link #addChild(Object)} method
     *
     * @param value the node value
     */
    protected Node(T value) {
        super();
        this.value = value;
    }

    /**
     * Create a new node. Only meant for wrapper use,
     * new nodes should be added using the {@link #addChild(Object)} method.
     *
     * @param value the node value.
     * @param parent the parent node.
     */
    protected Node(T value, Node<T> parent) {
        this(value);
        this.parent = parent;
    }

    /**
     * Access the node's value.
     *
     * @return T the node's value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Access the node's child nodes.
     *
     * @return the list of node children.
     */
    public List<Node<T>> getChildren() {
        return children;
    }

    /**
     * Add a child to this node.
     *
     * @param value the child's value.
     * @return the child node.
     */
    public Node<T> addChild(T value) {
        Node<T> node = new Node<>(value, this);
        children.add(node);
        return node;
    }

    /**
     * Give a set of values in the tree.
     *
     * @return the set of values.
     */
    public Set<T> flatten() {
        Set<T> result = new HashSet<>();
        result.add(getValue());
        for (Node<T> child : getChildren()) {
            result.addAll(child.flatten());
        }
        return result;
    }

    /**
     * Check if the node has an ancestor that represents the given value.
     *
     * @param value the node value.
     * @return <code>true</code> if there's an ancestor that represents the value.
     */
    public boolean hasAncestor(T value) {
        if (parent == null) {
            return false;
        } else {
            return value.equals(parent.value) || parent.hasAncestor(value);
        }
    }
    
    public boolean hasChild(T value) {
        for (Node<T> child : getChildren()) {
            if (value.equals(child.getValue())) {
                return true;
            }
        }
        return false;
    }

    /*
     * Write this node to the PrintWriter.  It should be indented one step
     * further for every element in the indents array.  If an element in the
     * array is <code>true</code>, there should be a | to connect to the next
     * sibling.
     */
    protected void write(PrintWriter writer, Tree.Converter<T> converter, boolean... indents) {
        for (boolean indent : indents) {
            writer.printf("%-3s", indent ? "|" : "");
        }
        writer.printf("+- %s%n", converter.toString(this));
        for (Node<T> child : getChildren()) {
            child.write(writer, converter, concat(indents, hasNextSibling()));
        }
    }

    /*
     * Is this node the last child node for its parent
     * or is there a next sibling?
     */
    private boolean hasNextSibling() {
        if (parent == null) {
            return false;
        } else {
            return parent.getChildren().size() > 1
                    && parent.getChildren().indexOf(this) < parent.getChildren().size() - 1;
        }
    }

    /*
     * Add an element to the end of the array
     */
    private boolean[] concat(boolean[] array, boolean element) {
        boolean[] result = new boolean[array.length + 1];
        System.arraycopy(array, 0, result, 0, array.length);
        result[array.length] = element;
        return result;
    }
}
