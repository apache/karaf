package org.apache.karaf.examples.redis.command;

import org.apache.karaf.examples.redis.api.User;
import org.apache.karaf.examples.redis.api.UserServiceRedis;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.Random;

@Service
@Command(scope = "user", name = "add-redis", description = "Add user to current list and save it on redis.")
public class AddUserToRedisCommand implements Action {

    @Reference
    private UserServiceRedis userServiceRedis;

    @Option(name = "-i", aliases = "--id", description = "ID of user.", required = false, multiValued = false)
    int id = 0;

    @Argument(index = 0, name = "firstName", description = "First name of user.", required = true, multiValued = false)
    String firstName;

    @Argument(index = 1, name = "lastName", description = "Last name of user.", required = true, multiValued = false)
    String lastName;

    @Argument(index = 2, name = "phoneNumber", description = "Phone number of user.", required = true, multiValued = false)
    String phoneNumber;

    public Object execute() throws Exception {
        if(id == 0){
            this.id = new Random().nextInt(5000);
        }
        User user = new User();
        user.setId(this.id);
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        user.setPhoneNumber(this.phoneNumber);
        userServiceRedis.add(user);
        return null;
    }
}
