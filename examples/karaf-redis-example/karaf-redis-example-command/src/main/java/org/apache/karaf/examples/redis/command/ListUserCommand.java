package org.apache.karaf.examples.redis.command;


import org.apache.karaf.examples.redis.api.User;
import org.apache.karaf.examples.redis.api.UserService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.Collection;

@Service
@Command(scope = "user", name = "list", description = "Get list of current user.")
public class ListUserCommand implements Action {

    @Reference
    private UserService userService;

    public Object execute() throws Exception {

        Collection<User> users = userService.list();
        ShellTable shellTable = new ShellTable();
        shellTable.column("ID");
        shellTable.column("First Name");
        shellTable.column("Last Name");
        shellTable.column("Phone Number");
        for(User user : users){
            shellTable.addRow().addContent(user.getId(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());
        }
        shellTable.print(System.out);

        return null;
    }
}
