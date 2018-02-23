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
package org.apache.karaf.shell.impl.console.commands;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.command.Process;
import org.jline.builtins.Options;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

public class Procedural {

    static final String[] functions = {"each", "if", "not", "throw", "try", "until", "while", "break", "continue"};

    public Object _main(CommandSession session, Object[] argv) throws Throwable {
        if (argv == null || argv.length < 1) {
            throw new IllegalArgumentException();
        }
        Process process = Process.Utils.current();
        try {
            return run(session, process, argv);
        } catch (OptionException e) {
            process.err().println(e.getMessage());
            process.error(2);
        } catch (HelpException e) {
            process.err().println(e.getMessage());
            process.error(0);
        } catch (ThrownException e) {
            process.error(1);
            throw e.getCause();
        }
        return null;
    }

    protected static class OptionException extends Exception {
        public OptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    protected static class HelpException extends Exception {
        public HelpException(String message) {
            super(message);
        }
    }

    protected static class ThrownException extends Exception {
        public ThrownException(Throwable cause) {
            super(cause);
        }
    }

    protected static class BreakException extends Exception {
    }

    protected static class ContinueException extends Exception {
    }

    protected Options parseOptions(CommandSession session, String[] usage, Object[] argv) throws HelpException, OptionException {
        try {
            Options opt = Options.compile(usage, s -> get(session, s)).parse(argv, true);
            if (opt.isSet("help")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                opt.usage(new PrintStream(baos));
                throw new HelpException(baos.toString());
            }
            return opt;
        } catch (IllegalArgumentException e) {
            throw new OptionException(e.getMessage(), e);
        }
    }

    protected String get(CommandSession session, String name) {
        Object o = session.get(name);
        return o != null ? o.toString() : null;
    }

    protected Object run(CommandSession session, Process process, Object[] argv) throws Throwable {
        switch (argv[0].toString()) {
            case "each":
                return doEach(session, process, argv);
            case "if":
                return doIf(session, process, argv);
            case "not":
                return doNot(session, process, argv);
            case "throw":
                return doThrow(session, process, argv);
            case "try":
                return doTry(session, process, argv);
            case "until":
                return doUntil(session, process, argv);
            case "while":
                return doWhile(session, process, argv);
            case "break":
                return doBreak(session, process, argv);
            case "continue":
                return doContinue(session, process, argv);
            default:
                throw new UnsupportedOperationException();
        }
    }

    protected List<Object> doEach(CommandSession session,
                                  Process process,
                                  Object[] argv) throws Exception {
        String[] usage = {
                "each -  loop over the elements",
                "Usage: each [-r] elements [do] { closure }",
                "         elements              an array to iterate on",
                "         closure               a closure to call",
                "  -? --help                    Show help",
                "  -r --result                  Return a list containing each iteration result",
        };
        Options opt = parseOptions(session, usage, argv);

        Collection<Object> elements = getElements(opt);
        if (opt.argObjects().size() > 0 && "do".equals(opt.argObjects().get(0))) {
            opt.argObjects().remove(0);
        }
        List<Function> functions = getFunctions(opt);


        if (elements == null || functions == null || functions.size() != 1) {
            process.err().println("usage: each elements [do] { closure }");
            process.err().println("       elements: an array to iterate on");
            process.err().println("       closure: a function or closure to call");
            process.error(2);
            return null;
        }

        List<Object> args = new ArrayList<>();
        List<Object> results = new ArrayList<>();
        args.add(null);

        for (Object x : elements) {
            checkInterrupt();
            args.set(0, x);
            try {
                results.add(functions.get(0).execute(session, args));
            } catch (BreakException b) {
                break;
            } catch (ContinueException c) {
                continue;
            }
        }

        return opt.isSet("result") ? results : null;
    }

    protected Object doIf(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "if -  if / then / else construct",
                "Usage: if {condition} [then] {if-action} [elif {cond} [then] {elif-action}]... [else] {else-action}",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);

        List<Function> conditions = new ArrayList<>();
        List<Function> actions = new ArrayList<>();
        Function elseFunction = null;
        int step = 0;
        boolean error = false;
        for (Object obj : opt.argObjects()) {
            switch (step) {
                case 0:
                    if (obj instanceof Function) {
                        conditions.add((Function) obj);
                    } else {
                        error = true;
                    }
                    step = 1;
                    break;
                case 1:
                    if ("then".equals(obj)) {
                        step = 2;
                        break;
                    }
                case 2:
                    if (obj instanceof Function) {
                        actions.add((Function) obj);
                        step = 3;
                    } else {
                        error = true;
                    }
                    break;
                case 3:
                    if ("elif".equals(obj)) {
                        step = 4;
                    } else if ("else".equals(obj)) {
                        step = 7;
                    } else if (obj instanceof Function) {
                        elseFunction = (Function) obj;
                        step = 8;
                    } else {
                        error = true;
                    }
                    break;
                case 4:
                    if (obj instanceof Function) {
                        conditions.add((Function) obj);
                    } else {
                        error = true;
                    }
                    step = 5;
                    break;
                case 5:
                    if ("then".equals(obj)) {
                        step = 6;
                        break;
                    }
                case 6:
                    if (obj instanceof Function) {
                        actions.add((Function) obj);
                        step = 3;
                    } else {
                        error = true;
                    }
                    break;
                case 7:
                    if (obj instanceof Function) {
                        elseFunction = (Function) obj;
                        step = 8;
                    } else {
                        error = true;
                    }
                    break;
                case 8:
                    error = true;
                    break;
            }
            if (error) {
                break;
            }
        }
        error |= conditions.isEmpty();
        error |= conditions.size() != actions.size();

        if (error) {
            process.err().println("usage: if {condition} [then] {if-action} [elif {elif-action}]... [else] {else-action}");
            process.error(2);
            return null;
        }
        for (int i = 0, length = conditions.size(); i < length; ++i) {
            if (isTrue(session, conditions.get(i))) {
                return actions.get(i).execute(session, null);
            }
        }
        if (elseFunction != null) {
            return elseFunction.execute(session, null);
        }
        return null;
    }

    protected Boolean doNot(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "not -  return the opposite condition",
                "Usage: not { condition }",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        List<Function> functions = getFunctions(opt);
        if (functions == null || functions.size() != 1) {
            process.err().println("usage: not { condition }");
            process.error(2);
            return null;
        }
        return !isTrue(session, functions.get(0));

    }

    protected Object doThrow(CommandSession session, Process process, Object[] argv) throws ThrownException, HelpException, OptionException {
        String[] usage = {
                "throw -  throw an exception",
                "Usage: throw [ message [ cause ] ]",
                "       throw exception",
                "       throw",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        if (opt.argObjects().size() == 0) {
            Object exception = session.get("exception");
            if (exception instanceof Throwable)
                throw new ThrownException((Throwable) exception);
            else
                throw new ThrownException(new Exception());
        }
        else if (opt.argObjects().size() == 1 && opt.argObjects().get(0) instanceof Throwable) {
            throw new ThrownException((Throwable) opt.argObjects().get(0));
        }
        else {
            String message = opt.argObjects().get(0).toString();
            Throwable cause = null;
            if (opt.argObjects().size() > 1) {
                if (opt.argObjects().get(1) instanceof Throwable) {
                    cause = (Throwable) opt.argObjects().get(1);
                }
            }
            throw new ThrownException(new Exception(message).initCause(cause));
        }
    }

    protected Object doTry(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "try -  try / catch / finally construct",
                "Usage: try { try-action } [ [catch] { catch-action } [ [finally] { finally-action } ]  ]",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        Function tryAction = null;
        Function catchFunction = null;
        Function finallyFunction = null;
        int step = 0;
        boolean error = false;
        for (Object obj : opt.argObjects()) {
            if (tryAction == null) {
                if (obj instanceof Function) {
                    tryAction = (Function) obj;
                } else {
                    error = true;
                    break;
                }
                step = 1;
            } else if ("catch".equals(obj)) {
                if (step != 1) {
                    error = true;
                    break;
                }
                step = 2;
            } else if ("finally".equals(obj)) {
                if (step != 1 && step != 3) {
                    error = true;
                    break;
                }
                step = 4;
            } else if (step == 1 || step == 2) {
                if (obj instanceof Function) {
                    catchFunction = (Function) obj;
                } else {
                    error = true;
                    break;
                }
                step = 3;
            } else if (step == 3 || step == 4) {
                if (obj instanceof Function) {
                    finallyFunction = (Function) obj;
                } else {
                    error = true;
                    break;
                }
                step = 5;
            } else {
                error = true;
                break;
            }
        }
        error |= tryAction == null;
        error |= catchFunction == null && finallyFunction == null;

        if (error) {
            process.err().println("usage: try { try-action } [ [catch] { catch-action } [ [finally] { finally-action } ] ]");
            process.error(2);
            return null;
        }
        try {
            return tryAction.execute(session, null);
        } catch (BreakException b) {
            throw b;
        } catch (Exception e) {
            session.put("exception", e);
            if (catchFunction != null) {
                catchFunction.execute(session, null);
            }
            return null;
        } finally {
            if (finallyFunction != null) {
                finallyFunction.execute(session, null);
            }
        }
    }

    protected Object doWhile(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "while -  while loop",
                "Usage: while { condition } [do] { action }",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        Function condition = null;
        Function action = null;
        int step = 0;
        boolean error = false;
        for (Object obj : opt.argObjects()) {
            if (condition == null) {
                if (obj instanceof Function) {
                    condition = (Function) obj;
                } else {
                    error = true;
                    break;
                }
                step = 1;
            } else if ("do".equals(obj)) {
                if (step != 1) {
                    error = true;
                    break;
                }
                step = 2;
            } else if (step == 1 || step == 2) {
                if (obj instanceof Function) {
                    action = (Function) obj;
                } else {
                    error = true;
                    break;
                }
                step = 3;
            } else {
                error = true;
                break;
            }
        }
        error |= condition == null;
        error |= action == null;

        if (error) {
            process.err().println("usage: while { condition } [do] { action }");
            process.error(2);
            return null;
        }
        while (isTrue(session, condition)) {
            try {
                action.execute(session, null);
            } catch (BreakException b) {
                break;
            } catch (ContinueException c) {
                continue;
            }
        }
        return null;
    }

    protected Object doUntil(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "until -  until loop",
                "Usage: until { condition } [do] { action }",
                "  -? --help                    Show help",
        };
        Options opt = parseOptions(session, usage, argv);
        Function condition = null;
        Function action = null;
        int step = 0;
        boolean error = false;
        for (Object obj : opt.argObjects()) {
            if (condition == null) {
                if (obj instanceof Function) {
                    condition = (Function) obj;
                } else {
                    error = true;
                    break;
                }
                step = 1;
            } else if ("do".equals(obj)) {
                if (step != 1) {
                    error = true;
                    break;
                }
                step = 2;
            } else if (step == 1 || step == 2) {
                if (obj instanceof Function) {
                    action = (Function) obj;
                } else {
                    error = true;
                    break;
                }
                step = 3;
            } else {
                error = true;
                break;
            }
        }
        error |= condition == null;
        error |= action == null;

        if (error) {
            process.err().println("usage: until { condition } [do] { action }");
            process.error(2);
            return null;
        }
        while (!isTrue(session, condition)) {
            try {
                action.execute(session, null);
            } catch (BreakException e) {
                break;
            } catch (ContinueException c) {
                continue;
            }
        }
        return null;
    }

    protected Object doBreak(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "break -  break from loop",
                "Usage: break",
                "  -? --help                    Show help",
        };
        parseOptions(session, usage, argv);
        throw new BreakException();
    }

    protected Object doContinue(CommandSession session, Process process, Object[] argv) throws Exception {
        String[] usage = {
                "continue -  continue loop",
                "Usage: continue",
                "  -? --help                    Show help",
        };
        parseOptions(session, usage, argv);
        throw new ContinueException();
    }

    private boolean isTrue(CommandSession session, Function function) throws Exception {
        checkInterrupt();
        return isTrue(function.execute(session, null));
    }

    private boolean isTrue(Object result) throws InterruptedException {
        checkInterrupt();

        if (result == null)
            return false;

        if (result instanceof Boolean)
            return (Boolean) result;

        if (result instanceof Number) {
            if (0 == ((Number) result).intValue())
                return false;
        }

        if ("".equals(result))
            return false;

        if ("0".equals(result))
            return false;

        return true;
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("interrupted");
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> getElements(Options opt) {
        Collection<Object> elements = null;
        if (opt.argObjects().size() > 0) {
            Object o = opt.argObjects().remove(0);
            if (o instanceof Collection) {
                elements = (Collection<Object>) o;
            } else if (o != null && o.getClass().isArray()) {
                elements = Arrays.asList((Object[]) o);
            }
        }
        return elements;
    }

    private List<Function> getFunctions(Options opt) {
        List<Function> functions = new ArrayList<>();
        for (Object o : opt.argObjects()) {
            if (o instanceof Function) {
                functions.add((Function) o);
            }
            else {
                functions = null;
                break;
            }
        }
        return functions;
    }

}
