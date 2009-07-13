package aQute.bnd.build;

import aQute.lib.osgi.*;

@SuppressWarnings("unchecked")
public class ProjectBuilder extends Builder {
    Project project;

    public ProjectBuilder(Project project) {
        super(project);
        this.project = project;
    }

    public ProjectBuilder(ProjectBuilder builder) {
        super(builder);
        this.project = builder.project;
    }


    /** 
     * We put our project and our workspace on the macro path.
     */
    protected Object [] getMacroDomains() {
        return new Object[] {project, project.getWorkspace()};
    }

    public Builder getSubBuilder() throws Exception {
        return project.getBuilder(this);
    }

    public Project getProject() {
        return project;
    }
}
