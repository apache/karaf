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
package org.apache.karaf.shell.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import jline.TerminalSupport;
import jline.console.Key;
import jline.internal.ReplayPrefixOneCharInputStream;
import org.apache.sshd.common.PtyMode;
import org.apache.sshd.server.Environment;

import static jline.console.Key.*;
import static org.apache.karaf.shell.ssh.SshTerminal.UnixKey.*;

public class SshTerminal extends TerminalSupport {

    private Environment environment;
    private String encoding = System.getProperty("input.encoding", "UTF-8");
    private ReplayPrefixOneCharInputStream replayStream = new ReplayPrefixOneCharInputStream(encoding);
    private InputStreamReader replayReader;
    private boolean deleteSendsBackspace = false;
    private boolean backspaceSendsDelete = false;


    public SshTerminal(Environment environment) {
        super(true);
        setAnsiSupported(true);
        this.environment = environment;
        try {
            replayReader = new InputStreamReader(replayStream, encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Integer verase = this.environment.getPtyModes().get(PtyMode.VERASE);
        deleteSendsBackspace = verase != null && verase == Key.DELETE.code;
    }

    public void init() throws Exception {
    }

    public void restore() throws Exception {
    }

    @Override
    public int getWidth() {
        return Integer.valueOf(this.environment.getEnv().get(Environment.ENV_COLUMNS));
    }

    @Override
    public int getHeight() {
        return Integer.valueOf(this.environment.getEnv().get(Environment.ENV_LINES));
    }

    @Override
    public int readVirtualKey(final InputStream in) throws IOException {
        int c = readCharacter(in);

        if (Key.valueOf(c) == DELETE && deleteSendsBackspace) {
            c = BACKSPACE.code;
        } else if (Key.valueOf(c) == BACKSPACE && backspaceSendsDelete) {
            c = DELETE.code;
        }

        UnixKey key = UnixKey.valueOf(c);

        // in Unix terminals, arrow keys are represented by a sequence of 3 characters. E.g., the up arrow key yields 27, 91, 68
        if (key == ARROW_START) {
            // also the escape key is 27 thats why we read until we have something different than 27
            // this is a bugfix, because otherwise pressing escape and than an arrow key was an undefined state
            while (key == ARROW_START) {
                c = readCharacter(in);
                key = UnixKey.valueOf(c);
            }

            if (key == ARROW_PREFIX || key == O_PREFIX) {
                c = readCharacter(in);
                key = UnixKey.valueOf(c);

                if (key == ARROW_UP) {
                    return CTRL_P.code;
                }
                else if (key == ARROW_DOWN) {
                    return CTRL_N.code;
                }
                else if (key == ARROW_LEFT) {
                    return CTRL_B.code;
                }
                else if (key == ARROW_RIGHT) {
                    return CTRL_F.code;
                }
                else if (key == HOME_CODE) {
                    return CTRL_A.code;
                }
                else if (key == END_CODE) {
                    return CTRL_E.code;
                }
                else if (key == DEL_THIRD) {
                    readCharacter(in); // read 4th & ignore
                    return DELETE.code;
                }
            }
        }

        // handle unicode characters, thanks for a patch from amyi@inf.ed.ac.uk
        if (c > 128) {
            // handle unicode characters longer than 2 bytes,
            // thanks to Marc.Herbert@continuent.com
            replayStream.setInput(c, in);
            // replayReader = new InputStreamReader(replayStream, encoding);
            c = replayReader.read();
        }

        return c;
    }

    /**
     * Unix keys.
     */
    public static enum UnixKey
    {
        ARROW_START(27),

        ARROW_PREFIX(91),

        ARROW_LEFT(68),

        ARROW_RIGHT(67),

        ARROW_UP(65),

        ARROW_DOWN(66),

        O_PREFIX(79),

        HOME_CODE(72),

        END_CODE(70),

        DEL_THIRD(51),

        DEL_SECOND(126),;

        public final short code;

        UnixKey(final int code) {
            this.code = (short) code;
        }

        private static final Map<Short, UnixKey> codes;

        static {
            Map<Short, UnixKey> map = new HashMap<Short, UnixKey>();

            for (UnixKey key : UnixKey.values()) {
                map.put(key.code, key);
            }

            codes = map;
        }

        public static UnixKey valueOf(final int code) {
            return codes.get((short) code);
        }
    }
}
