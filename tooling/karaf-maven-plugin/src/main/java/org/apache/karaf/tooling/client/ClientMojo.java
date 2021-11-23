/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.client;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.RuntimeSshException;

import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Client MOJO to deployWithSsh command on a running Karaf instance
 */
@Mojo(name = "client", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class ClientMojo extends AbstractMojo {

    @Parameter(defaultValue = "8101")
    private int port;

    @Parameter(defaultValue = "localhost")
    private String host;

    @Parameter(defaultValue = "karaf")
    private String user;

    @Parameter(defaultValue = "karaf")
    private String password;

    @Parameter(defaultValue = "0")
    private int attempts;

    @Parameter(defaultValue = "2")
    private int delay;

    @Parameter
    private List<CommandDescriptor> commands;

    @Parameter
    private List<ScriptDescriptor> scripts;

    @Parameter
    private File keyFile;

    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    private static final String NEW_LINE = System.getProperty("line.separator");

    public void execute() throws MojoExecutionException {
        if (skip || (System.getProperty("client.skip") != null && System.getProperty("client.skip").equalsIgnoreCase("true"))) {
            getLog().info("Client execution is skipped");
            return;
        }
        // ranking the commands and scripts
        Comparator<CommandDescriptor> comparator = Comparator.comparingDouble(CommandDescriptor::getRank);
        SortedSet<CommandDescriptor> sortedCommands = new TreeSet<>(comparator);
        if (scripts != null) {
            for (ScriptDescriptor script : scripts) {
                File file = script.getScript();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    int lineIndex = 0;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }
                        CommandDescriptor descriptor = new CommandDescriptor();
                        descriptor.setCommand(line);
                        double rankSuffix = 0.5;
                        for (int j = 0; j < lineIndex; j++) {
                            rankSuffix = rankSuffix + 0.00001;
                        }
                        descriptor.setRank(script.getRank() + rankSuffix);
                        lineIndex++;
                        sortedCommands.add(descriptor);
                    }
                } catch (Exception e) {
                    throw new MojoExecutionException(e, e.getMessage(), e.toString());
                }
            }
        }
        if (commands != null) {
            sortedCommands.addAll(commands);
        }

        if (sortedCommands == null || sortedCommands.isEmpty()) {
            getLog().warn("No command specified");
            return;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        for (CommandDescriptor command : sortedCommands) {
            String cmd = command.getCommand();
            getLog().info(cmd);
            pw.println(cmd);
        }
        execute(sw.toString());
    }

    protected void execute(String cmd) throws MojoExecutionException {
        SshClient client = null;
        try {
            final Console console = System.console();
            client = SshClient.setUpDefaultClient();
            setupAgent(user, keyFile, client);

            client.setUserInteraction( new UserInteraction() {
                @Override
                public void welcome(ClientSession s, String banner, String lang) {
                    console.printf(banner);
                }
                @Override
                public String[] interactive(ClientSession s, String name, String instruction, String lang, String[] prompt, boolean[] echo)
                {
                    String[] answers = new String[prompt.length];
                    try {
                        for (int i = 0; i < prompt.length; i++) {
                            if (console != null) {
                                if (echo[i]) {
                                    answers[i] = console.readLine(prompt[i] + " ");
                                }
                                else {
                                    answers[i] = new String( console.readPassword(prompt[i] + " "));
                                }
                            }
                        }
                    }
                    catch (IOError e) {
                    }
                    return answers;
                }
                @Override
                public boolean isInteractionAllowed(ClientSession session) {
                    return true;
                }
                @Override
                public void serverVersionInfo(ClientSession session, List<String> lines) {
                }
                @Override
                public String getUpdatedPassword(ClientSession session, String prompt, String lang) {
                    return null;
                }
            });
            client.start();
            if (console != null) {
                console.printf("Logging in as %s\n", user);
            }
            ClientSession session = connect(client);
            if (password != null) {
                session.addPasswordIdentity(password);
            }
            session.auth().verify();

            final ClientChannel channel = session.createChannel("exec", cmd.concat(NEW_LINE));
            channel.setIn(new ByteArrayInputStream(new byte[0]));
            final ByteArrayOutputStream sout = new ByteArrayOutputStream();
            final ByteArrayOutputStream serr = new ByteArrayOutputStream();
            channel.setOut(sout);
            channel.setErr(serr);
            channel.open();
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);

            sout.writeTo(System.out);
            serr.writeTo(System.err);

            // Expects issue KARAF-2623 is fixed
            final boolean isError = (channel.getExitStatus() != null && channel.getExitStatus().intValue() != 0);
            if (isError) {
                final String errorMarker = Ansi.ansi().fg(Color.RED).toString();
                final int fromIndex = sout.toString().indexOf(errorMarker) + errorMarker.length();
                final int toIndex = sout.toString().lastIndexOf(Ansi.ansi().fg(Color.DEFAULT ).toString());
                throw new MojoExecutionException(NEW_LINE + sout.toString().substring(fromIndex, toIndex));
            }
        }
        catch (MojoExecutionException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new MojoExecutionException(t, t.getMessage(), t.toString());
        }
        finally {
            try {
                client.stop();
            }
            catch (Throwable t) {
                throw new MojoExecutionException(t, t.getMessage(), t.toString());
            }
        }
    }

    private void setupAgent(String user, File keyFile, SshClient client) {
        SshAgent agent = startAgent(user, keyFile);
        client.setAgentFactory( new LocalAgentFactory(agent));
        client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
    }

    private SshAgent startAgent(String user, File keyFile) {
        try
        {
            SshAgent agent = new AgentImpl();
            if (keyFile != null) {
                FileKeyPairProvider fileKeyPairProvider = new FileKeyPairProvider(keyFile.getAbsoluteFile().toPath());
                for (KeyPair key : fileKeyPairProvider.loadKeys(null)) {
                    agent.addIdentity(key, user);
                }
            }
            return agent;
        }
        catch (Throwable e) {
            getLog().error("Error starting ssh agent for: " + e.getMessage(), e);
            return null;
        }
    }

    private ClientSession connect(SshClient client) throws IOException, InterruptedException {
        int retries = 0;
        ClientSession session = null;
        do {
            final ConnectFuture future = client.connect(user, host, port);
            future.await();
            try {
                session = future.getSession();
            }
            catch (RuntimeSshException ex) {
                if (retries++ < attempts) {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(delay));
                    getLog().info("retrying (attempt " + retries + ") ...");
                }
                else {
                    throw ex;
                }
            }
        } while (session == null);
        return session;
    }

}

