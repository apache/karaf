package org.apache.karaf.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.karaf.main.lock.Lock;
import org.apache.karaf.main.lock.LockFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.startlevel.StartLevel;

public class LifecycleManager extends Thread {
	Logger LOG = Logger.getLogger(this.getClass().getName());
	
    public static final String KARAF_SHUTDOWN_TIMEOUT = "karaf.shutdown.timeout";

    public static final String KARAF_SHUTDOWN_PORT = "karaf.shutdown.port";

    public static final String KARAF_SHUTDOWN_HOST = "karaf.shutdown.host";

    public static final String KARAF_SHUTDOWN_PORT_FILE = "karaf.shutdown.port.file";

    public static final String KARAF_SHUTDOWN_COMMAND = "karaf.shutdown.command";

    public static final String KARAF_SHUTDOWN_PID_FILE = "karaf.shutdown.pid.file";

    public static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

    public static final String PROPERTY_LOCK_DELAY = "karaf.lock.delay";
    
    public static final String PROPERTY_LOCK_LEVEL = "karaf.lock.level";

	private Properties props;
	private Framework framework;
	private int shutdownTimeout = 5 * 60 * 1000;
    private ShutdownCallback shutdownCallback;
    private boolean exiting = false;
    private Lock lock;
    private int defaultStartLevel = 100;
    private int lockStartLevel = 1;
    private int lockDelay = 1000;

    
    public LifecycleManager(Properties props, Framework framework) {
    	this.props = props;
    	this.framework = framework;
    	this.shutdownTimeout = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_TIMEOUT, Integer.toString(shutdownTimeout)));
    	this.defaultStartLevel = Integer.parseInt(props.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));
        this.lockStartLevel = Integer.parseInt(props.getProperty(PROPERTY_LOCK_LEVEL, Integer.toString(lockStartLevel)));
        this.lockDelay = Integer.parseInt(props.getProperty(PROPERTY_LOCK_DELAY, Integer.toString(lockDelay)));
        props.setProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(lockStartLevel));
    }

    public void setShutdownCallback(ShutdownCallback shutdownCallback) {
        this.shutdownCallback = shutdownCallback;
    }
    
    protected void setupShutdown() {
    	String pidFile = props.getProperty(KARAF_SHUTDOWN_PID_FILE);
        InstanceInfoManager.writePid(pidFile);
        try {
            int port = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_PORT, "0"));
            String host = props.getProperty(KARAF_SHUTDOWN_HOST, "localhost");
            String portFile = props.getProperty(KARAF_SHUTDOWN_PORT_FILE);
            final String shutdown = props.getProperty(KARAF_SHUTDOWN_COMMAND, DEFAULT_SHUTDOWN_COMMAND);
            if (port >= 0) {
                ServerSocket shutdownSocket = new ServerSocket(port, 1, InetAddress.getByName(host));
                if (port == 0) {
                    port = shutdownSocket.getLocalPort();
                }
                if (portFile != null) {
                    Writer w = new OutputStreamWriter(new FileOutputStream(portFile));
                    w.write(Integer.toString(port));
                    w.close();
                }
                Thread thread = new ShutdownSocketThread(shutdown, shutdownSocket, framework);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
	public boolean destroyKaraf() throws Exception {
        if (framework == null) {
            return true;
        }
        try {
            int step = 5000;

            // Notify the callback asap
            if (shutdownCallback != null) {
                shutdownCallback.waitingForShutdown(step);
            }

            // Stop the framework in case it's still active
            exiting = true;
            if (framework.getState() == Bundle.ACTIVE || framework.getState() == Bundle.STARTING) {
                new Thread() {
                    public void run() {
                        try {
                            framework.stop();
                        } catch (BundleException e) {
                            System.err.println("Error stopping karaf: " + e.getMessage());
                        }
                    }
                }.start();
            }

            int timeout = shutdownTimeout;
            if (shutdownTimeout <= 0) {
                timeout = Integer.MAX_VALUE;
            }
            while (timeout > 0) {
                timeout -= step;
                if (shutdownCallback != null) {
                    shutdownCallback.waitingForShutdown(step * 2);
                }
                FrameworkEvent event = framework.waitForStop(step);
                if (event.getType() != FrameworkEvent.WAIT_TIMEDOUT) {
                    return true;
                }
            }
            return false;
        } finally {
        	if (lock != null) {
                lock.release();
            }
        }
    }

    protected void setStartLevel(int level) throws Exception {
        BundleContext ctx = framework.getBundleContext();
        ServiceReference[] refs = ctx.getServiceReferences(StartLevel.class.getName(), null);
        StartLevel sl = (StartLevel) ctx.getService(refs[0]);
        sl.setStartLevel(level);
    }
	
	public void run() {
	    try {
	    	lock = LockFactory.createLock(props);
	        boolean lockLogged = false;
			setStartLevel(lockStartLevel);
			while (!exiting) {
			    if (lock.lock()) {
			        if (lockLogged) {
			            LOG.info("Lock acquired.");
			        }
			        setupShutdown();
			        setStartLevel(defaultStartLevel);
			        while (lock.isAlive())  {
			        	Thread.sleep(lockDelay);
			        }
			        if (framework.getState() == Bundle.ACTIVE && !exiting) {
			            LOG.info("Lost the lock, stopping this instance ...");
			            setStartLevel(lockStartLevel);
			        }
			    } else if (!lockLogged) {
			        LOG.info("Waiting for the lock ...");
			        lockLogged = true;
			    }
			    Thread.sleep(lockDelay);
			}
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
    public void awaitShutdown() throws Exception {
        if (framework == null) {
            return;
        }
        while (true) {
            FrameworkEvent event = framework.waitForStop(0);
            if (event.getType() != FrameworkEvent.STOPPED_UPDATE) {
                return;
            }
        }
    }
	
	/**
	 * Shuts down the local instance
	 * @param props
	 * @throws IOException
	 */
	public static void shutDown(Properties props) throws IOException {
		int port = Integer.parseInt(props.getProperty(KARAF_SHUTDOWN_PORT, "0"));
		String host = props.getProperty(KARAF_SHUTDOWN_HOST, "localhost");
		String portFile = props.getProperty(KARAF_SHUTDOWN_PORT_FILE);
		String shutdown = props.getProperty(KARAF_SHUTDOWN_COMMAND, DEFAULT_SHUTDOWN_COMMAND);
		if (port == 0 && portFile != null) {
			BufferedReader r = new BufferedReader(new InputStreamReader(
					new FileInputStream(portFile)));
			String portStr = r.readLine();
			port = Integer.parseInt(portStr);
			r.close();
		}
		if (port > 0) {
			Socket s = new Socket(host, port);
			s.getOutputStream().write(shutdown.getBytes());
			s.close();
		} else {
			System.err.println("Unable to find port...");
		}
	}
}
