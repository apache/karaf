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
package org.apache.felix.gogo.runtime;

import org.apache.felix.gogo.runtime.osgi.OSGiCommands;
import org.apache.felix.gogo.runtime.osgi.OSGiConverters;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.gogo.runtime.shell.CommandProxy;
import org.apache.felix.gogo.runtime.shell.CommandProcessorImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.Converter;
import org.osgi.service.command.Function;
import org.osgi.service.threadio.ThreadIO;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Activator implements BundleActivator
{
    private CommandProcessorImpl processor;
    private ThreadIOImpl threadio;
    private ServiceTracker commandTracker;
    private ServiceTracker converterTracker;
    private ServiceTracker felixTracker;
    private ServiceRegistration processorRegistration;
    private ServiceRegistration threadioRegistration;
    private Map<ServiceReference, ServiceRegistration> felixRegistrations;
    private OSGiCommands commands;
    private OSGiConverters converters;
    private ServiceRegistration convertersRegistration;
    
    protected CommandProcessorImpl newProcessor(ThreadIO tio)
    {
        return new CommandProcessorImpl(threadio);
    }

    public void start(final BundleContext context) throws Exception
    {
        threadio = new ThreadIOImpl();
        threadio.start();
        threadioRegistration = context.registerService(ThreadIO.class.getName(),
            threadio, null);

        processor = newProcessor(threadio);
        processorRegistration = context.registerService(CommandProcessor.class.getName(),
            processor, null);
        
        commandTracker = trackOSGiCommands(context);
        commandTracker.open();

        felixRegistrations = new HashMap<ServiceReference, ServiceRegistration>();
        felixTracker = trackFelixCommands(context);
        felixTracker.open();

        converterTracker = new ServiceTracker(context, Converter.class.getName(), null)
        {
            @Override
            public Object addingService(ServiceReference reference)
            {
                Converter converter = (Converter) super.addingService(reference);
                processor.addConverter(converter);
                return converter;
            }

            @Override
            public void removedService(ServiceReference reference, Object service)
            {
                processor.removeConverter((Converter) service);
                super.removedService(reference, service);
            }
        };
        converterTracker.open();

        // FIXME: optional?
        commands = new OSGiCommands(context);
        commands.registerCommands(processor, context.getBundle());
        converters = new OSGiConverters(context);
        convertersRegistration = context.registerService(Converter.class.getCanonicalName(), converters, null);
    }

    public void stop(BundleContext context) throws Exception
    {
        convertersRegistration.unregister();
        processorRegistration.unregister();
        threadioRegistration.unregister();
        
        commandTracker.close();
        converterTracker.close();
        felixTracker.close();

        threadio.stop();
    }

    private ServiceTracker trackOSGiCommands(final BundleContext context)
        throws InvalidSyntaxException
    {
        Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*))",
            CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION));

        return new ServiceTracker(context, filter, null)
        {
            @Override
            public Object addingService(ServiceReference reference)
            {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
                Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
                List<Object> commands = new ArrayList<Object>();

                if (scope != null && function != null)
                {
                    if (function.getClass().isArray())
                    {
                        for (Object f : ((Object[]) function))
                        {
                            Function target = new CommandProxy(context, reference,
                                f.toString());
                            processor.addCommand(scope.toString(), target, f.toString());
                            commands.add(target);
                        }
                    }
                    else
                    {
                        Function target = new CommandProxy(context, reference,
                            function.toString());
                        processor.addCommand(scope.toString(), target, function.toString());
                        commands.add(target);
                    }
                    return commands;
                }
                return null;
            }

            @Override
            public void removedService(ServiceReference reference, Object service)
            {
                Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
                Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);

                if (scope != null && function != null)
                {
                    if (!function.getClass().isArray())
                    {
                        processor.removeCommand(scope.toString(), function.toString());
                    }
                    else
                    {
                        for (Object func : (Object[]) function)
                        {
                            processor.removeCommand(scope.toString(), func.toString());
                        }
                    }
                }

                super.removedService(reference, service);
            }
        };
    }

    private ServiceTracker trackFelixCommands(final BundleContext context)
    {
        return new ServiceTracker(context, FelixCommandAdaptor.FELIX_COMMAND, null)
        {
            @Override
            public Object addingService(ServiceReference ref)
            {
                Object felixCommand = super.addingService(ref);
                try
                {
                    FelixCommandAdaptor adaptor = new FelixCommandAdaptor(felixCommand);
                    felixRegistrations.put(ref, context.registerService(
                        FelixCommandAdaptor.class.getName(), adaptor,
                        adaptor.getAttributes()));
                    return felixCommand;
                }
                catch (Exception e)
                {
                    System.err.println("felixcmd: " + e);
                    return null;
                }
            }

            @Override
            public void removedService(ServiceReference reference, Object service)
            {
                ServiceRegistration reg = felixRegistrations.remove(reference);
                if (reg != null)
                {
                    reg.unregister();
                }
                super.removedService(reference, service);
            }
        };
    }
}
