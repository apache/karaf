package org.apache.felix.ipojo.test.scenarios.component;

public class BadConstructors {
    
    public BadConstructors() {
        throw new Error("BAD");
    }
    
    public BadConstructors(int i) {
        // DO NOTHING
    }
    
    public static BadConstructors createBad() {
        throw new RuntimeException("BAD");
    }
    
    public static BadConstructors createBad2(int o) {
        return new BadConstructors(o);
    }

}
