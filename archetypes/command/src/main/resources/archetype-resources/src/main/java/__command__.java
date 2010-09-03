#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
#set( $cmd =  $command.toLowerCase())

package ${package};

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;


/**
 * Displays the last log entries
 */
@Command(scope = "${scope}", name = "${cmd}", description = "${description}")
public class ${command} extends OsgiCommandSupport {

    protected Object doExecute() throws Exception {
         System.out.println("Executing command ${cmd}");
         return null;
    }
}
