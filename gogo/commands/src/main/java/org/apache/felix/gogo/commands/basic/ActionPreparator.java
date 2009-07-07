package org.apache.felix.gogo.commands.basic;

import java.util.List;

import org.osgi.service.command.CommandSession;
import org.apache.felix.gogo.commands.Action;

public interface ActionPreparator {

    boolean prepare(Action action, CommandSession session, List<Object> arguments) throws Exception;

}
