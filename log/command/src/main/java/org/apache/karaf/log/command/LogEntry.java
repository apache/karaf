package org.apache.karaf.log.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.osgi.service.log.LogService;

import java.util.HashMap;
import java.util.Map;

@Command(scope = "log", name = "log", description = "Log a message.")
public class LogEntry extends AbstractAction {

    @Argument(index = 0, name = "message", description = "The message to log", required = true, multiValued = false)
    private String message;

    @Option(name = "--level", aliases = {"-l"}, description = "The level the message will be logged at", required = false, multiValued = false)
    private String level = "INFO";

    private LogService logService;

    private final Map<String,Integer> mappings = new HashMap<String,Integer>();

    public LogEntry(LogService logService) {
        this.logService = logService;

        mappings.put("ERROR",1);
        mappings.put("WARNING",2);
        mappings.put("INFO",3);
        mappings.put("DEBUG",4);
    }

    @Override
    protected Object doExecute() throws Exception {
        logService.log(toLevel(level.toUpperCase()), message);

        return null;
    }

    private int toLevel(String logLevel) {
        Integer level =  mappings.get(logLevel);
        if(level == null) {
            level = 3;
        }
        return level;
    }

}
