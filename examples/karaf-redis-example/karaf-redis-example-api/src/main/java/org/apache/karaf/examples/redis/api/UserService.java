package org.apache.karaf.examples.redis.api;

import java.util.Collection;

public interface UserService {

    void add(User user);
    void remove(int id);
    Collection<User> list();
    void clear();

}
