package org.apache.karaf.examples.redis.provider;

import com.google.gson.GsonBuilder;
import org.apache.karaf.examples.redis.api.User;
import org.apache.karaf.examples.redis.api.UserServiceRedis;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserServiceRedisImp implements UserServiceRedis {

    private static final String USER_LIST_NAME = "USERLIST";

    public void add(User user) {
        Jedis redis = new Jedis("localhost");
        redis.lpush(USER_LIST_NAME, new GsonBuilder().create().toJson(user, User.class));
        redis.close();
    }

    public void remove(int id) {
        Jedis redis = new Jedis("localhost");
        Map<Integer, User> users = this.getUserMap();
        if(users.keySet().contains(id)){
            for(int i = 0; i < redis.llen(USER_LIST_NAME); i++){
                String userJson = redis.lindex(USER_LIST_NAME, i);
                User user = new GsonBuilder().create().fromJson(userJson, User.class);
                if(user.getId() == id){
                    redis.lrem(USER_LIST_NAME, 1, userJson);
                    break;
                }
            }
        }
        redis.close();
    }

    public Collection<User> list() {
        return this.getUserMap().values();
    }

    public void clear() {
        Jedis redis = new Jedis("localhost");
        redis.del(USER_LIST_NAME);
        redis.close();
    }

    private Map<Integer, User> getUserMap(){
        Map<Integer, User> users = new HashMap<Integer, User>();
        Jedis redis = new Jedis("localhost");
        for(int i = 0; i < redis.llen(USER_LIST_NAME); i++){
            User user = new GsonBuilder().create().fromJson(redis.lindex(USER_LIST_NAME, i), User.class);
            users.put(user.getId(), user);
        }
        redis.close();
        return users;
    }
}
