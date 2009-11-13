/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.karaf.shell.ssh;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.felix.karaf.shell.console.Completer;
import org.apache.felix.karaf.shell.console.completer.AggregateCompleter;
import org.apache.felix.karaf.shell.console.jline.Console;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;

/**
 * SSHD {@link org.apache.sshd.server.Command} factory which provides access to Shell.
 *
 * @version $Rev: 731517 $ $Date: 2009-01-05 11:25:19 +0100 (Mon, 05 Jan 2009) $
 */
public class ShellFactoryImpl implements Factory<Command>
{
    private CommandProcessor commandProcessor;
    private List<Completer> completers;

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    public void setCompleters(List<Completer> completers) {
        this.completers = completers;
    }

    public Command create() {
        return new ShellImpl();
    }

    public class ShellImpl implements Command
    {
        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback callback;

        private boolean closed;

        public void setInputStream(final InputStream in) {
            this.in = in;
        }

        public void setOutputStream(final OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(final OutputStream err) {
            this.err = err;
        }

        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        public void start(final Environment env) throws IOException {
            try {
                final Callable<Boolean> printStackTraces = new Callable<Boolean>() {
                    public Boolean call() {
                        return Boolean.valueOf(System.getProperty(Console.PRINT_STACK_TRACES));
                    }
                };
                Console console = new Console(commandProcessor,
                                              in,
                                              new PrintStream(out, true),
                                              new PrintStream(err, true),
                                              new SshTerminal(env),
                                              new AggregateCompleter(completers),
                                              new Runnable() {
                                                  public void run() {
                                                      destroy();
                                                  }
                                              },
                                              printStackTraces);
                CommandSession session = console.getSession();
                session.put("APPLICATION", System.getProperty("karaf.name", "root"));
                for (Map.Entry<String,String> e : env.getEnv().entrySet()) {
                    session.put(e.getKey(), e.getValue());
                }
                new Thread(console).start();
            } catch (Exception e) {
                throw (IOException) new IOException("Unable to start shell").initCause(e);
            }
        }

        public void destroy() {
            if (!closed) {
                closed = true;
                ShellFactoryImpl.close(in, out, err);
                callback.onExit(0);
            }
        }

    }

    private static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static Converter getConverter() {
        return new Converter();
    }

    public static class Converter implements org.osgi.service.blueprint.container.Converter {

        public boolean canConvert(Object sourceObject, ReifiedType targetType) {
            return ShellFactoryImpl.class.isAssignableFrom(sourceObject.getClass())
                    && Factory.class.equals(targetType.getRawClass())
                    && Command.class.equals(targetType.getActualTypeArgument(0).getRawClass());
        }

        public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
            return sourceObject;
        }
    }

}
