package org.apache.geronimo.gshell.spring;

import org.apache.geronimo.gshell.ExitNotification;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * Exit the current shell.
 *
 * @version $Rev: 593392 $ $Date: 2007-11-09 03:14:15 +0100 (Fri, 09 Nov 2007) $
 */
@CommandComponent(id="gshell-builtins:exit", description="Exit the (sub)shell")
public class ExitCommand
    extends OsgiCommandSupport
{
    @Argument(description="System exit code")
    private int exitCode = 0;

    protected Object doExecute() throws Exception {
        if (context.getVariables().get(LayoutManager.CURRENT_NODE) != null)
        {
            log.info("Exiting subshell");
            Variables v = context.getVariables();
            while (v != null && v.get(LayoutManager.CURRENT_NODE) != null) {
                v.unset(LayoutManager.CURRENT_NODE);
                v = v.parent();
            }
            return SUCCESS;
        }
        else
        {
            log.info("Exiting w/code: {}", exitCode);

            //
            // DO NOT Call System.exit() !!!
            //

            throw new ExitNotification(exitCode);
        }
    }
}
