package org.apache.felix.ipojo.tests.inheritance.d;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.tests.inheritance.a.IA;
import org.apache.felix.ipojo.tests.inheritance.b.IB;

@Component
public class D {
    @Requires
    private IB[] cImpls;
    private IB cImplDesired;

    // works if I use following instead and cast to C type below
    // in for loop
    // but this creates dependency on bundle C instead of just
    // the interface bundles A & B
    // @Requires(default-implementation=C)
    // private iB[] cImpls;
    // private C cImplDesired;

    @Validate
    public void start() {
        for( IB iimpl : cImpls) {

            // works just fine
            System.out.println(iimpl.methTwo());

            // following produces 
            // invalid D instance with NoMethodFoundError
            // unless I cast to C instead of iA
            if( ((IA) iimpl).methOne().equals( "one")) {
                cImplDesired = iimpl;
                System.out.println(iimpl.methOne());
            }
        }
    }
}
