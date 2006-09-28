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
package org.apache.felix.bundlerepository;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.*;

import org.apache.felix.shell.Command;
import org.osgi.framework.*;
import org.osgi.service.obr.*;

public class ObrCommandImpl implements Command
{
    private static final String HELP_CMD = "help";
    private static final String ADDURL_CMD = "add-url";
    private static final String REMOVEURL_CMD = "remove-url";
    private static final String LISTURL_CMD = "list-url";
    private static final String LIST_CMD = "list";
    private static final String INFO_CMD = "info";
    private static final String DEPLOY_CMD = "deploy";
    private static final String START_CMD = "start";
    private static final String SOURCE_CMD = "source";

    private static final String EXTRACT_SWITCH = "-x";

    private BundleContext m_context = null;
    private RepositoryAdmin m_repoAdmin = null;

    public ObrCommandImpl(BundleContext context, RepositoryAdmin repoAdmin)
    {
        m_context = context;
        m_repoAdmin = repoAdmin;
    }

    public String getName()
    {
        return "obr";
    }

    public String getUsage()
    {
        return "obr help";
    }

    public String getShortDescription()
    {
        return "OSGi bundle repository.";
    }

    public synchronized void execute(String commandLine, PrintStream out, PrintStream err)
    {
        try
        {
            // Parse the commandLine to get the OBR command.
            StringTokenizer st = new StringTokenizer(commandLine);
            // Ignore the invoking command.
            st.nextToken();
            // Try to get the OBR command, default is HELP command.
            String command = HELP_CMD;
            try
            {
                command = st.nextToken();
            }
            catch (Exception ex)
            {
                // Ignore.
            }
            
            // Perform the specified command.
            if ((command == null) || (command.equals(HELP_CMD)))
            {
                help(out, st);
            }
            else
            {
                if (command.equals(ADDURL_CMD) ||
                    command.equals(REMOVEURL_CMD) ||
                    command.equals(LISTURL_CMD))
                {
                    urls(commandLine, command, out, err);
                }
                else if (command.equals(LIST_CMD))
                {
                    list(commandLine, command, out, err);
                }
                else if (command.equals(INFO_CMD))
                {
                    info(commandLine, command, out, err);
                }
                else if (command.equals(DEPLOY_CMD) || command.equals(START_CMD))
                {
                    deploy(commandLine, command, out, err);
                }
                else if (command.equals(SOURCE_CMD))
                {
                    source(commandLine, command, out, err);
                }
                else
                {
                    err.println("Unknown command: " + command);
                }
            }
        }
        catch (InvalidSyntaxException ex)
        {
            err.println("Syntax error: " + ex.getMessage());
        }
        catch (IOException ex)
        {
            err.println("Error: " + ex);
        }
    }

    private void urls(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException
    {
        // Parse the commandLine.
        StringTokenizer st = new StringTokenizer(commandLine);
        // Ignore the "obr" command.
        st.nextToken();
        // Ignore the "url" command.
        st.nextToken();

        int count = st.countTokens();
        if (count > 0)
        {
            while (st.hasMoreTokens())
            {
                if (command.equals(ADDURL_CMD))
                {
                    try
                    {
                        m_repoAdmin.addRepository(new URL(st.nextToken()));
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace(err);
                    }
                }
                else
                {
                    m_repoAdmin.removeRepository(new URL(st.nextToken()));
                }
            }
        }
        else
        {
            Repository[] repos = m_repoAdmin.listRepositories();
            if ((repos != null) && (repos.length > 0))
            {
                for (int i = 0; i < repos.length; i++)
                {
                    out.println(repos[i].getURL());
                }
            }
            else
            {
                out.println("No repository URLs are set.");
            }
        }
    }

    private void list(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');

        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();

        String substr = null;
    
        for (type = tokenizer.nextToken();
            type != StreamTokenizer.TT_EOF;
            type = tokenizer.nextToken())
        {
            // Add a space in between tokens.
            if (substr == null)
            {
                substr = "";
            }
            else
            {
                substr += " ";
            }
                        
            if ((type == StreamTokenizer.TT_WORD) ||
                (type == '\'') || (type == '"'))
            {
                substr += tokenizer.sval;
            }
        }

        StringBuffer sb = new StringBuffer();
        if ((substr == null) || (substr.length() == 0))
        {
            sb.append("(|(presentationname=*)(symbolicname=*))");
        }
        else
        {
            sb.append("(|(presentationname=*");
            sb.append(substr);
            sb.append("*)(symbolicname=*");
            sb.append(substr);
            sb.append("*))");
        }
        Resource[] resources = m_repoAdmin.discoverResources(sb.toString());
        for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
        {
            String name = resources[resIdx].getPresentationName();
            Version version = resources[resIdx].getVersion();
            if (version != null)
            {
                out.println(name + " (" + version + ")");
            }
            else
            {
                out.println(name);
            }
        }
    
        if (resources == null)
        {
            out.println("No matching bundles.");
        }
    }

    private void info(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        ParsedCommand pc = parseInfo(commandLine);
        for (int cmdIdx = 0; (pc != null) && (cmdIdx < pc.getTargetCount()); cmdIdx++)                
        {
            // Find the target's bundle resource.
            Resource[] resources = searchRepository(pc.getTargetId(cmdIdx), pc.getTargetVersion(cmdIdx));
            if (resources == null)
            {
                err.println("Unknown bundle and/or version: "
                    + pc.getTargetId(cmdIdx));
            }
            else
            {
                for (int resIdx = 0; resIdx < resources.length; resIdx++)
                {
                    if (resIdx > 0)
                    {
                        out.println("");
                    }
                    printResource(out, resources[resIdx]);
                }
            }
        }
    }

    private void deploy(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        ParsedCommand pc = parseInstallStart(commandLine);
        _deploy(pc, command, out, err);
    }

    private void _deploy(
        ParsedCommand pc, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        Resolver resolver = m_repoAdmin.resolver();
        for (int i = 0; (pc != null) && (i < pc.getTargetCount()); i++)                
        {
            // Find the target's bundle resource.
            Resource resource = selectNewestVersion(
                searchRepository(pc.getTargetId(i), pc.getTargetVersion(i)));
            if (resource != null)
            {
                resolver.add(resource);
            }
            else
            {
                err.println("Unknown bundle - " + pc.getTargetId(i));
            }
        }

        if ((resolver.getAddedResources() != null) &&
            (resolver.getAddedResources().length > 0))
        {
            if (resolver.resolve())
            {
                out.println("Target resource(s):");
                printUnderline(out, 19);
                Resource[] resources = resolver.getAddedResources();
                for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
                {
                    out.println("   " + resources[resIdx].getPresentationName()
                        + " (" + resources[resIdx].getVersion() + ")");
                }
                resources = resolver.getRequiredResources();
                if ((resources != null) && (resources.length > 0))
                {
                    out.println("\nRequired resource(s):");
                    printUnderline(out, 21);
                    for (int resIdx = 0; resIdx < resources.length; resIdx++)
                    {
                        out.println("   " + resources[resIdx].getPresentationName()
                            + " (" + resources[resIdx].getVersion() + ")");
                    }
                }
                resources = resolver.getOptionalResources();
                if ((resources != null) && (resources.length > 0))
                {
                    out.println("\nOptional resource(s):");
                    printUnderline(out, 21);
                    for (int resIdx = 0; resIdx < resources.length; resIdx++)
                    {
                        out.println("   " + resources[resIdx].getPresentationName()
                            + " (" + resources[resIdx].getVersion() + ")");
                    }
                }
    
                try
                {
                    out.print("\nDeploying...");
                    resolver.deploy(command.equals(START_CMD));
                    out.println("done.");
                }
                catch (IllegalStateException ex)
                {
                    err.println(ex);
                }
            }
            else
            {
                Requirement[] reqs = resolver.getUnsatisfiedRequirements();
                if ((reqs != null) && (reqs.length > 0))
                {
                    out.println("Unsatisfied requirement(s):");
                    printUnderline(out, 27);
                    for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
                    {
                        out.println("   " + reqs[reqIdx].getFilter());
                        Resource[] resources = resolver.getResources(reqs[reqIdx]);
                        for (int resIdx = 0; resIdx < resources.length; resIdx++)
                        {
                            out.println("      " + resources[resIdx].getPresentationName());
                        }
                    }
                }
                else
                {
                    out.println("Could not resolve targets.");
                }
            }
        }
    }

    private void source(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        // Parse the command line to get all local targets to update.
        ParsedCommand pc = parseSource(commandLine);
        for (int i = 0; i < pc.getTargetCount(); i++)
        {
            Resource resource = selectNewestVersion(
                searchRepository(pc.getTargetId(i), pc.getTargetVersion(i)));
            if (resource == null)
            {
                err.println("Unknown bundle and/or version: "
                    + pc.getTargetId(i));
            }
            else
            {
                URL srcURL = (URL) resource.getProperties().get(Resource.SOURCE_URL);
                if (srcURL != null)
                {
                    FileUtil.downloadSource(
                        out, err, srcURL, pc.getDirectory(), pc.isExtract());
                }
                else
                {
                    err.println("Missing source URL: " + pc.getTargetId(i));
                }
            }
        }
    }

    private Resource[] searchRepository(String targetId, String targetVersion)
    {
        // Try to see if the targetId is a bundle ID.
        try
        {
            Bundle bundle = m_context.getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        }
        catch (NumberFormatException ex)
        {
            // It was not a number, so ignore.
        }

        // The targetId may be a bundle name or a bundle symbolic name,
        // so create the appropriate LDAP query.
        StringBuffer sb = new StringBuffer("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null)
        {
            sb.insert(0, "(&");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return m_repoAdmin.discoverResources(sb.toString());
    }

    public Resource selectNewestVersion(Resource[] resources)
    {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            if (i == 0)
            {
                idx = 0;
                v = resources[i].getVersion();
            }
            else
            {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0)
                {
                    idx = i;
                    v = vtmp;
                }
            }
        }

        return (idx < 0) ? null : resources[idx];
    }

    private void printResource(PrintStream out, Resource resource)
    {
        printUnderline(out, resource.getPresentationName().length());
        out.println(resource.getPresentationName());
        printUnderline(out, resource.getPresentationName().length());

        Map map = resource.getProperties();
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getValue().getClass().isArray())
            {
                out.println(entry.getKey() + ":");
                for (int j = 0; j < Array.getLength(entry.getValue()); j++)
                {
                    out.println("   " + Array.get(entry.getValue(), j));
                }
            }
            else
            {
                out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
 
        Requirement[] reqs = resource.getRequirements();
        if ((reqs != null) && (reqs.length > 0))
        {
            out.println("Requires:");
            for (int i = 0; i < reqs.length; i++)
            {
                out.println("   " + reqs[i].getFilter());
            }
        }
        
        Capability[] caps = resource.getCapabilities();
        if ((caps != null) && (caps.length > 0))
        {
            out.println("Capabilities:");
            for (int i = 0; i < caps.length; i++)
            {
                out.println("   " + caps[i].getProperties());
            }
        }
    }

    private static void printUnderline(PrintStream out, int length)
    {
        for (int i = 0; i < length; i++)
        {
            out.print('-');
        }
        out.println("");
    }

    private ParsedCommand parseInfo(String commandLine)
        throws IOException, InvalidSyntaxException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');
    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();
    
        int EOF = 1;
        int SWITCH = 2;
        int TARGET = 4;
        int VERSION = 8;
        int VERSION_VALUE = 16;

        // Construct an install record.
        ParsedCommand pc = new ParsedCommand();
        String currentTargetName = null;

        // The state machine starts by expecting either a
        // SWITCH or a TARGET.
        int expecting = (TARGET);
        while (true)
        {
            // Get the next token type.
            type = tokenizer.nextToken();
            switch (type)
            {
                // EOF received.
                case StreamTokenizer.TT_EOF:
                    // Error if we weren't expecting EOF.
                    if ((expecting & EOF) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Expecting more arguments.", null);
                    }
                    // Add current target if there is one.
                    if (currentTargetName != null)
                    {
                        pc.addTarget(currentTargetName, null);
                    }
                    // Return cleanly.
                    return pc;

                // WORD or quoted WORD received.
                case StreamTokenizer.TT_WORD:
                case '\'':
                case '\"':
                    // If we are expecting a target, the record it.
                    if ((expecting & TARGET) > 0)
                    {
                        // Add current target if there is one.
                        if (currentTargetName != null)
                        {
                            pc.addTarget(currentTargetName, null);
                        }
                        // Set the new target as the current target.
                        currentTargetName = tokenizer.sval;
                        expecting = (EOF | TARGET | VERSION);
                    }
                    else if ((expecting & VERSION_VALUE) > 0)
                    {
                        pc.addTarget(currentTargetName, tokenizer.sval);
                        currentTargetName = null;
                        expecting = (EOF | TARGET);
                    }
                    else
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting '" + tokenizer.sval + "'.", null);
                    }
                    break;

                // Version separator character received.
                case ';':
                    // Error if we weren't expecting the version separator.
                    if ((expecting & VERSION) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting version.", null);
                    }
                    // Otherwise, we will only expect a version value next.
                    expecting = (VERSION_VALUE);
                    break;
            }
        }
    }

    private ParsedCommand parseInstallStart(String commandLine)
        throws IOException, InvalidSyntaxException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');
    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();
    
        int EOF = 1;
        int SWITCH = 2;
        int TARGET = 4;
        int VERSION = 8;
        int VERSION_VALUE = 16;

        // Construct an install record.
        ParsedCommand pc = new ParsedCommand();
        String currentTargetName = null;

        // The state machine starts by expecting either a
        // SWITCH or a TARGET.
        int expecting = (SWITCH | TARGET);
        while (true)
        {
            // Get the next token type.
            type = tokenizer.nextToken();
            switch (type)
            {
                // EOF received.
                case StreamTokenizer.TT_EOF:
                    // Error if we weren't expecting EOF.
                    if ((expecting & EOF) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Expecting more arguments.", null);
                    }
                    // Add current target if there is one.
                    if (currentTargetName != null)
                    {
                        pc.addTarget(currentTargetName, null);
                    }
                    // Return cleanly.
                    return pc;

                // WORD or quoted WORD received.
                case StreamTokenizer.TT_WORD:
                case '\'':
                case '\"':
                    // If we are expecting a target, the record it.
                    if ((expecting & TARGET) > 0)
                    {
                        // Add current target if there is one.
                        if (currentTargetName != null)
                        {
                            pc.addTarget(currentTargetName, null);
                        }
                        // Set the new target as the current target.
                        currentTargetName = tokenizer.sval;
                        expecting = (EOF | TARGET | VERSION);
                    }
                    else if ((expecting & VERSION_VALUE) > 0)
                    {
                        pc.addTarget(currentTargetName, tokenizer.sval);
                        currentTargetName = null;
                        expecting = (EOF | TARGET);
                    }
                    else
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting '" + tokenizer.sval + "'.", null);
                    }
                    break;

                // Version separator character received.
                case ';':
                    // Error if we weren't expecting the version separator.
                    if ((expecting & VERSION) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting version.", null);
                    }
                    // Otherwise, we will only expect a version value next.
                    expecting = (VERSION_VALUE);
                    break;
            }
        }
    }

    private ParsedCommand parseSource(String commandLine)
        throws IOException, InvalidSyntaxException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('/', '/');
        tokenizer.wordChars('\\', '\\');
        tokenizer.wordChars(':', ':');
    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();
    
        int EOF = 1;
        int SWITCH = 2;
        int DIRECTORY = 4;
        int TARGET = 8;
        int VERSION = 16;
        int VERSION_VALUE = 32;

        // Construct an install record.
        ParsedCommand pc = new ParsedCommand();
        String currentTargetName = null;

        // The state machine starts by expecting either a
        // SWITCH or a DIRECTORY.
        int expecting = (SWITCH | DIRECTORY);
        while (true)
        {
            // Get the next token type.
            type = tokenizer.nextToken();
            switch (type)
            {
                // EOF received.
                case StreamTokenizer.TT_EOF:
                    // Error if we weren't expecting EOF.
                    if ((expecting & EOF) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Expecting more arguments.", null);
                    }
                    // Add current target if there is one.
                    if (currentTargetName != null)
                    {
                        pc.addTarget(currentTargetName, null);
                    }
                    // Return cleanly.
                    return pc;

                // WORD or quoted WORD received.
                case StreamTokenizer.TT_WORD:
                case '\'':
                case '\"':
                    // If we are expecting a command SWITCH and the token
                    // equals a command SWITCH, then record it.
                    if (((expecting & SWITCH) > 0) && tokenizer.sval.equals(EXTRACT_SWITCH))
                    {
                        pc.setExtract(true);
                        expecting = (DIRECTORY);
                    }
                    // If we are expecting a directory, the record it.
                    else if ((expecting & DIRECTORY) > 0)
                    {
                        // Set the directory for the command.
                        pc.setDirectory(tokenizer.sval);
                        expecting = (TARGET);
                    }
                    // If we are expecting a target, the record it.
                    else if ((expecting & TARGET) > 0)
                    {
                        // Add current target if there is one.
                        if (currentTargetName != null)
                        {
                            pc.addTarget(currentTargetName, null);
                        }
                        // Set the new target as the current target.
                        currentTargetName = tokenizer.sval;
                        expecting = (EOF | TARGET | VERSION);
                    }
                    else if ((expecting & VERSION_VALUE) > 0)
                    {
                        pc.addTarget(currentTargetName, tokenizer.sval);
                        currentTargetName = null;
                        expecting = (EOF | TARGET);
                    }
                    else
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting '" + tokenizer.sval + "'.", null);
                    }
                    break;

                // Version separator character received.
                case ';':
                    // Error if we weren't expecting the version separator.
                    if ((expecting & VERSION) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting version.", null);
                    }
                    // Otherwise, we will only expect a version value next.
                    expecting = (VERSION_VALUE);
                    break;
            }
        }
    }

    private void help(PrintStream out, StringTokenizer st)
    {
        String command = HELP_CMD;
        if (st.hasMoreTokens())
        {
            command = st.nextToken();
        }
        if (command.equals(ADDURL_CMD))
        {
            out.println("");
            out.println("obr " + ADDURL_CMD + " [<repository-url> ...]");
            out.println("");
            out.println(
                "This command adds the space-delimited list of repository URLs to\n" +
                "the repository service.");
            out.println("");
        }
        else if (command.equals(REMOVEURL_CMD))
        {
            out.println("");
            out.println("obr " + REMOVEURL_CMD + " [<repository-url> ...]");
            out.println("");
            out.println(
                "This command removes the space-delimited list of repository URLs\n" +
                "from the repository service.");
            out.println("");
        }
        else if (command.equals(LISTURL_CMD))
        {
            out.println("");
            out.println("obr " + LISTURL_CMD);
            out.println("");
            out.println(
                "This command displays the repository URLs currently associated\n" +
                "with the repository service.");
            out.println("");
        }
        else if (command.equals(LIST_CMD))
        {
            out.println("");
            out.println("obr " + LIST_CMD + " [<string> ...]");
            out.println("");
            out.println(
                "This command lists bundles available in the bundle repository.\n" +
                "If no arguments are specified, then all available bundles are\n" +
                "listed, otherwise any arguments are concatenated with spaces\n" +
                "and used as a substring filter on the bundle names.");
            out.println("");
        }
        else if (command.equals(INFO_CMD))
        {
            out.println("");
            out.println("obr " + INFO_CMD
                + " <bundle-name>|<bundle-symbolic-name>|<bundle-id>[;<version>] ...");
            out.println("");
            out.println(
                "This command displays the meta-data for the specified bundles.\n" +
                "If a bundle's name contains spaces, then it must be surrounded\n" +
                "by quotes. It is also possible to specify a precise version\n" +
                "if more than one version exists, such as:\n" +
                "\n" +
                "    obr info \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example retrieves the meta-data for version \"1.0.0\"\n" +
                "of the bundle named \"Bundle Repository\".");
            out.println("");
        }
        else if (command.equals(DEPLOY_CMD))
        {
            out.println("");
            out.println("obr " + DEPLOY_CMD
                + " <bundle-name>|<bundle-symbolic-name>|<bundle-id>[;<version>] ... ");
            out.println("");
            out.println(
                "This command tries to install or update the specified bundles\n" +
                "and all of their dependencies. You can specify either the bundle\n" +
                "name or the bundle identifier. If a bundle's name contains spaces,\n" +
                "then it must be surrounded by quotes. It is also possible to\n" +
                "specify a precise version if more than one version exists, such as:\n" +
                "\n" +
                "    obr deploy \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "For the above example, if version \"1.0.0\" of \"Bundle Repository\" is\n" +
                "already installed locally, then the command will attempt to update it\n" +
                "and all of its dependencies; otherwise, the command will install it\n" +
                "and all of its dependencies.");
            out.println("");
        }
        else if (command.equals(START_CMD))
        {
            out.println("");
            out.println("obr " + START_CMD
                + " <bundle-name>|<bundle-symbolic-name>|<bundle-id>[;<version>] ...");
            out.println("");
            out.println(
                "This command installs and starts the specified bundles and all\n" +
                "of their dependencies. If a bundle's name contains spaces, then\n" +
                "it must be surrounded by quotes. If a specified bundle is already\n" +                "installed, then this command has no effect. It is also possible\n" +                "to specify a precise version if more than one version exists,\n" +                "such as:\n" +
                "\n" +
                "    obr start \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example installs and starts version \"1.0.0\" of the\n" +
                "bundle named \"Bundle Repository\" and its dependencies.");
            out.println("");
        }
        else if (command.equals(SOURCE_CMD))
        {
            out.println("");
            out.println("obr " + SOURCE_CMD
                + " [" + EXTRACT_SWITCH
                + "] <local-dir> <bundle-name>[;<version>] ...");
            out.println("");
            out.println(
                "This command retrieves the source archives of the specified\n" +
                "bundles and saves them to the specified local directory; use\n" +
                "the \"" + EXTRACT_SWITCH + "\" switch to automatically extract the source archives.\n" +
                "If a bundle name contains spaces, then it must be surrounded\n" +
                "by quotes. It is also possible to specify a precise version if\n" +                "more than one version exists, such as:\n" +
                "\n" +
                "    obr source /home/rickhall/tmp \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example retrieves the source archive of version \"1.0.0\"\n" +
                "of the bundle named \"Bundle Repository\" and saves it to the\n" +
                "specified local directory.");
            out.println("");
        }
        else
        {
            out.println("obr " + HELP_CMD
                + " [" + ADDURL_CMD
                + " | " + REMOVEURL_CMD
                + " | " + LISTURL_CMD
                + " | " + LIST_CMD
                + " | " + INFO_CMD
                + " | " + DEPLOY_CMD + " | " + START_CMD
                + " | " + SOURCE_CMD + "]");
            out.println("obr " + ADDURL_CMD + " [<repository-file-url> ...]");
            out.println("obr " + REMOVEURL_CMD + " [<repository-file-url> ...]");
            out.println("obr " + LISTURL_CMD);
            out.println("obr " + LIST_CMD + " [<string> ...]");
            out.println("obr " + INFO_CMD
                + " <bundle-name>|<bundle-symbolic-name>|<bundle-id>[;<version>] ...");
            out.println("obr " + DEPLOY_CMD
                + " <bundle-name>|<bundle-symbolic-name>|<bundle-id>[;<version>] ...");
            out.println("obr " + START_CMD
                + " <bundle-name>|<bundle-symbolic-name>|<bundle-id>[;<version>] ...");
            out.println("obr " + SOURCE_CMD
                + " [" + EXTRACT_SWITCH
                + "] <local-dir> <bundle-name>[;<version>] ...");
        }
    }

    private static class ParsedCommand
    {
        private static final int NAME_IDX = 0;
        private static final int VERSION_IDX = 1;

        private boolean m_isResolve = true;
        private boolean m_isCheck = false;
        private boolean m_isExtract = false;
        private String m_dir = null;
        private String[][] m_targets = new String[0][];
        
        public boolean isResolve()
        {
            return m_isResolve;
        }
        
        public void setResolve(boolean b)
        {
            m_isResolve = b;
        }

        public boolean isCheck()
        {
            return m_isCheck;
        }
        
        public void setCheck(boolean b)
        {
            m_isCheck = b;
        }

        public boolean isExtract()
        {
            return m_isExtract;
        }
        
        public void setExtract(boolean b)
        {
            m_isExtract = b;
        }

        public String getDirectory()
        {
            return m_dir;
        }
        
        public void setDirectory(String s)
        {
            m_dir = s;
        }

        public int getTargetCount()
        {
            return m_targets.length;
        }
        
        public String getTargetId(int i)
        {
            if ((i < 0) || (i >= getTargetCount()))
            {
                return null;
            }
            return m_targets[i][NAME_IDX];
        }
        
        public String getTargetVersion(int i)
        {
            if ((i < 0) || (i >= getTargetCount()))
            {
                return null;
            }
            return m_targets[i][VERSION_IDX];
        }

        public void addTarget(String name, String version)
        {
            String[][] newTargets = new String[m_targets.length + 1][];
            System.arraycopy(m_targets, 0, newTargets, 0, m_targets.length);
            newTargets[m_targets.length] = new String[] { name, version };
            m_targets = newTargets;
        }
    }
}
