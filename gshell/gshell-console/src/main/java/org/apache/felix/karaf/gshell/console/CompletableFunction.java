package org.apache.felix.karaf.gshell.console;

import java.util.List;

import org.osgi.service.command.Function;

public interface CompletableFunction extends Function {

    List<Completer> getCompleters();

}
