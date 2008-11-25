package org.apache.felix.ipojo.test.scenarios.component.event;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handlers.event.Subscriber;
import org.osgi.service.event.Event;


@Component
public class PubSub {
    @org.apache.felix.ipojo.handlers.event.Publisher(name="p1", synchronous=true)
    org.apache.felix.ipojo.handlers.event.publisher.Publisher publisher1;
    
    @org.apache.felix.ipojo.handlers.event.Publisher(name="p2", synchronous=false, topics="foo,bar", data_key="data")
    org.apache.felix.ipojo.handlers.event.publisher.Publisher publisher2;
    
    @org.apache.felix.ipojo.handlers.event.Publisher(name="p3", synchronous=true, topics="bar")
    org.apache.felix.ipojo.handlers.event.publisher.Publisher publisher3;
    
    @Subscriber(name="s1", data_key="data")
    public void receive1(Object foo) {
        // Nothing
    }
    
    @Subscriber(name="s2", topics="foo,bar", filter="(foo=true)")
    public void receive2(Event foo) {
        // Nothing
    }
    
    
    @Subscriber(name="s3", topics="foo", data_key="data", data_type="java.lang.String")
    public void receive3(String foo) {
        // Nothing
    }
    
    
    
}
