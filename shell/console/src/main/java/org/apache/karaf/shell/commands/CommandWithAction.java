package org.apache.karaf.shell.commands;

import org.apache.felix.service.command.Function;

public interface CommandWithAction extends Function {
    Class<? extends Action> getActionClass();

    Action createNewAction();

    void releaseAction(Action action) throws Exception;
}
