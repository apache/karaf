package org.apache.felix.bundlerepository.impl.wrapper;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Feb 25, 2010
 * Time: 11:47:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequirementWrapper implements org.osgi.service.obr.Requirement {

    final Requirement requirement;

    public RequirementWrapper(Requirement requirement) {
        this.requirement = requirement;
    }

    public String getName() {
        return requirement.getName();
    }

    public String getFilter() {
        return requirement.getFilter();
    }

    public boolean isMultiple() {
        return requirement.isMultiple();
    }

    public boolean isOptional() {
        return requirement.isOptional();
    }

    public boolean isExtend() {
        return requirement.isExtend();
    }

    public String getComment() {
        return requirement.getComment();
    }

    public boolean isSatisfied(org.osgi.service.obr.Capability capability) {
        return requirement.isSatisfied(Wrapper.unwrap(capability));
    }
}
