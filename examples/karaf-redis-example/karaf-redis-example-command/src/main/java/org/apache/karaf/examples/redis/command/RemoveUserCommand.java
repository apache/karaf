package org.apache.karaf.examples.redis.command;


import org.apache.karaf.examples.redis.api.UserService;
import org.apache.karaf.examples.redis.command.completer.Completer;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Service
@Command(scope = "user", name = "remove", description = "Remove user from list by ID.")
public class RemoveUserCommand implements Action {

    @Reference
    private UserService userService;

    @Argument(index = 0, name = "ids", description = "ID of user that you want to remove him/his.", required = true, multiValued = true)
    @Completion(Completer.class)
    List<Integer> ids;

    public Object execute() throws Exception {
        for(int id : ids){
            userService.remove(id);
        }
        return null;
    }
}
