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

/**
 * Based on http://antony.lesuisse.org/software/ajaxterm/
 *  Public Domain License
 */

package org.apache.karaf.webconsole.gogo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.jline.Console;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>GogoPlugin</code>
 */
public class GogoPlugin extends AbstractWebConsolePlugin {

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(GogoPlugin.class);

    public static final String NAME = "gogo";

    public static final String LABEL = "Gogo";

    public static final int TERM_WIDTH = 120;
    public static final int TERM_HEIGHT = 39;



    private BundleContext bundleContext;

    private CommandProcessor commandProcessor;

    public void setBundleContext(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    public void setCommandProcessor(CommandProcessor commandProcessor)
    {
        this.commandProcessor = commandProcessor;
    }

    /*
    * Blueprint lifecycle callback methods
    */

    public void start()
    {
        super.activate( bundleContext );
        this.logger.info( LABEL + " plugin activated" );
    }

    public void stop()
    {
        this.logger.info( LABEL + " plugin deactivated" );
        super.deactivate();
    }

    //
    // AbstractWebConsolePlugin interface
    //
    public String getLabel()
    {
        return NAME;
    }


    public String getTitle()
    {
        return LABEL;
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        PrintWriter pw = response.getWriter();

        String appRoot = request.getContextPath() + request.getServletPath();
        pw.println( "<link href=\"" + appRoot + "/gogo/res/ui/gogo.css\" rel=\"stylesheet\" type=\"text/css\" />" );
        pw.println( "<script src=\"" + appRoot + "/gogo/res/ui/gogo.js\" type=\"text/javascript\"></script>" );
        pw.println( "<div id='console'><div id='term'></div></div>" );
        pw.println( "<script type=\"text/javascript\"><!--" );
        pw.println( "window.onload = function() { gogo.Terminal(document.getElementById(\"term\"), " + TERM_WIDTH + ", " + TERM_HEIGHT + "); }" );
        pw.println( "--></script>" );
    }

    protected URL getResource( String path )
    {
        path = path.substring( NAME.length() + 1 );
        URL url = this.getClass().getClassLoader().getResource( path );
        if (url != null) {
            InputStream ins = null;
            try {
                ins = url.openStream();
                if (ins == null) {
                    this.logger.error("failed to open " + url);
                    url = null;
                }
            } catch (IOException e) {
                this.logger.error(e.getMessage(), e);
                url = null;
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                        this.logger.error(e.getMessage(), e);
                    }
                }
            }
        }
        return url;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String encoding = request.getHeader("Accept-Encoding");
        boolean supportsGzip = (encoding != null && encoding.toLowerCase().indexOf("gzip") > -1);
        SessionTerminal st = (SessionTerminal) request.getSession(true).getAttribute("terminal");
        if (st == null || st.isClosed()) {
            st = new SessionTerminal();
            request.getSession().setAttribute("terminal", st);
        }
        String str = request.getParameter("k");
        String f = request.getParameter("f");
        String dump = st.handle(str, f != null && f.length() > 0);
        if (dump != null) {
            if (supportsGzip) {
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Content-Type", "text/html");
                try {
                    GZIPOutputStream gzos =  new GZIPOutputStream(response.getOutputStream());
                    gzos.write(dump.getBytes());
                    gzos.close();
                } catch (IOException ie) {
                    // handle the error here
                    ie.printStackTrace();
                }
            } else {
                response.getOutputStream().write(dump.getBytes());
            }
        }
    }


    public class SessionTerminal implements Runnable {

        private Terminal terminal;
        private Console console;
        private PipedOutputStream in;
        private PipedInputStream out;
        private boolean closed;

        public SessionTerminal() throws IOException {
            try {
                this.terminal = new Terminal(TERM_WIDTH, TERM_HEIGHT);
                terminal.write("\u001b\u005B20\u0068"); // set newline mode on

                in = new PipedOutputStream();
                out = new PipedInputStream();
                PrintStream pipedOut = new PrintStream(new PipedOutputStream(out), true);

                console = new Console(commandProcessor,
                                      new PipedInputStream(in),
                                      pipedOut,
                                      pipedOut,
                                      new WebTerminal(TERM_WIDTH, TERM_HEIGHT),
                                      null);
                CommandSession session = console.getSession();
                session.put("APPLICATION", System.getProperty("karaf.name", "root"));
                session.put("USER", "karaf");
                session.put("COLUMNS", Integer.toString(TERM_WIDTH));
                session.put("LINES", Integer.toString(TERM_HEIGHT));
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw (IOException) new IOException().initCause(e);
            }
            new Thread(console).start();
            new Thread(this).start();
        }

        public boolean isClosed() {
            return closed;
        }

        public String handle(String str, boolean forceDump) throws IOException {
            try {
                if (str != null && str.length() > 0) {
                    String d = terminal.pipe(str);
                    for (byte b : d.getBytes()) {
                        in.write(b);
                    }
                    in.flush();
                }
            } catch (IOException e) {
                closed = true;
                throw e;
            }
            try {
                return terminal.dump(10, forceDump);
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.toString());
            }
        }

        public void run() {
            try {
                for (;;) {
                    byte[] buf = new byte[8192];
                    int l = out.read(buf);
                    InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(buf, 0, l));
                    StringBuilder sb = new StringBuilder();
                    for (;;) {
                        int c = r.read();
                        if (c == -1) {
                            break;
                        }
                        sb.append((char) c);
                    }
                    if (sb.length() > 0) {
                        terminal.write(sb.toString());
                    }
                    String s = terminal.read();
                    if (s != null && s.length() > 0) {
                        for (byte b : s.getBytes()) {
                            in.write(b);
                        }
                    }
                }
            } catch (IOException e) {
                closed = true;
                e.printStackTrace();
            }
        }

    }
}
