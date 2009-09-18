package org.apache.felix.karaf.shell.console;

import java.util.List;

import org.osgi.service.command.Function;

public interface CompletableFunction extends Function {

    List<Completer> getCompleters();

}
