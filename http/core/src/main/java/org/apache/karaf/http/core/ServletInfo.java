package org.apache.karaf.http.core;

import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;

public class ServletInfo {
    private String name;
    private Bundle bundle;
    private String className;
    private String alias;
    private int state;
    private String[] urls;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Bundle getBundle() {
        return bundle;
    }
    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public int getState() {
        return state;
    }
    public void setState(int state) {
        this.state = state;
    }
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }
    public String[] getUrls() {
        return urls;
    }
    public void setUrls(String[] urls) {
        this.urls = urls;
    }
    
    public String getStateString() {
        switch (state) {
            case WebEvent.DEPLOYING:
                return "Deploying  ";
            case WebEvent.DEPLOYED:
                return "Deployed   ";
            case WebEvent.UNDEPLOYING:
                return "Undeploying";
            case WebEvent.UNDEPLOYED:
                return "Undeployed ";
            case WebEvent.FAILED:
                return "Failed     ";
            case WebEvent.WAITING:
                return "Waiting    ";
            default:
                return "Failed     ";
        }
    }
}
