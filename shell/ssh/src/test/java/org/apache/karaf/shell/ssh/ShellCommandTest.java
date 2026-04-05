/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Test;

public class ShellCommandTest {

    @Test
    public void testDestroyInterruptsBlockingCommand() throws Exception {
        // Simulate a blocking command (like log:tail) that waits until interrupted
        CountDownLatch commandStarted = new CountDownLatch(1);
        CountDownLatch commandFinished = new CountDownLatch(1);

        Session session = EasyMock.createMock(Session.class);
        session.put(EasyMock.anyString(), EasyMock.anyObject());
        EasyMock.expectLastCall().anyTimes();

        // The execute() call blocks until the thread is interrupted
        EasyMock.expect(session.execute(EasyMock.anyString())).andAnswer((IAnswer<Object>) () -> {
            commandStarted.countDown();
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                // Expected when destroy() is called
            }
            commandFinished.countDown();
            return null;
        });
        session.close();
        EasyMock.expectLastCall().once();

        SessionFactory sessionFactory = EasyMock.createMock(SessionFactory.class);
        EasyMock.expect(sessionFactory.create(
                EasyMock.anyObject(InputStream.class),
                EasyMock.anyObject(PrintStream.class),
                EasyMock.anyObject(PrintStream.class)))
                .andReturn(session);

        Environment env = EasyMock.createMock(Environment.class);
        EasyMock.expect(env.getEnv()).andReturn(Collections.emptyMap());

        ExitCallback exitCallback = EasyMock.createMock(ExitCallback.class);
        exitCallback.onExit(EasyMock.anyInt());
        EasyMock.expectLastCall().once();

        ChannelSession channelSession = EasyMock.createMock(ChannelSession.class);
        ServerSession serverSession = EasyMock.createMock(ServerSession.class);
        EasyMock.expect(channelSession.getServerSession()).andReturn(serverSession);
        EasyMock.expect(serverSession.getAttribute(KarafJaasAuthenticator.SUBJECT_ATTRIBUTE_KEY)).andReturn(null);
        EasyMock.expect(serverSession.close(false)).andReturn(null);

        EasyMock.replay(session, sessionFactory, env, exitCallback, channelSession, serverSession);

        ShellCommand shellCommand = new ShellCommand(sessionFactory, "log:tail");
        shellCommand.setInputStream(new ByteArrayInputStream(new byte[0]));
        shellCommand.setOutputStream(new ByteArrayOutputStream());
        shellCommand.setErrorStream(new ByteArrayOutputStream());
        shellCommand.setExitCallback(exitCallback);

        // Start the command (runs in a separate thread)
        shellCommand.start(channelSession, env);

        // Wait for the blocking command to start
        Assert.assertTrue("Command should have started", commandStarted.await(5, TimeUnit.SECONDS));

        // Simulate SSH disconnect by calling destroy
        shellCommand.destroy(channelSession);

        // The command thread should finish within a reasonable time
        Assert.assertTrue("Command thread should have been interrupted and finished",
                commandFinished.await(5, TimeUnit.SECONDS));

        EasyMock.verify(session);
    }
}
