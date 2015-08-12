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

import jline.TerminalSupport;
import org.apache.karaf.shell.api.console.Signal;
import org.apache.karaf.shell.api.console.SignalListener;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.sshd.server.Environment;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class SshTerminal extends TerminalSupport implements Terminal {

    private Environment environment;
    private final Map<Signal, Set<SignalListener>> listeners;

    public SshTerminal(Environment environment) {
        super(true);
        setAnsiSupported(true);
        listeners = new ConcurrentHashMap<>(3);
        this.environment = environment;
        this.environment.addSignalListener(new org.apache.sshd.server.SignalListener() {
            @Override
            public void signal(org.apache.sshd.server.Signal signal) {
                SshTerminal.this.signal(Signal.WINCH);
            }
        }, org.apache.sshd.server.Signal.WINCH);
    }

    public void init() throws Exception {
    }

    public void restore() throws Exception {
    }

    public void addSignalListener(SignalListener listener, Signal... signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals may not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        for (Signal s : signals) {
            getSignalListeners(s, true).add(listener);
        }
    }

    public void addSignalListener(SignalListener listener) {
        addSignalListener(listener, EnumSet.allOf(Signal.class));
    }

    public void addSignalListener(SignalListener listener, EnumSet<Signal> signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals may not be null");
        }
        addSignalListener(listener, signals.toArray(new Signal[signals.size()]));
    }

    public void removeSignalListener(SignalListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener may not be null");
        }
        for (Signal s : EnumSet.allOf(Signal.class)) {
            final Set<SignalListener> ls = getSignalListeners(s, false);
            if (ls != null) {
                ls.remove(listener);
            }
        }
    }

    public void signal(Signal signal) {
        final Set<SignalListener> ls = getSignalListeners(signal, false);
        if (ls != null) {
            for (SignalListener l : ls) {
                l.signal(signal);
            }
        }
    }

    protected Set<SignalListener> getSignalListeners(Signal signal, boolean create) {
        Set<SignalListener> ls = listeners.get(signal);
        if (ls == null && create) {
            synchronized (listeners) {
                ls = listeners.get(signal);
                if (ls == null) {
                    ls = new CopyOnWriteArraySet<>();
                    listeners.put(signal, ls);
                }
            }
        }
        // may be null in case create=false
        return ls;
    }

    @Override
    public int getWidth() {
        int width = 0;
        try {
            width = Integer.valueOf(this.environment.getEnv().get(Environment.ENV_COLUMNS));
        } catch (Throwable t) {
            // Ignore
        }
        return width > 0 ? width : super.getWidth();
    }

    @Override
    public int getHeight() {
        int height = 0;
        try {
            height = Integer.valueOf(this.environment.getEnv().get(Environment.ENV_LINES));
        } catch (Throwable t) {
            // Ignore
        }
        return height > 0 ? height : super.getHeight();
    }

}