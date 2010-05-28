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
package org.apache.felix.gogo.shell;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator, Runnable
{
    private ServiceTracker commandProcessorTracker;
    private Set<ServiceRegistration> regs = new HashSet<ServiceRegistration>();
    private CommandSession session;
    private Shell shell;
    private Thread thread;

    public void start(final BundleContext context) throws Exception
    {
        commandProcessorTracker = processorTracker(context);
    }

    public void stop(BundleContext context) throws Exception
    {
        if (thread != null)
        {
            thread.interrupt();
        }

        commandProcessorTracker.close();
        
        Iterator<ServiceRegistration> iterator = regs.iterator();
        while (iterator.hasNext())
        {
            ServiceRegistration reg = iterator.next();
            reg.unregister();
            iterator.remove();
        }
    }

    public void run()
    {
        try
        {
            Thread.sleep(100);    // wait for gosh command to be registered
            String args = System.getProperty("gosh.args", "");
            session.execute("gosh --login " + args);
        }
        catch (Exception e)
        {
            Object loc = session.get(".location");
            if (null == loc || !loc.toString().contains(":"))
            {
                loc = "gogo";
            }

            System.err.println(loc + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            session.close();
        }
    }

    private void startShell(BundleContext context, CommandProcessor processor)
    {
        Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put(CommandProcessor.COMMAND_SCOPE, "gogo");

        // register converters
        regs.add(context.registerService(Converter.class.getName(), new Converters(context), null));
        
        // register commands
        
        dict.put(CommandProcessor.COMMAND_FUNCTION, Builtin.functions);
        regs.add(context.registerService(Builtin.class.getName(), new Builtin(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Procedural.functions);
        regs.add(context.registerService(Procedural.class.getName(), new Procedural(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Posix.functions);
        regs.add(context.registerService(Posix.class.getName(), new Posix(), dict));

        dict.put(CommandProcessor.COMMAND_FUNCTION, Telnet.functions);
        regs.add(context.registerService(Telnet.class.getName(), new Telnet(processor), dict));
        
        shell = new Shell(context, processor);
        dict.put(CommandProcessor.COMMAND_FUNCTION, Shell.functions);
        regs.add(context.registerService(Shell.class.getName(), shell, dict));
        
        // start shell
        session = processor.createSession(System.in, System.out, System.err);
        thread = new Thread(this, "Gogo shell");
        thread.start();
    }

    private ServiceTracker processorTracker(BundleContext context)
    {
        ServiceTracker t = new ServiceTracker(context, CommandProcessor.class.getName(),
            null)
        {
            @Override
            public Object addingService(ServiceReference reference)
            {
                CommandProcessor processor = (CommandProcessor) super.addingService(reference);
                startShell(context, processor);
                return processor;
            }

            @Override
            public void removedService(ServiceReference reference, Object service)
            {
                if (thread != null)
                {
                    thread.interrupt();
                }
                super.removedService(reference, service);
            }
        };

        t.open();
        return t;
    }

}
