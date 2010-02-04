package org.apache.felix.sigil.gogo.junit;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

public class PrintListener implements TestListener {

    public PrintListener() {
    }

    public void startTest(Test test) {
        System.out.println( "Start " + test );
        System.out.flush();
    }

    public void endTest(Test test) {
        System.out.println( "End " + test );
        System.out.flush();
    }


    public void addError(Test test, Throwable t) {
        System.out.println( "Error " + test );
        t.printStackTrace(System.out);
        System.out.flush();
    }

    public void addFailure(Test test, AssertionFailedError error) {
        System.out.println( "Failure " + test + ": " + error.getMessage() );
        System.out.flush();
    }
}
