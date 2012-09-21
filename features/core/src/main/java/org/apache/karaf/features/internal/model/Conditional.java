package org.apache.karaf.features.internal.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.karaf.features.Feature;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditional", propOrder = {
        "condition",
        "config",
        "configfile",
        "feature",
        "bundle"
})
public class Conditional extends Content implements org.apache.karaf.features.Conditional {

    @XmlElement(name = "condition")
    protected List<Dependency> condition;

    public List<Dependency> getCondition() {
        if (condition == null) {
            this.condition = new ArrayList<Dependency>();
        }
        return condition;
    }

    @Override
    public Feature asFeature(String name, String version) {
        String conditionName = name + "-condition-" + getConditionId().replaceAll("[^A-Za-z0-9 ]", "_");
        org.apache.karaf.features.internal.model.Feature f = new org.apache.karaf.features.internal.model.Feature(conditionName, version);
        f.getBundle().addAll(getBundle());
        f.getConfig().addAll(getConfig());
        f.getConfigfile().addAll(getConfigfile());
        f.getFeature().addAll(getFeature());
        return f;
    }

    private String getConditionId() {
        StringBuffer sb = new StringBuffer();
        Iterator<Dependency> di = getCondition().iterator();
        while (di.hasNext()) {
            Dependency dependency = di.next();
            sb.append(dependency.getName() + "_" + dependency.getVersion());
            if (di.hasNext()) {
                sb.append("_");
            }
        }
        return sb.toString();
    }
}
