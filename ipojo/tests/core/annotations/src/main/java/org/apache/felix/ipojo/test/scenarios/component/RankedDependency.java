package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.test.scenarios.annotations.service.FooService;

@Component
public class RankedDependency {

    @Requires(comparator = MyComparator.class)
    public FooService fs;

    @Unbind(comparator = MyComparator.class)
    public void unbindBar() {
    }

    @Bind
    public void bindBar() {
    }

    @Unbind
    public void unbindBaz() {
    }

    @Bind(comparator = MyComparator.class)
    public void bindBaz() {
    }

    @Requires(id = "inv")
    public FooService fs2inv;

    @Bind(id = "inv", comparator = MyComparator.class)
    public void bindFS2Inv() {
    }

    @Unbind(id = "inv")
    public void unbindFS2Inv() {
    }

    @Unbind(comparator = MyComparator.class, id = "unbindonly")
    public void unbind() {
    }

    @Bind(comparator = MyComparator.class, id = "bindonly")
    public void bind() {
    }

}
