package org.apache.felix.ipojo.test.scenarios.service.dependency.comparator.component;

import java.util.Comparator;

import org.osgi.framework.ServiceReference;

public class GradeComparator implements Comparator {

    public int compare(Object arg0, Object arg1) {
        ServiceReference ref0 = null;
        ServiceReference ref1 = null;
        Integer grade0 = null;
        Integer grade1 = null;
        if (arg0 instanceof ServiceReference) {
            ref0 = (ServiceReference)  arg0;
            grade0 = (Integer) ref0.getProperty("grade");
        }
        if (arg1 instanceof ServiceReference) {
            ref1 = (ServiceReference) arg1;
            grade1 = (Integer) ref1.getProperty("grade");
        }
        
        if (ref0 != null && ref1 != null
                && grade0 != null && grade1 != null) {
            return grade1.compareTo(grade0); // Best grade first.
        } else {
            return 0; // Equals
        }
    }

}
