package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.test.scenarios.manipulation.service.Plop;
//TODO this test requires source compatibility 1.5
public class PlopImpl implements Plop {

    public String getPlop() {
        return "plop";
    }

}
