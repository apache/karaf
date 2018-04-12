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
package org.apache.karaf.shell.impl.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.apache.karaf.shell.api.console.SignalListener;
import org.apache.karaf.shell.api.console.Terminal;
import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JLineTerminal implements Terminal, org.jline.terminal.Terminal {

    private final org.jline.terminal.Terminal terminal;
    private final ConcurrentMap<Signal, Set<SignalListener>> listeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<Signal, SignalHandler> handlers = new ConcurrentHashMap<>();
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JLineTerminal.class);

    public JLineTerminal(org.jline.terminal.Terminal terminal) {
        this.terminal = terminal;
        for (Signal signal : Signal.values()) {
            terminal.handle(signal, this::handle);
            handlers.put(signal, SignalHandler.SIG_DFL);
        }
    }

    @Override
    public String getType() {
        return terminal.getType();
    }

    @Override
    public boolean puts(Capability capability, Object... params) {
        return terminal.puts(capability, params);
    }

    @Override
    public boolean getBooleanCapability(Capability capability) {
        return terminal.getBooleanCapability(capability);
    }

    @Override
    public Integer getNumericCapability(Capability capability) {
        return terminal.getNumericCapability(capability);
    }

    @Override
    public String getStringCapability(Capability capability) {
        return terminal.getStringCapability(capability);
    }

    @Override
    public int getWidth() {
        return terminal.getWidth();
    }

    @Override
    public int getHeight() {
        return terminal.getHeight();
    }

    @Override
    public void flush() {
        terminal.flush();
    }

    @Override
    public boolean isAnsiSupported() {
        return true;
    }

    @Override
    public boolean isEchoEnabled() {
        return terminal.echo();
    }

    @Override
    public void setEchoEnabled(boolean enabled) {
        terminal.echo(enabled);
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }

    @Override
    public String getName() {
        return terminal.getName();
    }

    @Override
    public NonBlockingReader reader() {
        return terminal.reader();
    }

    @Override
    public PrintWriter writer() {
        return terminal.writer();
    }

    @Override
    public Charset encoding() {
        return terminal.encoding();
    }

    @Override
    public InputStream input() {
        return terminal.input();
    }

    @Override
    public OutputStream output() {
        return terminal.output();
    }

    @Override
    public Attributes enterRawMode() {
        return terminal.enterRawMode();
    }

    @Override
    public boolean echo() {
        return terminal.echo();
    }

    @Override
    public boolean echo(boolean echo) {
        return terminal.echo(echo);
    }

    @Override
    public Attributes getAttributes() {
        return terminal.getAttributes();
    }

    @Override
    public void setAttributes(Attributes attr) {
        terminal.setAttributes(attr);
    }

    @Override
    public Size getSize() {
        return terminal.getSize();
    }

    @Override
    public void setSize(Size size) {
        terminal.setSize(size);
    }

    @Override
    public void raise(Signal signal) {
        terminal.raise(signal);
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        Objects.requireNonNull(signal);
        Objects.requireNonNull(handler);
        return handlers.put(signal, handler);
    }

    @Override
    public void addSignalListener(SignalListener listener) {
        addSignalListener(listener, EnumSet.allOf(org.apache.karaf.shell.api.console.Signal.class));
    }

    @Override
    public void addSignalListener(SignalListener listener, org.apache.karaf.shell.api.console.Signal... signals) {
        for (org.apache.karaf.shell.api.console.Signal sig : signals) {
            addSignalListener(listener, sig);
        }
    }

    @Override
    public void addSignalListener(SignalListener listener, EnumSet<org.apache.karaf.shell.api.console.Signal> signals) {
        for (org.apache.karaf.shell.api.console.Signal sig : signals) {
            addSignalListener(listener, sig);
        }
    }

    private void addSignalListener(SignalListener listener, org.apache.karaf.shell.api.console.Signal signal) {
        Set<SignalListener> ls = listeners.compute(signal(signal), (s, l) -> l != null ? l : new CopyOnWriteArraySet<>());
        ls.add(listener);
    }

    @Override
    public void removeSignalListener(SignalListener listener) {
        for (Signal signal : Signal.values()) {
            Set<SignalListener> ls = listeners.get(signal);
            if (ls != null) {
                ls.remove(listener);
            }
        }
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        return terminal.getCursorPosition(discarded);
    }

    @Override
    public boolean hasMouseSupport() {
        return terminal.hasMouseSupport();
    }

    @Override
    public boolean trackMouse(MouseTracking tracking) {
        return terminal.trackMouse(tracking);
    }

    @Override
    public MouseEvent readMouseEvent() {
        return terminal.readMouseEvent();
    }

    @Override
    public MouseEvent readMouseEvent(IntSupplier supplier) {
        return terminal.readMouseEvent(supplier);
    }

    private Signal signal(org.apache.karaf.shell.api.console.Signal sig) {
        switch (sig) {
            case INT:
                return Signal.INT;
            case QUIT:
                return Signal.QUIT;
            case TSTP:
                return Signal.TSTP;
            case CONT:
                return Signal.CONT;
            case WINCH:
                return Signal.WINCH;
        }
        throw new UnsupportedOperationException();
    }

    private org.apache.karaf.shell.api.console.Signal signal(Signal sig) {
        switch (sig) {
            case INT:
                return org.apache.karaf.shell.api.console.Signal.INT;
            case QUIT:
                return org.apache.karaf.shell.api.console.Signal.QUIT;
            case TSTP:
                return org.apache.karaf.shell.api.console.Signal.TSTP;
            case CONT:
                return org.apache.karaf.shell.api.console.Signal.CONT;
            case WINCH:
                return org.apache.karaf.shell.api.console.Signal.WINCH;
        }
        throw new UnsupportedOperationException();
    }

    protected void handle(Signal signal) {
        SignalHandler handler = handlers.get(signal);
        if (handler != SignalHandler.SIG_DFL && handler != SignalHandler.SIG_IGN) {
            try {
                handler.handle(signal);
            } catch (UnsupportedOperationException uoe) {
                LOGGER.debug("unsupported operation", uoe);
            }
        }
        Set<SignalListener> sl = listeners.get(signal);
        if (sl != null) {
            for (SignalListener l : sl) {
                l.signal(signal(signal));
            }
        }
    }

    @Override
    public boolean canPauseResume() {
        return terminal.canPauseResume();
    }

    @Override
    public void pause() {
        terminal.pause();
    }

    @Override
    public void resume() {
        terminal.resume();
    }

    @Override
    public boolean paused() {
        return terminal.paused();
    }

    @Override
    public boolean hasFocusSupport() {
        return terminal.hasFocusSupport();
    }

    @Override
    public boolean trackFocus(boolean tracking) {
        return terminal.trackFocus(tracking);
    }
}
