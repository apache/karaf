package org.apache.servicemix.kernel.gshell.core.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class NamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser(CommandParser.COMMAND_BUNDLE, new CommandParser());
    }

}
