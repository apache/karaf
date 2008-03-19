package org.apache.geronimo.gshell.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 18, 2008
 * Time: 4:35:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoCloseInputStreamTest extends TestCase {

    public void testStream() throws Exception {
        final InputStream in = new NoCloseInputStream(System.in);
        new Thread() {
            public void run() {
                try {
                    int c;
                    System.err.println("Reading from in...");
                    while ((c = in.read()) != -1) {
                        System.err.println("Read from in: " + c);
                    }
                    System.err.println("Exiting thread...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        Thread.sleep(2000);

        in.close();

        Thread.sleep(2000);

    }
}
