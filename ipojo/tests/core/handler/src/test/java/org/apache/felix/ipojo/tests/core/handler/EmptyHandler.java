package org.apache.felix.ipojo.tests.core.handler;

import java.util.Dictionary;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;

public class EmptyHandler extends PrimitiveHandler {

    @Override
    public void configure(Element arg0, Dictionary arg1)
            throws ConfigurationException {
        info("Configured");
    }

    @Override
    public void start() {
        info("Started");
    }

    @Override
    public void stop() {
        info("Stopped");
    }

}
