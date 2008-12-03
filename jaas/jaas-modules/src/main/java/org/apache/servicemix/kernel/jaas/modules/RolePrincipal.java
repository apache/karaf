package org.apache.servicemix.kernel.jaas.modules;

import java.security.Principal;

public class RolePrincipal implements Principal {

    private final String name;

    public RolePrincipal(String name) {
        assert name != null;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePrincipal)) return false;

        RolePrincipal that = (RolePrincipal) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RolePrincipal[" + name + "]";
    }
}
