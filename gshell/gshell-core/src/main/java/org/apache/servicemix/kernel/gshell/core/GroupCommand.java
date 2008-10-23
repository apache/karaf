package org.apache.servicemix.kernel.gshell.core;

import org.apache.commons.vfs.FileObject;
import org.apache.geronimo.gshell.command.CommandAction;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.registry.CommandResolver;

public class GroupCommand extends org.apache.geronimo.gshell.wisdom.command.GroupCommand {

    public GroupCommand(final FileObject file) {
        super(file);
        setAction(new GroupCommandAction());
    }

    private class GroupCommandAction
        implements CommandAction
    {
        public Object execute(final CommandContext context) throws Exception {
            assert context != null;

            FileObject file = getFile();

            log.debug("Changing to group: {}", file);

            context.getVariables().parent().set(CommandResolver.GROUP, file);

            return Result.SUCCESS;
        }
    }

}
