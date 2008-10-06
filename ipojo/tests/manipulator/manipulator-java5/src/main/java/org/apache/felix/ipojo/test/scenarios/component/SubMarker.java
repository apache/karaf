package org.apache.felix.ipojo.test.scenarios.component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SubMarker {
    
    String subname();


}
