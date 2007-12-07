package org.apache.geronimo.gshell.spring;

import java.util.List;

import org.apache.geronimo.gshell.ansi.Code;
import org.apache.geronimo.gshell.ansi.Renderer;
import org.apache.geronimo.gshell.branding.Branding;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.annotation.Requirement;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.layout.model.AliasNode;
import org.apache.geronimo.gshell.layout.model.CommandNode;
import org.apache.geronimo.gshell.layout.model.GroupNode;
import org.apache.geronimo.gshell.layout.model.Node;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.registry.NotRegisteredException;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.codehaus.plexus.util.StringUtils;

/**
 * Display help
 *
 * @version $Rev: 596570 $ $Date: 2007-11-20 09:47:27 +0100 (Tue, 20 Nov 2007) $
 */
@CommandComponent(id="gshell-builtins:help", description="Show command help")
public class HelpCommand
    extends OsgiCommandSupport
{
    @Requirement
    private CommandRegistry commandRegistry;

    @Requirement
    private LayoutManager layoutManager;

    @Requirement
    private Branding branding;

    @Argument(required = false, multiValued = true)
    private List<String> path;

    private Renderer renderer = new Renderer();

    public HelpCommand(CommandRegistry commandRegistry, LayoutManager layoutManager, Branding branding) {
        this.commandRegistry = commandRegistry;
        this.layoutManager = layoutManager;
        this.branding = branding;
    }

    protected OsgiCommandSupport createCommand() throws Exception {
        return new HelpCommand(commandRegistry, layoutManager, branding);
    }

    protected Object doExecute() throws Exception {
        io.out.println();

        GroupNode gn = layoutManager.getLayout();
        if (context.getVariables().get(LayoutManager.CURRENT_NODE) != null) {
            gn = (GroupNode) context.getVariables().get(LayoutManager.CURRENT_NODE);
        }
        CommandNode cn = null;
        if (path != null) {
            for (String p : path) {
                if (cn != null) {
                    io.err.println("Unexpected path '" + p + "'");
                    return FAILURE;
                }
                Node n = gn.find(p);
                if (n == null) {
                    io.err.println("Path '" + p + "' not found!");
                    return FAILURE;
                } else if (n instanceof GroupNode) {
                    gn = (GroupNode) n;
                } else if (n instanceof CommandNode) {
                    cn = (CommandNode) n;
                } else {
                    throw new IllegalStateException("Unsupported node type " + n.getParent().getName());
                }
            }
        }

        if (cn == null) {
            // TODO: take into account the sub shell
            if (path == null || path.isEmpty()) {
                io.out.print(branding.getAbout());
                io.out.println();
                io.out.println("Available commands:");
            } else {
                io.out.println("Available commands in " + path);
            }
            displayGroupCommands(gn);
        }
        else {
            displayCommandHelp(cn.getId());
        }

        return SUCCESS;
    }

    private void displayGroupCommands(final GroupNode group) throws Exception {
        int maxNameLen = 20; // FIXME: Figure this out dynamically

        // First display command/aliases nodes
        for (Node child : group.nodes()) {
            if (child instanceof CommandNode) {
                try {
                    CommandNode node = (CommandNode) child;
                    String name = StringUtils.rightPad(node.getName(), maxNameLen);

                    Command command = commandRegistry.lookup(node.getId());
                    String desc = command.getDescription();

                    io.out.print("  ");
                    io.out.print(renderer.render(Renderer.encode(name, Code.BOLD)));

                    if (desc != null) {
                        io.out.print("  ");
                        io.out.println(desc);
                    }
                    else {
                        io.out.println();
                    }
                } catch (NotRegisteredException e) {
                    // Ignore those exceptions (command will not be displayed)
                }
            }
            else if (child instanceof AliasNode) {
                AliasNode node = (AliasNode) child;
                String name = StringUtils.rightPad(node.getName(), maxNameLen);

                io.out.print("  ");
                io.out.print(renderer.render(Renderer.encode(name, Code.BOLD)));
                io.out.print("  ");

                io.out.print("Alias to: ");
                io.out.println(renderer.render(Renderer.encode(node.getCommand(), Code.BOLD)));
            }
        }

        io.out.println();

        // Then groups
        for (Node child : group.nodes()) {
            if (child instanceof GroupNode) {
                GroupNode node = (GroupNode) child;

                io.out.print("  ");
                io.out.println(renderer.render(Renderer.encode(node.getPath(), Code.BOLD)));

                io.out.println();
                //displayGroupCommands(node);
                //io.out.println();
            }
        }
    }

    private void displayCommandHelp(final String path) throws Exception {
        assert path != null;

        Command cmd = commandRegistry.lookup(path);

        if (cmd == null) {
            io.out.println("Command " + Renderer.encode(path, Code.BOLD) + " not found.");
            io.out.println("Try " + Renderer.encode("help", Code.BOLD) + " for a list of available commands.");
        }
        else {
            io.out.println("Command " + Renderer.encode(path, Code.BOLD));
            io.out.println("   " + cmd.getDescription());
        }

        io.out.println();
    }
}

