package org.apache.felix.karaf.shell.console.jline;

import java.util.List;

import jline.Completor;
import org.apache.felix.karaf.shell.console.Completer;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jul 6, 2009
 * Time: 10:26:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class CompleterAsCompletor implements Completor {

    private final Completer completer;

    public CompleterAsCompletor(Completer completer) {
        this.completer = completer;
    }

    public int complete(String buffer, int cursor, List candidates) {
        return completer.complete(buffer, cursor, candidates);
    }
}
