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
package org.apache.karaf.shell.impl.action.osgi;

import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.impl.action.command.ManagerImpl;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle extender scanning for command classes.
 */
public class CommandExtender extends AbstractExtender {

    public static final String KARAF_COMMANDS = "Karaf-Commands";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExtender.class);

    //
    // Adapt BundleActivator to make it blueprint friendly
    //

    private Registry registry;

    public CommandExtender(Registry registry) {
        setSynchronous(true);
        this.registry = registry;
        this.registry.register(new ManagerImpl(this.registry, this.registry));
    }

    //
    // Extender implementation
    //

    @Override
    protected Extension doCreateExtension(Bundle bundle) throws Exception {
        if (bundle.getHeaders().get(KARAF_COMMANDS) != null) {
            return new CommandExtension(bundle, registry);
        }
        return null;
    }

    @Override
    protected void debug(Bundle bundle, String msg) {
        StringBuilder buf = new StringBuilder();
        if ( bundle != null )
        {
            buf.append( bundle.getSymbolicName() );
            buf.append( " (" );
            buf.append( bundle.getBundleId() );
            buf.append( "): " );
        }
        buf.append(msg);
        LOGGER.debug(buf.toString());
    }

    @Override
    protected void warn(Bundle bundle, String msg, Throwable t) {
        StringBuilder buf = new StringBuilder();
        if ( bundle != null )
        {
            buf.append( bundle.getSymbolicName() );
            buf.append( " (" );
            buf.append( bundle.getBundleId() );
            buf.append( "): " );
        }
        buf.append(msg);
        LOGGER.warn(buf.toString(), t);
    }

    @Override
    protected void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }

}
