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
package org.apache.felix.karaf.shell.dev.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tree that can be written to the console.
 *
 * The output will look like this:
 * <pre>
 * root
 * +- child1
 * |  +- grandchild
 * +- child2
 * </pre>
 */
public class Tree<T> extends Node<T> {

    /**
     * Creates a new tree with the given root node
     *
     * @param root the root node
     */
    public Tree(T root) {
        super(root);
    }

    /**
     * Write the tree to a PrintStream, using the default toString() method to output the node values
     *
     * @param stream
     */
    public void write(PrintStream stream) {
        write(new PrintWriter(stream));
    }

    /**
     * Write the tree to a PrintStream, using the provided converter to output the node values
     *
     * @param stream
     * @param converter
     */
    public void write(PrintStream stream, Converter<T> converter) {
        write(new PrintWriter(stream), converter);
    }

    /**
     * Write the tree to a PrintWriter, using the default toString() method to output the node values
     *
     * @param writer
     */
    public void write(PrintWriter writer) {
        write(writer, new Converter() {
            public String toString(Node node) {
                return node.getValue().toString();
            }
        });
    }

    /**
     * Write the tree to a PrintWriter, using the provided converter to output the node values
     *
     * @param writer
     * @param converter
     */
    public void write(PrintWriter writer, Converter<T> converter) {
        writer.printf("%s%n", converter.toString(this));
        for (Node<T> child : getChildren()) {
            child.write(writer, converter);
        }
        writer.flush();
    }

    /**
     * Interface to convert node values to string
     *
     * @param <T> the object type for the node value
     */
    public static interface Converter<T> {

        public String toString(Node<T> node);

    }
}
