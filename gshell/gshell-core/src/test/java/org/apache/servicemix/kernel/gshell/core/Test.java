package org.apache.servicemix.kernel.gshell.core;

import junit.framework.TestCase;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.application.ApplicationManager;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;

public class Test extends TestCase {

    public void test() throws Exception {

        System.setProperty("startLocalConsole", "true");

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "META-INF/spring/gshell.xml",
                               "META-INF/spring/gshell-vfs.xml",
                               "META-INF/spring/gshell-commands.xml",
                               "org/apache/servicemix/kernel/gshell/core/gshell-test.xml" });
        ApplicationManager appMgr = (ApplicationManager) context.getBean("applicationManager");
        assertNotNull(appMgr);
        Shell shell = appMgr.create();
        assertNotNull(shell);
        shell.execute("help");
    }
}
