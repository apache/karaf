package org.apache.karaf.main.lock;

import java.util.Properties;

public class LockFactory {
    /**
     * If a lock should be used before starting the runtime
     */
    public static final String PROPERTY_USE_LOCK = "karaf.lock";

    /**
     * The lock implementation
     */
    public static final String PROPERTY_LOCK_CLASS = "karaf.lock.class";

    public static final String PROPERTY_LOCK_CLASS_DEFAULT = SimpleFileLock.class.getName();
    
	public static Lock createLock(Properties props) {
		if (Boolean.parseBoolean(props.getProperty(PROPERTY_USE_LOCK, "true"))) {
			return new NoLock();
		}
		String clz = props.getProperty(PROPERTY_LOCK_CLASS, PROPERTY_LOCK_CLASS_DEFAULT);
		try {
			return (Lock) Class.forName(clz).getConstructor(Properties.class).newInstance(props);
		} catch (Exception e) {
			throw new RuntimeException("Exception instantiating lock class " + clz, e);
		}
	}
}
