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

import java.io.PrintStream;
import java.io.StringWriter;

import junit.framework.Assert;

import org.apache.commons.io.output.WriterOutputStream;
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

        StringWriter writer = new StringWriter();
        PrintStream out = new PrintStream(new WriterOutputStream(writer));
        table.print(out, true);
        out.flush();
        String expected = 
                "   id | Name                 |                Centered                \n" + 
                "----------------------------------------------------------------------\n" + 
                "    1 | Test                 |               Description              \n" + 
                "   20 | My name              |               Description              \n" + 
                "12345 | A very long text tha | A very long text that should not be cut\n";
        Assert.assertEquals(expected, getString(writer));
    }

    @Test
    public void testGrow() {
        ShellTable table = new ShellTable().size(10);
        table.column(new Col("1"));
        table.column(new Col("2"));

        table.addRow().addContent("1", "2");

        StringWriter writer = new StringWriter();
        PrintStream out = new PrintStream(new WriterOutputStream(writer));
        table.print(out, true);
        out.flush();
        String expected = 
                "1      | 2\n" + 
        		"----------\n" + 
        		"1      | 2\n";
        Assert.assertEquals(expected, getString(writer));
    }

    @Test
    public void testShrink() {
        ShellTable table = new ShellTable().size(10);
        table.column(new Col("1").maxSize(5));
        table.column(new Col("2").alignRight());

        table.addRow().addContent("quite long", "and here an even longer text");

        StringWriter writer = new StringWriter();
        PrintStream out = new PrintStream(new WriterOutputStream(writer));
        table.print(out, true);
        out.flush();
        String expected = //
                  "1     |  2\n" //
                + "----------\n" //
                + "quite |  a\n";
        Assert.assertEquals(expected, getString(writer));
    }

    @Test
    public void testTooSmall() {
        ShellTable table = new ShellTable().size(2);
        table.column(new Col("1").maxSize(5));
        table.column(new Col("2").alignRight());

        table.addRow().addContent("quite long", "and here an even longer text");

        StringWriter writer = new StringWriter();
        PrintStream out = new PrintStream(new WriterOutputStream(writer));
        table.print(out, true);
        out.flush();
        String expected = //
                  "1     | \n" + // 
                  "--------\n" + //
                  "quite | \n";
        Assert.assertEquals(expected, getString(writer));
    }

    @Test
    public void testNoFormat() {
        ShellTable table = new ShellTable();
        table.column(new Col("first"));
        table.column(new Col("second"));

        table.addRow().addContent("first column", "second column");

        StringWriter writer = new StringWriter();
        PrintStream out = new PrintStream(new WriterOutputStream(writer));
        table.print(out, false);
        out.flush();
        String expected = "first column\tsecond column\n";
        Assert.assertEquals(expected, getString(writer));
    }

    @Test
    public void testNoFormatWithCustomSeparator() {
        ShellTable table = new ShellTable();
        table.separator(";");
        table.column(new Col("first"));
        table.column(new Col("second"));

        table.addRow().addContent("first column", "second column");

        StringWriter writer = new StringWriter();
        PrintStream out = new PrintStream(new WriterOutputStream(writer));
        table.print(out, false);
        out.flush();
        String expected = "first column;second column\n";
        Assert.assertEquals(expected, getString(writer));
    }
    
    private String getString(StringWriter writer) {
        return writer.getBuffer().toString().replace("\r\n", "\n");
    }

}
