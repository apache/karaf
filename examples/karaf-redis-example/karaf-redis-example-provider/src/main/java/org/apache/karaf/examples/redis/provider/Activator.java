package org.apache.karaf.examples.redis.provider;

import org.apache.karaf.examples.redis.api.UserService;
import org.apache.karaf.examples.redis.api.UserServiceRedis;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;

@Services(
        provides = {
                @ProvideService(UserService.class)
        }
)
public class Activator extends BaseActivator {

    @Override
    protected void doStart() throws Exception {
        UserServiceImp userServiceImp = new UserServiceImp();
        register(UserService.class, userServiceImp);
        UserServiceRedisImp userServiceRedisImp = new UserServiceRedisImp();
        register(UserServiceRedis.class, userServiceRedisImp);
    }
}

