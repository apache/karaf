package org.apache.karaf.examples.redis.command;

import org.apache.karaf.examples.redis.api.UserService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "user", name = "remove-all", description = "Clear all users that exist in the current list.")
public class RemoveAllUserCommand implements Action {

    @Reference
    private UserService userService;

    public Object execute() throws Exception {
        userService.clear();
        return null;
    }
}
