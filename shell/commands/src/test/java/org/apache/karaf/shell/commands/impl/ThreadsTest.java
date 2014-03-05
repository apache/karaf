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
package org.apache.karaf.shell.commands.impl;

import org.junit.Test;

/**
 * These are not real tests as they do no assertions. They simply help to see how the layout will look like.
 */
public class ThreadsTest {
    @Test
    public void testThreadlist() throws Exception {
        ThreadsAction action = new ThreadsAction();
        action.list = true;
        action.execute();
    }
    
    @Test
    public void testThreadInfo() throws Exception {
        ThreadsAction action = new ThreadsAction();
        action.id = 1L;
        action.execute();
    }

    @Test
    public void testThreadTree() throws Exception {
        ThreadsAction action = new ThreadsAction();
        action.tree = true;
        action.execute();
    }

    @Test
    public void testThreadDump() throws Exception {
        ThreadsAction action = new ThreadsAction();
        action.execute();
    }
}
