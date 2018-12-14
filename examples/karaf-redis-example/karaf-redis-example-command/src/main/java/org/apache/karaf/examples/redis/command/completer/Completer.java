package org.apache.karaf.examples.redis.command.completer;

import org.apache.karaf.examples.redis.api.User;
import org.apache.karaf.examples.redis.api.UserService;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import java.util.List;

@Service
public class Completer implements org.apache.karaf.shell.api.console.Completer {

    @Reference
    private UserService userService;

    public int complete(Session session, CommandLine commandLine, List<String> list) {
        StringsCompleter completer = new StringsCompleter();
        for(User user : userService.list()){
            completer.getStrings().add(String.valueOf(user.getId()));
        }
        return completer.complete(session, commandLine, list);
    }
}
