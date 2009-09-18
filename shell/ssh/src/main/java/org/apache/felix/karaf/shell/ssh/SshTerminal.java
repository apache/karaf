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

import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import jline.Terminal;
import org.apache.sshd.server.ShellFactory;

public class SshTerminal extends Terminal implements ShellFactory.SignalListener {

    public static final short ARROW_START = 27;
    public static final short ARROW_PREFIX = 91;
    public static final short ARROW_LEFT = 68;
    public static final short ARROW_RIGHT = 67;
    public static final short ARROW_UP = 65;
    public static final short ARROW_DOWN = 66;
    public static final short O_PREFIX = 79;
    public static final short HOME_CODE = 72;
    public static final short END_CODE = 70;

    public static final short DEL_THIRD = 51;
    public static final short DEL_SECOND = 126;

    private ShellFactory.Environment environment;
    private boolean backspaceDeleteSwitched = false;
    private String encoding = System.getProperty("input.encoding", "UTF-8");
    private ReplayPrefixOneCharInputStream replayStream = new ReplayPrefixOneCharInputStream(encoding);
    private InputStreamReader replayReader;

    public SshTerminal(ShellFactory.Environment environment) {
        this.environment = environment;
        this.environment.addSignalListener(this);
        try {
            replayReader = new InputStreamReader(replayStream, encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initializeTerminal() throws Exception {
    }

    public void restoreTerminal() throws Exception {
    }

    public int getTerminalWidth() {
        return Integer.valueOf(this.environment.getEnv().get("COLUMNS"));
    }

    public int getTerminalHeight() {
        return Integer.valueOf(this.environment.getEnv().get("LINES"));
    }

    public boolean isSupported() {
        return true;
    }

    public boolean getEcho() {
        return false;
    }

    public boolean isEchoEnabled() {
        return false;
    }

    public void enableEcho() {
    }

    public void disableEcho() {
    }

    public void signal(int signal) {

    }

    public int readVirtualKey(InputStream in) throws IOException {
        int c = readCharacter(in);

        if (backspaceDeleteSwitched)
            if (c == DELETE)
                c = '\b';
            else if (c == '\b')
                c = DELETE;

        // in Unix terminals, arrow keys are represented by
        // a sequence of 3 characters. E.g., the up arrow
        // key yields 27, 91, 68
        if (c == ARROW_START) {
		//also the escape key is 27
		//thats why we read until we
		//have something different than 27
		//this is a bugfix, because otherwise
		//pressing escape and than an arrow key
		//was an undefined state
		while (c == ARROW_START)
            		c = readCharacter(in);
            if (c == ARROW_PREFIX || c == O_PREFIX) {
                c = readCharacter(in);
                if (c == ARROW_UP) {
                    return CTRL_P;
                } else if (c == ARROW_DOWN) {
                    return CTRL_N;
                } else if (c == ARROW_LEFT) {
                    return CTRL_B;
                } else if (c == ARROW_RIGHT) {
                    return CTRL_F;
                } else if (c == HOME_CODE) {
                    return CTRL_A;
                } else if (c == END_CODE) {
                    return CTRL_E;
                } else if (c == DEL_THIRD) {
                    c = readCharacter(in); // read 4th
                    return DELETE;
                }
            }
        }
        // handle unicode characters, thanks for a patch from amyi@inf.ed.ac.uk
        if (c > 128) {
          // handle unicode characters longer than 2 bytes,
          // thanks to Marc.Herbert@continuent.com
            replayStream.setInput(c, in);
//            replayReader = new InputStreamReader(replayStream, encoding);
            c = replayReader.read();

        }

        return c;
    }

    /**
     * This is awkward and inefficient, but probably the minimal way to add
     * UTF-8 support to JLine
     *
     * @author <a href="mailto:Marc.Herbert@continuent.com">Marc Herbert</a>
     */
    static class ReplayPrefixOneCharInputStream extends InputStream {
        byte firstByte;
        int byteLength;
        InputStream wrappedStream;
        int byteRead;

        final String encoding;

        public ReplayPrefixOneCharInputStream(String encoding) {
            this.encoding = encoding;
        }

        public void setInput(int recorded, InputStream wrapped) throws IOException {
            this.byteRead = 0;
            this.firstByte = (byte) recorded;
            this.wrappedStream = wrapped;

            byteLength = 1;
            if (encoding.equalsIgnoreCase("UTF-8"))
                setInputUTF8(recorded, wrapped);
            else if (encoding.equalsIgnoreCase("UTF-16"))
                byteLength = 2;
            else if (encoding.equalsIgnoreCase("UTF-32"))
                byteLength = 4;
        }


        public void setInputUTF8(int recorded, InputStream wrapped) throws IOException {
            // 110yyyyy 10zzzzzz
            if ((firstByte & (byte) 0xE0) == (byte) 0xC0)
                this.byteLength = 2;
            // 1110xxxx 10yyyyyy 10zzzzzz
            else if ((firstByte & (byte) 0xF0) == (byte) 0xE0)
                this.byteLength = 3;
            // 11110www 10xxxxxx 10yyyyyy 10zzzzzz
            else if ((firstByte & (byte) 0xF8) == (byte) 0xF0)
                this.byteLength = 4;
            else
                throw new IOException("invalid UTF-8 first byte: " + firstByte);
        }

        public int read() throws IOException {
            if (available() == 0)
                return -1;

            byteRead++;

            if (byteRead == 1)
                return firstByte;

            return wrappedStream.read();
        }

        /**
        * InputStreamReader is greedy and will try to read bytes in advance. We
        * do NOT want this to happen since we use a temporary/"losing bytes"
        * InputStreamReader above, that's why we hide the real
        * wrappedStream.available() here.
        */
        public int available() {
            return byteLength - byteRead;
        }
    }

}
