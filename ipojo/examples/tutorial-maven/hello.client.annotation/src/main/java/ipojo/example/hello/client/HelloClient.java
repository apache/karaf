/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package ipojo.example.hello.client;

import ipojo.example.hello.Hello;

import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

/**
 * A simple Hello service client. This client use annotation instead of XML metadata.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(name = "AnnotatedHelloClient", architecture = true)
public class HelloClient implements Runnable {
    
    /** Delay between two invocations. */
    private static final int DELAY = 10000;

    /**
     * Hello services. Injected by the container.
     */
    @Requires(specification="ipojo.example.hello.Hello", optional=true, defaultimplementation=MyDummyHello.class)
    private Hello m_hello;

    /**
     *  End flag.
     */
    private boolean m_end;

    /**
     * Run method.
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (!m_end) {
            try {
                invokeHelloServices();
                Thread.sleep(DELAY);
            } catch (InterruptedException ie) {
                /* will recheck end */
            }
        }
    }

    /**
     * Invoke hello services.
     */
    public void invokeHelloServices() {
        System.out.println(m_hello);
        Hello hello = m_hello;
        //for (Hello hello : m_hello) {
            System.out.println(hello.sayHello("Clement ") + hello);
        //}
    }

    /**
     * Starting.
     */
    @Validate
    public void starting() {
        Thread thread = new Thread(this);
        m_end = false;
        thread.start();
    }

    /**
     * Stopping.
     */
    @Invalidate
    public void stopping() {
        m_end = true;
    }
}
