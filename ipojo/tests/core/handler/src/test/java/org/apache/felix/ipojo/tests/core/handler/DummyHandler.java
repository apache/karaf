/*
 * Copyright 2009 OW2 Chameleon Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.apache.felix.ipojo.tests.core.handler;

import java.util.Dictionary;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.service.useradmin.User;


public class DummyHandler extends PrimitiveHandler {

    public DummyHandler() {
    }

    /*------------------------------------------------------------*
     *      Handler Specific Methods                              *
     *------------------------------------------------------------*/

    @Override
    public void initializeComponentFactory(ComponentTypeDescription typeDesc, Element metadata) throws ConfigurationException {
        // Initialize
        super.initializeComponentFactory(typeDesc, metadata);
    }

    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
    }


    private void bindUser(User user) {
        // in order to test
        user.getName();
    }

    private void unBindUser(User user) {
        // in order to test
        user.getType();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
