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
package org.apache.karaf.shell.support.ansi;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AnsiSplitterTest {

    @Test
    public void testWindow() throws IOException {
        String text = "\u001B[1mThis is bold.\u001B[22m";
        assertEquals("\u001B[1mis\u001B[0m", AnsiSplitter.substring(text, 5, 7, 4));
        assertEquals(13, AnsiSplitter.length(text, 4));
    }

}
