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
package org.apache.geronimo.gshell.obr;

import java.util.List;
import java.util.ArrayList;

import org.apache.geronimo.gshell.clp.CommandLineProcessor;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.clp.Printer;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.common.Arguments;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.BundleContextAware;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 3, 2007
 * Time: 9:44:39 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class OsgiCommandSupport implements Command, BundleContextAware {

    private BundleContext bundleContext;

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected CommandContext context;

    protected IO io;

    protected Variables variables;

    @Option(name="-h", aliases={"--help"}, description="Display this help message")
    private boolean displayHelp;

    public String getId() {
        CommandComponent cmd = getClass().getAnnotation(CommandComponent.class);
        if (cmd == null) {
            throw new IllegalStateException("Command id not found");
        }
        return cmd.id();
    }

    public String getDescription() {
        CommandComponent cmd = getClass().getAnnotation(CommandComponent.class);
        if (cmd == null) {
            throw new IllegalStateException("Command description not found");
        }
        return cmd.description();
    }

    public void setBundleContext(BundleContext context) {
        bundleContext = context;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public Object execute(final CommandContext context, final Object... args) throws Exception {
        OsgiCommandSupport cmd = getClass().newInstance();
        cmd.setBundleContext(bundleContext);
        cmd.init(context);
        return cmd.doExecute(args);
    }

    public void init(final CommandContext context) {
        assert context != null;

        this.context = context;
        this.io = context.getIO();
        this.variables = context.getVariables();

        // Re-setup logging using our id
        String id = getId();
        log = LoggerFactory.getLogger(getClass().getName() + "." + id);
    }

    public Object doExecute(final Object... args) throws Exception {
        assert args != null;

        log.info("Executing w/args: [{}]", Arguments.asString(args));

        CommandLineProcessor clp = new CommandLineProcessor(this);
        clp.process(Arguments.toStringArray(args));

        // Handle --help/-h automatically for the command
        if (displayHelp) {
            //
            // TODO: Make a special PrinterHandler to abstrat this muck from having to process it by hand
            //

            displayHelp(clp);

            return SUCCESS;
        }

        assert io != null;
        assert variables != null;

        return doExecute();
    }

    protected abstract Object doExecute() throws Exception;

    protected void displayHelp(final CommandLineProcessor clp) {
        assert clp != null;

        //
        // TODO: Need to ask the LayoutManager what the real name is for our command's ID
        //

        io.out.println(getId());
        io.out.println(" -- ");
        io.out.println();

        Printer printer = new Printer(clp);
        printer.printUsage(io.out);
        io.out.println();
    }

}