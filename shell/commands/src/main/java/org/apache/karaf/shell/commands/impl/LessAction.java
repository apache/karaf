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
package org.apache.karaf.shell.commands.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import jline.console.KeyMap;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Signal;
import org.apache.karaf.shell.api.console.SignalListener;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.ansi.AnsiSplitter;
import org.jledit.jline.NonBlockingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "shell", name = "less", description = "File pager.")
@Service
public class LessAction implements Action, SignalListener {

    private static final int ESCAPE = 27;
    public static final int ESCAPE_TIMEOUT = 100;
    public static final int READ_EXPIRED = -2;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name = "-e", aliases = "--quit-at-eof")
    boolean quitAtSecondEof;

    @Option(name = "-E", aliases = "--QUIT-AT-EOF")
    boolean quitAtFirstEof;

    @Option(name = "-N", aliases = "--LINE-NUMBERS")
    boolean printLineNumbers;

    @Option(name = "-q", aliases = {"--quiet", "--silent"})
    boolean quiet;

    @Option(name = "-Q", aliases = {"--QUIET", "--SILENT"})
    boolean veryQuiet;

    @Option(name = "-S", aliases = "--chop-long-lines")
    boolean chopLongLines;

    @Option(name = "-i", aliases = "--ignore-case")
    boolean ignoreCaseCond;

    @Option(name = "-I", aliases = "--IGNORE-CASE")
    boolean ignoreCaseAlways;

    @Option(name = "-x", aliases = "--tabs")
    int tabs = 4;

    @Argument(multiValued = true)
    List<File> files;

    @Reference(optional = true)
    Terminal terminal;

    @Reference
    Session session;

    BufferedReader reader;

    NonBlockingInputStream consoleInput;
    Reader consoleReader;

    KeyMap keys;

    int firstLineInMemory = 0;
    List<String> lines = new ArrayList<>();

    int firstLineToDisplay = 0;
    int firstColumnToDisplay = 0;
    int offsetInLine = 0;

    String message;
    final StringBuilder buffer = new StringBuilder();
    final StringBuilder opBuffer = new StringBuilder();
    final Stack<Character> pushBackChar = new Stack<>();
    Thread displayThread;
    final AtomicBoolean redraw = new AtomicBoolean();

    final Map<String, Operation> options = new TreeMap<>();

    int window;
    int halfWindow;

    int nbEof;

    String pattern;

    @Override
    public Object execute() throws Exception {
        InputStream in;
        if (files != null && !files.isEmpty()) {
            message = files.get(0).toString();
            in = new FileInputStream(files.get(0));
        } else {
            in = System.in;
        }
        reader = new BufferedReader(new InputStreamReader(new InterruptibleInputStream(in)));
        try {
            if (terminal == null || !isTty(System.out)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    checkInterrupted();
                }
                return null;
            } else {
                boolean echo = terminal.isEchoEnabled();
                terminal.setEchoEnabled(false);
                terminal.addSignalListener(this, Signal.WINCH);
                try {
                    window = terminal.getHeight() - 1;
                    halfWindow = window / 2;
                    keys = new KeyMap("less", false);
                    bindKeys(keys);
                    consoleInput = new NonBlockingInputStream(session.getKeyboard(), true);
                    consoleReader = new InputStreamReader(consoleInput);

                    // Use alternate buffer
                    System.out.print("\u001B[?1049h");
                    System.out.flush();

                    displayThread = new Thread() {
                        @Override
                        public void run() {
                            redrawLoop();
                        }
                    };
                    displayThread.start();
                    redraw();
                    checkInterrupted();

                    options.put("-e", Operation.OPT_QUIT_AT_SECOND_EOF);
                    options.put("--quit-at-eof", Operation.OPT_QUIT_AT_SECOND_EOF);
                    options.put("-E", Operation.OPT_QUIT_AT_FIRST_EOF);
                    options.put("-QUIT-AT-EOF", Operation.OPT_QUIT_AT_FIRST_EOF);
                    options.put("-N", Operation.OPT_PRINT_LINES);
                    options.put("--LINE-NUMBERS", Operation.OPT_PRINT_LINES);
                    options.put("-q", Operation.OPT_QUIET);
                    options.put("--quiet", Operation.OPT_QUIET);
                    options.put("--silent", Operation.OPT_QUIET);
                    options.put("-Q", Operation.OPT_VERY_QUIET);
                    options.put("--QUIET", Operation.OPT_VERY_QUIET);
                    options.put("--SILENT", Operation.OPT_VERY_QUIET);
                    options.put("-S", Operation.OPT_CHOP_LONG_LINES);
                    options.put("--chop-long-lines", Operation.OPT_CHOP_LONG_LINES);
                    options.put("-i", Operation.OPT_IGNORE_CASE_COND);
                    options.put("--ignore-case", Operation.OPT_IGNORE_CASE_COND);
                    options.put("-I", Operation.OPT_IGNORE_CASE_ALWAYS);
                    options.put("--IGNORE-CASE", Operation.OPT_IGNORE_CASE_ALWAYS);

                    Operation op;
                    do {
                        checkInterrupted();

                        op = null;
                        //
                        // Option edition
                        //
                        if (buffer.length() > 0 && buffer.charAt(0) == '-') {
                            int c = consoleReader.read();
                            message = null;
                            if (buffer.length() == 1) {
                                buffer.append((char) c);
                                if (c != '-') {
                                    op = options.get(buffer.toString());
                                    if (op == null) {
                                        message = "There is no " + printable(buffer.toString()) + " option";
                                        buffer.setLength(0);
                                    }
                                }
                            } else if (c == '\r') {
                                op = options.get(buffer.toString());
                                if (op == null) {
                                    message = "There is no " + printable(buffer.toString()) + " option";
                                    buffer.setLength(0);
                                }
                            } else {
                                buffer.append((char) c);
                                Map<String, Operation> matching = new HashMap<>();
                                for (Map.Entry<String, Operation> entry : options.entrySet()) {
                                    if (entry.getKey().startsWith(buffer.toString())) {
                                        matching.put(entry.getKey(), entry.getValue());
                                    }
                                }
                                switch (matching.size()) {
                                case 0:
                                    buffer.setLength(0);
                                    break;
                                case 1:
                                    buffer.setLength(0);
                                    buffer.append(matching.keySet().iterator().next());
                                    break;
                                }
                            }

                        }
                        //
                        // Pattern edition
                        //
                        else if (buffer.length() > 0 && (buffer.charAt(0) == '/' || buffer.charAt(0) == '?')) {
                            int c = consoleReader.read();
                            message = null;
                            if (c == '\r') {
                                pattern = buffer.toString().substring(1);
                                if (buffer.charAt(0) == '/') {
                                    moveToNextMatch();
                                } else {
                                    moveToPreviousMatch();
                                }
                                buffer.setLength(0);
                            } else {
                                buffer.append((char) c);
                            }
                        }
                        //
                        // Command reading
                        //
                        else {
                            Object obj = readOperation();
                            message = null;
                            if (obj instanceof Character) {
                                char c = (char) obj;
                                // Enter option mode or pattern edit mode
                                if (c == '-' || c == '/' || c == '?') {
                                    buffer.setLength(0);
                                }
                                buffer.append((char) obj);
                            } else if (obj instanceof Operation) {
                                op = (Operation) obj;
                            }
                        }
                        if (op != null) {
                            switch (op) {
                            case FORWARD_ONE_LINE:
                                moveForward(getStrictPositiveNumberInBuffer(1));
                                break;
                            case BACKWARD_ONE_LINE:
                                moveBackward(getStrictPositiveNumberInBuffer(1));
                                break;
                            case FORWARD_ONE_WINDOW_OR_LINES:
                                moveForward(getStrictPositiveNumberInBuffer(window));
                                break;
                            case FORWARD_ONE_WINDOW_AND_SET:
                                window = getStrictPositiveNumberInBuffer(window);
                                moveForward(window);
                                break;
                            case FORWARD_ONE_WINDOW_NO_STOP:
                                moveForward(window);
                                // TODO: handle no stop
                                break;
                            case FORWARD_HALF_WINDOW_AND_SET:
                                halfWindow = getStrictPositiveNumberInBuffer(halfWindow);
                                moveForward(halfWindow);
                                break;
                            case BACKWARD_ONE_WINDOW_AND_SET:
                                window = getStrictPositiveNumberInBuffer(window);
                                moveBackward(window);
                                break;
                            case BACKWARD_ONE_WINDOW_OR_LINES:
                                moveBackward(getStrictPositiveNumberInBuffer(window));
                                break;
                            case BACKWARD_HALF_WINDOW_AND_SET:
                                halfWindow = getStrictPositiveNumberInBuffer(halfWindow);
                                moveBackward(halfWindow);
                                break;
                            case GO_TO_FIRST_LINE_OR_N:
                                // TODO: handle number
                                firstLineToDisplay = firstLineInMemory;
                                offsetInLine = 0;
                                break;
                            case GO_TO_LAST_LINE_OR_N:
                                // TODO: handle number
                                moveForward(Integer.MAX_VALUE);
                                break;
                            case LEFT_ONE_HALF_SCREEN:
                                firstColumnToDisplay = Math.max(0, firstColumnToDisplay - terminal.getWidth() / 2);
                                break;
                            case RIGHT_ONE_HALF_SCREEN:
                                firstColumnToDisplay += terminal.getWidth() / 2;
                                break;
                            case REPEAT_SEARCH_BACKWARD:
                            case REPEAT_SEARCH_BACKWARD_SPAN_FILES:
                                moveToPreviousMatch();
                                break;
                            case REPEAT_SEARCH_FORWARD:
                            case REPEAT_SEARCH_FORWARD_SPAN_FILES:
                                moveToNextMatch();
                                break;
                            case UNDO_SEARCH:
                                pattern = null;
                                break;
                            case OPT_PRINT_LINES:
                                buffer.setLength(0);
                                printLineNumbers = !printLineNumbers;
                                message = printLineNumbers ? "Constantly display line numbers" : "Don't use line numbers";
                                break;
                            case OPT_QUIET:
                                buffer.setLength(0);
                                quiet = !quiet;
                                veryQuiet = false;
                                message = quiet ? "Ring the bell for errors but not at eof/bof" : "Ring the bell for errors AND at eof/bof";
                                break;
                            case OPT_VERY_QUIET:
                                buffer.setLength(0);
                                veryQuiet = !veryQuiet;
                                quiet = false;
                                message = veryQuiet ? "Never ring the bell" : "Ring the bell for errors AND at eof/bof";
                                break;
                            case OPT_CHOP_LONG_LINES:
                                buffer.setLength(0);
                                offsetInLine = 0;
                                chopLongLines = !chopLongLines;
                                message = chopLongLines ? "Chop long lines" : "Fold long lines";
                                break;
                            case OPT_IGNORE_CASE_COND:
                                ignoreCaseCond = !ignoreCaseCond;
                                ignoreCaseAlways = false;
                                message = ignoreCaseCond ? "Ignore case in searches" : "Case is significant in searches";
                                break;
                            case OPT_IGNORE_CASE_ALWAYS:
                                ignoreCaseAlways = !ignoreCaseAlways;
                                ignoreCaseCond = false;
                                message = ignoreCaseAlways ? "Ignore case in searches and in patterns" : "Case is significant in searches";
                                break;
                            }
                            buffer.setLength(0);
                        }
                        redraw();
                        if (quitAtFirstEof && nbEof > 0 || quitAtSecondEof && nbEof > 1) {
                            op = Operation.EXIT;
                        }
                    } while (op != Operation.EXIT);
                } catch (InterruptedException ie) {
                    log.debug("Interrupted by user");
                } finally {
                    terminal.setEchoEnabled(echo);
                    terminal.removeSignalListener(this);
                    consoleInput.shutdown();
                    displayThread.interrupt();
                    displayThread.join();
                    // Use main buffer
                    System.out.print("\u001B[?1049l");
                    // Clear line
                    System.out.println();
                    System.out.flush();
                }
            }
        } finally {
            reader.close();
        }
        return null;
    }

    private void moveToNextMatch() throws IOException {
        Pattern compiled = getPattern();
        if (compiled != null) {
            for (int lineNumber = firstLineToDisplay + 1; ; lineNumber++) {
                String line = getLine(lineNumber);
                if (line == null) {
                    break;
                } else if (compiled.matcher(line).find()) {
                    firstLineToDisplay = lineNumber;
                    offsetInLine = 0;
                    return;
                }
            }
        }
        message = "Pattern not found";
    }

    private void moveToPreviousMatch() throws IOException {
        Pattern compiled = getPattern();
        if (compiled != null) {
            for (int lineNumber = firstLineToDisplay - 1; lineNumber >= firstLineInMemory; lineNumber--) {
                String line = getLine(lineNumber);
                if (line == null) {
                    break;
                } else if (compiled.matcher(line).find()) {
                    firstLineToDisplay = lineNumber;
                    offsetInLine = 0;
                    return;
                }
            }
        }
        message = "Pattern not found";
    }

    private String printable(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ESCAPE) {
                sb.append("ESC");
            } else if (c < 32) {
                sb.append('^').append((char) (c + '@'));
            } else if (c < 128) {
                sb.append(c);
            } else {
                sb.append('\\').append(String.format("03o"));
            }
        }
        return sb.toString();
    }

    void moveForward(int lines) throws IOException {
        int width = terminal.getWidth() - (printLineNumbers ? 8 : 0);
        int height = terminal.getHeight();
        while (--lines >= 0) {

            int lastLineToDisplay = firstLineToDisplay;
            if (firstColumnToDisplay > 0 || chopLongLines) {
                lastLineToDisplay += height - 1;
            } else {
                int off = offsetInLine;
                for (int l = 0; l < height - 1; l++) {
                    String line = getLine(lastLineToDisplay);
                    if (ansiLength(line) > off + width) {
                        off += width;
                    } else {
                        off = 0;
                        lastLineToDisplay++;
                    }
                }
            }
            if (getLine(lastLineToDisplay) == null) {
                eof();
                return;
            }

            String line = getLine(firstLineToDisplay);
            if (ansiLength(line) > width + offsetInLine) {
                offsetInLine += width;
            } else {
                offsetInLine = 0;
                firstLineToDisplay++;
            }
        }
    }

    void moveBackward(int lines) throws IOException {
        int width = terminal.getWidth() - (printLineNumbers ? 8 : 0);
        while (--lines >= 0) {
            if (offsetInLine > 0) {
                offsetInLine = Math.max(0, offsetInLine - width);
            } else if (firstLineInMemory < firstLineToDisplay) {
                firstLineToDisplay--;
                String line = getLine(firstLineToDisplay);
                int length = ansiLength(line);
                offsetInLine = length - length % width;
            } else {
                bof();
                return;
            }
        }
    }

    private void eof() {
        nbEof++;
        message = "(END)";
        if (!quiet && !veryQuiet && !quitAtFirstEof && !quitAtSecondEof) {
            System.out.print((char) 0x07);
            System.out.flush();
        }
    }

    private void bof() {
        if (!quiet && !veryQuiet) {
            System.out.print((char) 0x07);
            System.out.flush();
        }
    }

    int getStrictPositiveNumberInBuffer(int def) {
        try {
            int n = Integer.parseInt(buffer.toString());
            return (n > 0) ? n : def;
        } catch (NumberFormatException e) {
            return def;
        } finally {
            buffer.setLength(0);
        }
    }

    void redraw() {
        synchronized (redraw) {
            redraw.set(true);
            redraw.notifyAll();
        }
    }

    void redrawLoop() {
        synchronized (redraw) {
            for (; ; ) {
                try {
                    if (redraw.compareAndSet(true, false)) {
                        display();
                    } else {
                        redraw.wait();
                    }
                } catch (Exception e) {
                    return;
                }
            }
        }
    }

    void display() throws IOException {
        System.out.println();
        int width = terminal.getWidth() - (printLineNumbers ? 8 : 0);
        int height = terminal.getHeight();
        int inputLine = firstLineToDisplay;
        String curLine = null;
        Pattern compiled = getPattern();
        for (int terminalLine = 0; terminalLine < height - 1; terminalLine++) {
            if (curLine == null) {
                curLine = getLine(inputLine++);
                if (curLine == null) {
                    curLine = "";
                }
                if (compiled != null) {
                    curLine = compiled.matcher(curLine).replaceAll("\033[7m$1\033[27m");
                }
            }
            String toDisplay;
            if (firstColumnToDisplay > 0 || chopLongLines) {
                int off = firstColumnToDisplay;
                if (terminalLine == 0 && offsetInLine > 0) {
                    off = Math.max(offsetInLine, off);
                }
                toDisplay = ansiSubstring(curLine, off, off + width);
                curLine = null;
            } else {
                if (terminalLine == 0 && offsetInLine > 0) {
                    curLine = ansiSubstring(curLine, offsetInLine, Integer.MAX_VALUE);
                }
                toDisplay = ansiSubstring(curLine, 0, width);
                curLine = ansiSubstring(curLine, width, Integer.MAX_VALUE);
                if (curLine.isEmpty()) {
                    curLine = null;
                }
            }
            if (printLineNumbers) {
                System.out.print(String.format("%7d ", inputLine));
            }
            System.out.println(toDisplay);
        }
        System.out.flush();
        if (message != null) {
            System.out.print("\033[7m" + message + " \033[0m");
        } else if (buffer.length() > 0) {
            System.out.print(" " + buffer);
        } else if (opBuffer.length() > 0) {
            System.out.print(" " + printable(opBuffer.toString()));
        } else {
            System.out.print(":");
        }
        System.out.flush();
    }

    private Pattern getPattern() {
        Pattern compiled = null;
        if (pattern != null) {
            boolean insensitive = ignoreCaseAlways || ignoreCaseCond && pattern.toLowerCase().equals(pattern);
            compiled = Pattern.compile("(" + pattern + ")", insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        }
        return compiled;
    }

    private int ansiLength(String curLine) throws IOException {
        return AnsiSplitter.length(curLine, tabs);
    }

    private String ansiSubstring(String curLine, int begin, int end) throws IOException {
        return AnsiSplitter.substring(curLine, begin, end, tabs);
    }

    String getLine(int line) throws IOException {
        while (line <= lines.size()) {
            String str = reader.readLine();
            if (str != null) {
                lines.add(str);
            } else {
                break;
            }
        }
        if (line < lines.size()) {
            return lines.get(line);
        }
        return null;
    }

    @Override
    public void signal(Signal signal) {
        // Ugly hack to force the jline unix terminal to retrieve the width/height of the terminal
        // because results are cached for 1 second.
        try {
            Field field = terminal.getClass().getDeclaredField("terminal");
            field.setAccessible(true);
            Object jlineTerminal = field.get(terminal);
            field = jlineTerminal.getClass().getSuperclass().getDeclaredField("settings");
            field.setAccessible(true);
            Object settings = field.get(jlineTerminal);
            field = settings.getClass().getDeclaredField("configLastFetched");
            field.setAccessible(true);
            field.setLong(settings, 0L);
        } catch (Throwable t) {
            // Ignore
        }
        redraw();
    }

    protected boolean isTty(OutputStream out) {
        try {
            Method mth = out.getClass().getDeclaredMethod("getCurrent");
            mth.setAccessible(true);
            Object current = mth.invoke(out);
            return current == session.getConsole();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * This is for long running commands to be interrupted by ctrl-c
     *
     * @throws InterruptedException
     */
    public static void checkInterrupted() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }


    protected Object readOperation() throws IOException {
        int c = pushBackChar.isEmpty() ? consoleReader.read() : pushBackChar.pop();
        if (c == -1) {
            return null;
        }
        opBuffer.append((char) c);

        Object o = keys.getBound(opBuffer);
        if (o == jline.console.Operation.DO_LOWERCASE_VERSION) {
            opBuffer.setLength(opBuffer.length() - 1);
            opBuffer.append(Character.toLowerCase((char) c));
            o = keys.getBound(opBuffer);
        }

        if (o instanceof KeyMap) {
            if (c == ESCAPE
                    && pushBackChar.isEmpty()
                    && consoleInput.isNonBlockingEnabled()
                    && consoleInput.peek(ESCAPE_TIMEOUT) == READ_EXPIRED) {
                o = ((KeyMap) o).getAnotherKey();
                if (o == null || o instanceof KeyMap) {
                    return null;
                }
                opBuffer.setLength(0);
            } else {
                return null;
            }
        }

        while (o == null && opBuffer.length() > 0) {
            c = opBuffer.charAt(opBuffer.length() - 1);
            opBuffer.setLength(opBuffer.length() - 1);
            Object o2 = keys.getBound(opBuffer);
            if (o2 instanceof KeyMap) {
                o = ((KeyMap) o2).getAnotherKey();
                if (o != null) {
                    pushBackChar.push((char) c);
                }
            }
        }

        if (o != null) {
            opBuffer.setLength(0);
            pushBackChar.clear();
        }
        return o;
    }


    private void bindKeys(KeyMap map) {
        // Arrow keys bindings
        map.bind("\033[0A", Operation.BACKWARD_ONE_LINE);
        map.bind("\033[0B", Operation.LEFT_ONE_HALF_SCREEN);
        map.bind("\033[0C", Operation.RIGHT_ONE_HALF_SCREEN);
        map.bind("\033[0D", Operation.FORWARD_ONE_LINE);

        map.bind("\340\110", Operation.BACKWARD_ONE_LINE);
        map.bind("\340\113", Operation.LEFT_ONE_HALF_SCREEN);
        map.bind("\340\115", Operation.RIGHT_ONE_HALF_SCREEN);
        map.bind("\340\120", Operation.FORWARD_ONE_LINE);
        map.bind("\000\110", Operation.BACKWARD_ONE_LINE);
        map.bind("\000\113", Operation.LEFT_ONE_HALF_SCREEN);
        map.bind("\000\115", Operation.RIGHT_ONE_HALF_SCREEN);
        map.bind("\000\120", Operation.FORWARD_ONE_LINE);

        map.bind("\033[A", Operation.BACKWARD_ONE_LINE);
        map.bind("\033[B", Operation.FORWARD_ONE_LINE);
        map.bind("\033[C", Operation.RIGHT_ONE_HALF_SCREEN);
        map.bind("\033[D", Operation.LEFT_ONE_HALF_SCREEN);

        map.bind("\033[OA", Operation.BACKWARD_ONE_LINE);
        map.bind("\033[OB", Operation.FORWARD_ONE_LINE);
        map.bind("\033[OC", Operation.RIGHT_ONE_HALF_SCREEN);
        map.bind("\033[OD", Operation.LEFT_ONE_HALF_SCREEN);

        map.bind("\0340H", Operation.BACKWARD_ONE_LINE);
        map.bind("\0340P", Operation.FORWARD_ONE_LINE);
        map.bind("\0340M", Operation.RIGHT_ONE_HALF_SCREEN);
        map.bind("\0340K", Operation.LEFT_ONE_HALF_SCREEN);

        map.bind("h", Operation.HELP);
        map.bind("H", Operation.HELP);

        map.bind("q", Operation.EXIT);
        map.bind(":q", Operation.EXIT);
        map.bind("Q", Operation.EXIT);
        map.bind(":Q", Operation.EXIT);
        map.bind("ZZ", Operation.EXIT);

        map.bind("e", Operation.FORWARD_ONE_LINE);
        map.bind(ctrl('E'), Operation.FORWARD_ONE_LINE);
        map.bind("j", Operation.FORWARD_ONE_LINE);
        map.bind(ctrl('N'), Operation.FORWARD_ONE_LINE);
        map.bind("\r", Operation.FORWARD_ONE_LINE);

        map.bind("y", Operation.BACKWARD_ONE_LINE);
        map.bind(ctrl('Y'), Operation.BACKWARD_ONE_LINE);
        map.bind("k", Operation.BACKWARD_ONE_LINE);
        map.bind(ctrl('K'), Operation.BACKWARD_ONE_LINE);
        map.bind(ctrl('P'), Operation.BACKWARD_ONE_LINE);

        map.bind("f", Operation.FORWARD_ONE_WINDOW_OR_LINES);
        map.bind(ctrl('F'), Operation.FORWARD_ONE_WINDOW_OR_LINES);
        map.bind(ctrl('V'), Operation.FORWARD_ONE_WINDOW_OR_LINES);
        map.bind(" ", Operation.FORWARD_ONE_WINDOW_OR_LINES);

        map.bind("b", Operation.BACKWARD_ONE_WINDOW_OR_LINES);
        map.bind(ctrl('B'), Operation.BACKWARD_ONE_WINDOW_OR_LINES);
        map.bind("\033v", Operation.BACKWARD_ONE_WINDOW_OR_LINES);

        map.bind("z", Operation.FORWARD_ONE_WINDOW_AND_SET);

        map.bind("w", Operation.BACKWARD_ONE_WINDOW_AND_SET);

        map.bind("\033 ", Operation.FORWARD_ONE_WINDOW_NO_STOP);

        map.bind("d", Operation.FORWARD_HALF_WINDOW_AND_SET);
        map.bind(ctrl('D'), Operation.FORWARD_HALF_WINDOW_AND_SET);

        map.bind("u", Operation.BACKWARD_HALF_WINDOW_AND_SET);
        map.bind(ctrl('U'), Operation.BACKWARD_HALF_WINDOW_AND_SET);

        map.bind("\033)", Operation.RIGHT_ONE_HALF_SCREEN);

        map.bind("\033(", Operation.LEFT_ONE_HALF_SCREEN);

        map.bind("F", Operation.FORWARD_FOREVER);

        map.bind("n", Operation.REPEAT_SEARCH_FORWARD);
        map.bind("N", Operation.REPEAT_SEARCH_BACKWARD);
        map.bind("\033n", Operation.REPEAT_SEARCH_FORWARD_SPAN_FILES);
        map.bind("\033N", Operation.REPEAT_SEARCH_BACKWARD_SPAN_FILES);
        map.bind("\033u", Operation.UNDO_SEARCH);

        map.bind("g", Operation.GO_TO_FIRST_LINE_OR_N);
        map.bind("<", Operation.GO_TO_FIRST_LINE_OR_N);
        map.bind("\033<", Operation.GO_TO_FIRST_LINE_OR_N);

        map.bind("G", Operation.GO_TO_LAST_LINE_OR_N);
        map.bind(">", Operation.GO_TO_LAST_LINE_OR_N);
        map.bind("\033>", Operation.GO_TO_LAST_LINE_OR_N);

        for (char c : "-/0123456789?".toCharArray()) {
            map.bind("" + c, c);
        }
    }

    String ctrl(char c) {
        return "" + ((char) (c & 0x1f));
    }

    static enum Operation {

        // General
        HELP,
        EXIT,

        // Moving
        FORWARD_ONE_LINE,
        BACKWARD_ONE_LINE,
        FORWARD_ONE_WINDOW_OR_LINES,
        BACKWARD_ONE_WINDOW_OR_LINES,
        FORWARD_ONE_WINDOW_AND_SET,
        BACKWARD_ONE_WINDOW_AND_SET,
        FORWARD_ONE_WINDOW_NO_STOP,
        FORWARD_HALF_WINDOW_AND_SET,
        BACKWARD_HALF_WINDOW_AND_SET,
        LEFT_ONE_HALF_SCREEN,
        RIGHT_ONE_HALF_SCREEN,
        FORWARD_FOREVER,
        REPAINT,
        REPAINT_AND_DISCARD,

        // Searching
        REPEAT_SEARCH_FORWARD,
        REPEAT_SEARCH_BACKWARD,
        REPEAT_SEARCH_FORWARD_SPAN_FILES,
        REPEAT_SEARCH_BACKWARD_SPAN_FILES,
        UNDO_SEARCH,

        // Jumping
        GO_TO_FIRST_LINE_OR_N,
        GO_TO_LAST_LINE_OR_N,
        GO_TO_PERCENT_OR_N,
        GO_TO_NEXT_TAG,
        GO_TO_PREVIOUS_TAG,
        FIND_CLOSE_BRACKET,
        FIND_OPEN_BRACKET,

        // Options
        OPT_PRINT_LINES,
        OPT_CHOP_LONG_LINES,
        OPT_QUIT_AT_FIRST_EOF,
        OPT_QUIT_AT_SECOND_EOF,
        OPT_QUIET,
        OPT_VERY_QUIET,
        OPT_IGNORE_CASE_COND,
        OPT_IGNORE_CASE_ALWAYS,

    }

    static class InterruptibleInputStream extends FilterInputStream {
        InterruptibleInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }
            return super.read(b, off, len);
        }
    }

}
