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
package org.apache.felix.webconsole.plugins.memoryusage.internal;

import java.io.File;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.felix.shell.Command;

class MemoryUsageCommand implements Command
{

    private static final String HELP_CMD = "help";

    private static final String POOLS_CMD = "pools";

    private static final String DUMP_CMD = "dump";

    private static final String OPT_DUMP_ALL = "-a";

    private static final String LS_CMD = "ls";

    private static final String RM_CMD = "rm";

    private final MemoryUsageSupport support;

    MemoryUsageCommand(final MemoryUsageSupport support)
    {
        this.support = support;
    }

    public void execute(String commandLine, final PrintStream out, final PrintStream err)
    {

        final PrintWriterPrintHelper printHelper = new PrintWriterPrintHelper(out);

        // Parse the commandLine to get the mem command.
        final StringTokenizer st = new StringTokenizer(commandLine);
        // Ignore the invoking command.
        st.nextToken();
        // Try to get the mem command, default is simple memory info dump.
        String command = null;
        try
        {
            command = st.nextToken();
        }
        catch (Exception ex)
        {
            // Ignore.
        }

        // Perform the specified command.
        if (command == null)
        {
            mem(printHelper);
        }
        else if (command.equals(HELP_CMD))
        {
            help(out, st);
        }
        else if (command.equals(POOLS_CMD))
        {
            pools(printHelper);
        }
        else if (command.equals(DUMP_CMD))
        {
            dump(out, st, err);
        }
        else if (command.equals(LS_CMD))
        {
            ls(printHelper);
        }
        else if (command.equals(RM_CMD))
        {
            rm(out, err, st);
        }
        else
        {
            err.println("Unknown command: " + command);
        }

        printHelper.flush();
    }

    public String getName()
    {
        return "mem";
    }

    public String getShortDescription()
    {
        return "Prints Java VM Memory Consumption or writes a heap dump";
    }

    public String getUsage()
    {
        return "mem help";
    }

    // ---------- internal

    private void mem(final MemoryUsageSupport.PrintHelper printHelper)
    {
        support.printOverallMemory(printHelper);
    }

    private void pools(final MemoryUsageSupport.PrintHelper printHelper)
    {
        support.printMemoryPools(printHelper);
    }

    private void help(final PrintStream out, final StringTokenizer word)
    {
        String command = HELP_CMD;
        if (word.hasMoreTokens())
        {
            command = word.nextToken();
        }
        if (command.equals(DUMP_CMD))
        {
            out.printf("%s %s [ %s ]%n", getName(), DUMP_CMD, OPT_DUMP_ALL);
            out.println("This command requests a heap dump to be created. If the\n"
                + "-a option is not added, only live objects are dumped.");
        }
        else if (command.equals(LS_CMD))
        {
            out.printf("%s %s%n", getName(), LS_CMD);
            out.println("This command lists all heap dumps created with the\n" + DUMP_CMD + " command.");
        }
        else if (command.equals(RM_CMD))
        {
            out.printf("%s %s <dump>%n", getName(), RM_CMD);
            out.println("This command removes the indicated heap dump file.\n" + "Use the " + LS_CMD
                + " to list heap dumps which may be removed.");
        }
        else if (command.equals(POOLS_CMD))
        {
            out.printf("%s %s%n", getName(), POOLS_CMD);
            out.println("This command shows information about all memory pools.");
        }
        else
        {
            out.printf("%s %s [ %s | %s | %s | %s ]%n", getName(), HELP_CMD, DUMP_CMD, LS_CMD, RM_CMD, POOLS_CMD);
            out.printf("%s %s [ %s ]%n", getName(), DUMP_CMD, OPT_DUMP_ALL);
            out.printf("%s %s%n", getName(), LS_CMD);
            out.printf("%s %s <dump>%n", getName(), RM_CMD);
            out.printf("%s %s%n", getName(), POOLS_CMD);
            out.println("Using the " + getName() + " command without any arguments prints a memory use overview.");
        }
    }

    private void dump(final PrintStream out, final StringTokenizer words, final PrintStream err)
    {
        String dumpTarget = null;
        boolean all = false;

        while (words.hasMoreTokens())
        {
            String word = words.nextToken();
            if (OPT_DUMP_ALL.equals(word))
            {
                all = true;
            }
            else
            {
                dumpTarget = word;
            }
        }

        try
        {
            File dumpFile = support.dumpHeap(dumpTarget, !all);
            out.println("Heap dumped to " + dumpFile + " (" + dumpFile.length() + " bytes)");
        }
        catch (NoSuchElementException e)
        {
            err.println("Failed dumping the heap, JVM does not provide known mechanism to create a Heap Dump");
        }
    }

    private void ls(final MemoryUsageSupport.PrintHelper printHelper)
    {
        support.listDumpFiles(printHelper);
    }

    private void rm(final PrintStream out, final PrintStream err, final StringTokenizer words)
    {
        if (words.hasMoreTokens())
        {
            do
            {
                final String name = words.nextToken();
                if (support.rmDumpFile(name))
                {
                    out.println("rm: " + name + " removed");
                    out.flush();
                }
                else
                {
                    err.println("rm: " + name + ": No such dump file");
                    err.flush();
                }
            }
            while (words.hasMoreTokens());
        }
    }

    private static class PrintWriterPrintHelper implements MemoryUsageSupport.PrintHelper
    {

        private static final String INDENTS = "          ";

        private final PrintStream pw;

        private String indent;

        PrintWriterPrintHelper(final PrintStream pw)
        {
            this.pw = pw;
            this.indent = "";
        }

        public void title(String title, int level)
        {
            pw.printf("%s%s%n", getIndent(level - 1), title);
            indent = getIndent(level);
        }

        public void val(String value)
        {
            pw.printf("%s%s%n", indent, value);
        }

        public void keyVal(final String key, final Object value)
        {
            if (value == null)
            {
                val(key);
            }
            else
            {
                pw.printf("%s%s: %s%n", indent, key, value);
            }
        }

        void flush()
        {
            pw.flush();
        }

        private static String getIndent(final int level)
        {
            if (level <= 0)
            {
                return "";
            }

            final int indent = 2 * level;
            if (indent > INDENTS.length())
            {
                return INDENTS;
            }
            return INDENTS.substring(0, indent);
        }
    }
}
