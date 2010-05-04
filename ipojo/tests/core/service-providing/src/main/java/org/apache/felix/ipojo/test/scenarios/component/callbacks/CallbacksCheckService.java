package org.apache.felix.ipojo.test.scenarios.component.callbacks;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.ps.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;

public class CallbacksCheckService implements FooService, CheckService {

	// 4 Counters
	int registered = 0;
	int unregistered = 0;
	int registered2 = 0;
	int unregistered2 = 0;

	// 4 Methods
	public void registered(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		registered++;
	}

	public void unregistered(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		unregistered++;
	}

	public void registered2(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		registered2++;
	}

	public void unregistered2(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		unregistered2++;
	}

    public boolean foo() {
        return true;
    }

    public Properties fooProps() {
        Properties props = new Properties();
        props.put("registered", new Integer(registered));
        props.put("registered2", new Integer(registered2));
        props.put("unregistered", new Integer(unregistered));
        props.put("unregistered2", new Integer(unregistered2));
        return props;
    }

    public boolean getBoolean() {
        return false;
    }

    public double getDouble() {
        return 0;
    }

    public int getInt() {
        return 0;
    }

    public long getLong() {
        return 0;
    }

    public Boolean getObject() {
        return null;
    }

    public boolean check() {
       return true;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("registered", new Integer(registered));
        props.put("registered2", new Integer(registered2));
        props.put("unregistered", new Integer(unregistered));
        props.put("unregistered2", new Integer(unregistered2));
        return props;
    }

}
