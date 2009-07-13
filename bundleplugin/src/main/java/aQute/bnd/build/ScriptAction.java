package aQute.bnd.build;

import aQute.bnd.service.action.*;

public class ScriptAction implements Action {
    final String script;
    final String type;
    
    public ScriptAction(String type, String script) {
        this.script = script;
        this.type = type;
    }

    public void execute(Project project, String action) throws Exception {
        project.script(type, script);
    }

}
