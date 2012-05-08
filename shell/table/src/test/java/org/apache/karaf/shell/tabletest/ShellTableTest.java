/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.shell.tabletest;

import org.apache.karaf.shell.table.Col;
import org.apache.karaf.shell.table.Row;
import org.apache.karaf.shell.table.ShellTable;
import org.junit.Test;

public class ShellTableTest {

    @Test
    public void testTable() {
        ShellTable table = new ShellTable();
        table.column(new Col("id").alignRight().maxSize(5));
        table.column(new Col("Name").maxSize(20));
        table.column(new Col("Centered").alignCenter());
        
        table.addRow().addContent(1, "Test", "Description");
        table.addRow().addContent(20, "My name", "Description");
        
        Row row = table.addRow();
        row.addContent(123456789);
        row.addContent("A very long text that should be cut");
        row.addContent("A very long text that should not be cut");
        
        table.print(System.out);
    }
}
