package org.apache.karaf.examples.redis.command;

import org.apache.karaf.examples.redis.api.UserServiceRedis;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "user", name = "remove-all-redis", description = "Remove all users of current list on Redis.")
public class RemoveAllUserRedisCommand implements Action {

    @Reference
    private UserServiceRedis userServiceRedis;

    public Object execute() throws Exception {
        userServiceRedis.clear();
        return null;
    }
}
