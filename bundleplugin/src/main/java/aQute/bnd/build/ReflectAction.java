package aQute.bnd.build;

import java.lang.reflect.*;

import aQute.bnd.service.action.*;

public class ReflectAction implements Action {
    String  what;
    
    public ReflectAction(String what) {
        this.what = what;
    }
    
    public void execute(Project project, String action) throws Exception {
        Method m = project.getClass().getMethod(what);
        m.invoke(project);
    }

    public String toString() {
        return "ra:" + what;
    }
}
