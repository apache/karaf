package org.apache.felix.ipojo.tests.inheritance.c;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.tests.inheritance.b.IB;

@Component
@Provides
public class C implements IB {

    public String methOne() { return "one";}
    public String methTwo() { return "two";}

}
