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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Test cases for {@link org.apache.felix.karaf.shell.dev.util.Tree}
 * and {@link org.apache.felix.karaf.shell.dev.util.Node}
 */
public class TreeTest {

    @Test
    public void writeTreeWithOneChild() throws IOException {
        Tree<String> tree = new Tree<String>("root");
        tree.addChild("child");

        BufferedReader reader = read(tree);

        assertEquals("root"     , reader.readLine());
        assertEquals("+- child" , reader.readLine());
    }

    @Test
    public void writeTreeWithOneChildAndNodeConverter() throws IOException {
        Tree<String> tree = new Tree<String>("root");
        tree.addChild("child");

        StringWriter writer = new StringWriter();
        tree.write(new PrintWriter(writer), new Tree.Converter<String>() {
            public String toString(Node<String> node) {
                return "my " + node.getValue();
            }
        });

        BufferedReader reader = new BufferedReader(new StringReader(writer.getBuffer().toString()));

        assertEquals("my root"     , reader.readLine());
        assertEquals("+- my child" , reader.readLine());
    }

    @Test
    public void writeTreeWithChildAndGrandChild() throws IOException {
        Tree<String> tree = new Tree<String>("root");
        Node<String> node = tree.addChild("child");
        node.addChild("grandchild");

        BufferedReader reader = read(tree);

        assertEquals("root"            , reader.readLine());
        assertEquals("+- child"        , reader.readLine());
        assertEquals("   +- grandchild", reader.readLine());
    }

    @Test
    public void writeTreeWithTwoChildrenAndOneGrandchild() throws IOException {
        Tree<String> tree = new Tree<String>("root");
        Node<String> child = tree.addChild("child1");
        child.addChild("grandchild");
        tree.addChild("child2");

        BufferedReader reader = read(tree);

        assertEquals("root"            , reader.readLine());
        assertEquals("+- child1"       , reader.readLine());
        assertEquals("|  +- grandchild", reader.readLine());
        assertEquals("+- child2"       , reader.readLine());
    }

    @Test
    public void flattenTree() throws IOException {
        Tree<String> tree = new Tree<String>("root");
        Node<String> child1 = tree.addChild("child1");
        child1.addChild("grandchild");
        Node child2 = tree.addChild("child2");
        child2.addChild("grandchild");

        Set<String> elements = tree.flatten();
        assertNotNull(elements);
        assertEquals(4, elements.size());
        assertTrue(elements.contains("root"));
        assertTrue(elements.contains("child1"));
        assertTrue(elements.contains("child2"));
        assertTrue(elements.contains("grandchild"));
    }

    private BufferedReader read(Tree<String> tree) {
        StringWriter writer = new StringWriter();
        tree.write(new PrintWriter(writer));

        BufferedReader reader = new BufferedReader(new StringReader(writer.getBuffer().toString()));
        return reader;
    }
}
