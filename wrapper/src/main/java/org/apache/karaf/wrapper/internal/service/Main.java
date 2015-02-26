/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.wrapper.internal.service;

import org.apache.karaf.main.ShutdownCallback;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * Java Service Wrapper Main class
 */
public class Main extends Thread implements WrapperListener, ShutdownCallback {

    private org.apache.karaf.main.Main main;
    private volatile boolean destroying;

    /*---------------------------------------------------------------
     * Constructors
     *-------------------------------------------------------------*/
    private Main() {
    }

    /*---------------------------------------------------------------
     * WrapperListener Methods
     *-------------------------------------------------------------*/

    /**
     * The start method is called when the WrapperManager is signaled by the
     * native Wrapper code that it can start its application.  This
     * method call is expected to return, so a new thread should be launched
     * if necessary.
     *
     * @param args List of arguments used to initialize the application.
     * @return Any error code if the application should exit on completion
     *         of the start method.  If there were no problems then this
     *         method should return null.
     */
    public Integer start(String[] args) {
        main = new org.apache.karaf.main.Main(args);
        try {
            main.launch();
            main.setShutdownCallback(this);
            start();
            return null;
        } catch (Throwable ex) {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
            return -1;
        }
    }

    public void run() {
        try {
            main.awaitShutdown();
            if (!destroying) {
                WrapperManager.stop(main.getExitCode());
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Called when the application is shutting down.  The Wrapper assumes that
     * this method will return fairly quickly.  If the shutdown code code
     * could potentially take a long time, then WrapperManager.signalStopping()
     * should be called to extend the timeout period.  If for some reason,
     * the stop method can not return, then it must call
     * WrapperManager.stopped() to avoid warning messages from the Wrapper.
     *
     * @param exitCode The suggested exit code that will be returned to the OS
     *                 when the JVM exits.
     * @return The exit code to actually return to the OS.  In most cases, this
     *         should just be the value of exitCode, however the user code has
     *         the option of changing the exit code if there are any problems
     *         during shutdown.
     */
    public int stop(int exitCode) {
        try {
            destroying = true;
            if (!main.destroy()) {
                System.err.println("Timeout waiting for Karaf to shutdown");
                return -3;
            }
        } catch (Throwable ex) {
            System.err.println("Error occured shutting down framework: " + ex);
            ex.printStackTrace();
            return -2;
        }

        return main.getExitCode();
    }

    /**
     * Call-back method is called by the @{link org.apache.karaf.main.Main} for Signaling
     * that the stopping process is in progress and the wrapper doesn't kill the JVM.
     */
    public void waitingForShutdown(int delay) {
        WrapperManager.signalStopping(delay);
    }

    /**
     * Called whenever the native Wrapper code traps a system control signal
     * against the Java process.  It is up to the callback to take any actions
     * necessary.  Possible values are: WrapperManager.WRAPPER_CTRL_C_EVENT,
     * WRAPPER_CTRL_CLOSE_EVENT, WRAPPER_CTRL_LOGOFF_EVENT, or
     * WRAPPER_CTRL_SHUTDOWN_EVENT
     *
     * @param event The system control signal.
     */
    public void controlEvent(int event) {
        if ((event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT)
                && (WrapperManager.isLaunchedAsService())) {
            // Ignore
        } else {
            WrapperManager.stop(0);
            // Will not get here.
        }
    }

    /*---------------------------------------------------------------
     * Main Method
     *-------------------------------------------------------------*/
    public static void main(String[] args) {
        // Start the application.  If the JVM was launched from the native
        //  Wrapper then the application will wait for the native Wrapper to
        //  call the application's start method.  Otherwise the start method
        //  will be called immediately.
        WrapperManager.start(new Main(), args);
    }

}