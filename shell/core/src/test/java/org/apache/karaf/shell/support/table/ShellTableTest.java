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
package org.apache.karaf.shell.support.table;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.PrintStream;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.junit.Test;

public class ShellTableTest {

    @Test
    public void testLongValueFull() {
        ShellTable table = new ShellTable().forceAscii();
        table.separator("|");
        table.column("col1");
        table.column("col2").maxSize(-1);
        table.addRow().addContent("my first column value", "my second column value is quite long");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals(String.format("%s%n","my first column value\tmy second column value is quite long"), baos.toString());
    }

    @Test
    public void testLongValueCut() {
        ShellTable table = new ShellTable().forceAscii();
        table.separator("|");
        table.column("col1");
        table.column("col2").maxSize(-1);
        table.addRow().addContent("my first column value", "my second column value is quite long");
        table.size(50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals(String.format("%s%n","my first column value\tmy second column value is q"), baos.toString());
    }

    @Test
    public void testLongValueMultiline() {
        ShellTable table = new ShellTable().forceAscii();
        table.separator("|");
        table.column("col1");
        table.column("col2").maxSize(-1).wrap();
        table.addRow().addContent("my first column value", "my second column value is quite long");
        table.size(50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals(String.format("%1$s\n%2$s%n","my first column value\tmy second column value is",
                "                     \tquite long"), baos.toString());
    }

    @Test
    public void testCP1252() throws Exception {
        testNonUtf8("cp1252");
    }

    @Test
    public void testANSI() throws Exception {
        testNonUtf8("ANSI_X3.4-1968");
    }

    private void testNonUtf8(String encoding)  throws Exception {
        ShellTable table = new ShellTable();
        table.column("col1");
        table.column("col2").maxSize(-1).wrap();
        table.addRow().addContent("my first column value", "my second column value is quite long");
        table.size(50);

        ThreadIO tio = new ThreadIOImpl();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, false, encoding);
        table.print(ps, true);
        tio.setStreams(new FileInputStream(FileDescriptor.in), ps, ps);

        table.print(System.out);

        assertEquals(
                "col1                  | col2\n" +
                "----------------------+---------------------------\n" +
                "my first column value | my second column value is\n" +
                "                      | quite long\n" +
                "col1                  | col2\n" +
                "----------------------+---------------------------\n" +
                "my first column value | my second column value is\n" +
                "                      | quite long\n",
                getString(baos));

    }
    
    @Test
    public void testColoredTable() {
        ShellTable table = new ShellTable().forceAscii();
        table.separator("|");
        table.column("State").colorProvider(cellContent -> {
			if(cellContent.contains("Active"))
				return SimpleAnsi.COLOR_GREEN;
			
			if(cellContent.contains("Resolved"))
				return SimpleAnsi.COLOR_YELLOW;
			return null;
		});

        table.column("Description").maxSize(-1);
        table.addRow().addContent("Normal", "This should have default color");
        table.addRow().addContent("Active", "Green color");
        table.addRow().addContent("This is Resolved", "Yellow color");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals("Normal          \tThis should have default color\n" +
        		"[32mActive          [39m\tGreen color\n" +
        		"[33mThis is Resolved[39m\tYellow color\n", baos.toString());
    }

    @Test
    public void testNoFormatSeparatorDefault() {
        ShellTable table = new ShellTable();
        table.column("col1");
        table.column("col2").maxSize(-1);
        table.addRow().addContent("my first column value", "my second column value is quite long");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals(String.format("%s%n","my first column value\tmy second column value is quite long"), baos.toString());
    }

    @Test
    public void testNoFormatSeparatorAsciiDefault() {
        ShellTable table = new ShellTable();
        table.separator(" | ");
        table.column("col1");
        table.column("col2").maxSize(-1);
        table.addRow().addContent("my first column value", "my second column value is quite long");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals(String.format("%s%n","my first column value\tmy second column value is quite long"), baos.toString());
    }

    @Test
    public void testNoFormatSeparatorForcedAscii() {
        ShellTable table = new ShellTable().forceAscii();
        table.column("col1");
        table.column("col2").maxSize(-1);
        table.addRow().addContent("my first column value", "my second column value is quite long");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals(String.format("%s%n","my first column value\tmy second column value is quite long"), baos.toString());
    }

    @Test
    public void testNoFormatSeparatorUtf8Default() {
        ShellTable table = new ShellTable();
        table.separator(" │ ");
        table.column("col1");
        table.column("col2").maxSize(-1);
        table.addRow().addContent("my first column value", "my second column value is quite long");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        table.print(new PrintStream(baos), false);
        assertEquals(String.format("%s%n","my first column value\tmy second column value is quite long"), baos.toString());
    }

    private String getString(ByteArrayOutputStream stream) {
        return stream.toString().replace("\r\n", "\n");
    }
}
