/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.jmood.compendium;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.felix.jmood.AgentConstants;
import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.utils.OSGi2JMXCodec;
import org.apache.felix.jmood.utils.ObjectNames;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/**
 * User manager for the gateway. This mbean provides access to the user admin
 * functionality.
 * 
 * 
 */
public class UserManager extends NotificationBroadcasterSupport implements
        MBeanRegistration, UserManagerMBean {
    private AgentContext ac;

    private UserManager um;

    public UserManager(AgentContext ac) {
        this.ac = ac;
        this.um = this;
    }

    private static long sequenceNumber = 0;

    /**
     * Creates a role of the specified type, case insensitive, with the
     * specified name
     * 
     * @param name
     * @param type
     * @throws Exception
     */
    public void createRole(String name, String type) throws Exception {
        int t = -1;
        if (type.equalsIgnoreCase(AgentConstants.GROUP))
            t = Role.GROUP;
        else if (type.equalsIgnoreCase(AgentConstants.USER))
            t = Role.USER;
        else
            throw new Exception(
                    "Incorrect type name. Valid names: User | Group. Case Insensitive");
        try {
            ac.getUserAdmin().createRole(name, t);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. Could not create Role");
        }
    }

    public CompositeData getRole(String name) throws Exception {
        try {
            return OSGi2JMXCodec.encodeRole(ac.getUserAdmin().getRole(name));
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. Could not get Role");
            return null;
        }
    }

    public CompositeData getGroup(String groupname) {
        try {
            Role group = ac.getUserAdmin().getRole(groupname);
            if (group.getType() == Role.GROUP)
                return OSGi2JMXCodec.encodeGroup((Group) group);
            else
                return null;

        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        } catch (Exception e) {
            ac.error("unexpected exception", e);
            return null;
        }
    }

    public CompositeData getUser(String username) throws Exception {
        try {
            Role user = ac.getUserAdmin().getRole(username);
            if (user.getType() == Role.USER)
                return OSGi2JMXCodec.encodeUser((User) user);
            else
                return null;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }

    }

    public CompositeData getAuthorization(String user) {
        try {
            return OSGi2JMXCodec.encodeAuthorization(ac.getUserAdmin()
                    .getAuthorization((User) ac.getUserAdmin().getRole(user)));
        } catch (Exception e) {
            ac.error("unexpected exception", e);
            return null;

        }
    }

    public String[] getRoles(String filter) throws Exception {
        try {
            Role[] roles = ac.getUserAdmin().getRoles(filter);
            String[] result = new String[roles.length];
            for (int i = 0; i < roles.length; i++) {
                result[i] = roles[i].getName();
            }
            return result;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }

    }

    public String getUser(String key, String value) {
        try {
            return ac.getUserAdmin().getUser(key, value).getName();

        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }
    }

    public boolean removeRole(String name) {
        try {
            return ac.getUserAdmin().removeRole(name);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return false;
        }
    }

    public String[] getRoles() throws Exception {
        try {
            Role[] roles = ac.getUserAdmin().getRoles(null);
            String[] result = new String[roles.length];
            for (int i = 0; i < roles.length; i++) {
                result[i] = roles[i].getName();
            }
            return result;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }
    }

    public String[] getGroups() throws Exception {
        try {
            Role[] roles = ac.getUserAdmin().getRoles(null);
            Vector tmp = new Vector();
            int j = 0;
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].getType() == Role.GROUP) {
                    j++;
                    tmp.add(roles[i].getName());
                }
            }
            if (j == 0)
                return new String[0];
            else {
                String[] result = new String[j];
                tmp.copyInto(result);
                return result;
            }
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }

    }

    public String[] getUsers() throws Exception {
        try {
            Role[] roles = ac.getUserAdmin().getRoles(null);
            Vector tmp = new Vector();
            int j = 0;
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].getType() == Role.USER) {
                    j++;
                    tmp.add(roles[i].getName());
                }
            }
            if (j == 0)
                return new String[0];
            else {
                String[] result = new String[j];
                tmp.copyInto(result);
                return result;
            }
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }

    }

    public String[] getMembers(String groupname) {
        try {
            Group group = (Group) ac.getUserAdmin().getRole(groupname);
            Role[] members = group.getMembers();
            if (members == null)
                return null;
            String[] names = new String[members.length];
            for (int i = 0; i < members.length; i++) {
                names[i] = members[i].getName();
            }
            return names;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }
    }

    public String[] getRequiredMembers(String groupname) {
        try {
            Group group = (Group) ac.getUserAdmin().getRole(groupname);
            Role[] members = group.getRequiredMembers();
            if (members == null)
                return null;
            String[] names = new String[members.length];
            for (int i = 0; i < members.length; i++) {
                names[i] = members[i].getName();
            }
            return names;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }

    }

    public boolean addMember(String groupname, String rolename) {
        try {
            Role group = ac.getUserAdmin().getRole(groupname);
            Role role = ac.getUserAdmin().getRole(rolename);
            if (!(group.getType() == Role.GROUP))
                return false;
            return ((Group) group).addMember(role);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return false;
        }
    }

    public boolean addRequiredMember(String groupname, String rolename) {
        try {
            Role group = ac.getUserAdmin().getRole(groupname);
            Role role = ac.getUserAdmin().getRole(rolename);
            if (!(group.getType() == Role.GROUP))
                return false;
            return ((Group) group).addRequiredMember(role);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return false;
        }

    }

    public boolean removeMember(String groupname, String rolename) {
        try {
            Role group = ac.getUserAdmin().getRole(groupname);
            Role role = ac.getUserAdmin().getRole(rolename);
            if (!(group.getType() == Role.GROUP))
                return false;
            return ((Group) group).removeMember(role);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return false;
        }
    }

    public String[] getImpliedRoles(String username) throws Exception {
        try {
            Role role = ac.getUserAdmin().getRole(username);
            if (role.getType() == Role.USER && role instanceof User) {
                return ac.getUserAdmin().getAuthorization((User) role)
                        .getRoles();
            } else
                return null;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }
    }

    public void addProperty(String key, Object value, String rolename)
            throws IllegalArgumentException {
        try {
            if (value instanceof Byte[]) {
                Byte[] ByteValue = (Byte[]) value;
                byte[] primitive = new byte[ByteValue.length];
                for (int i = 0; i < ByteValue.length; i++)
                    primitive[i] = ByteValue[i].byteValue();
                value = primitive;
            } else if (!(value instanceof String) && !(value instanceof byte[]))
                throw new IllegalArgumentException(
                        "Credentials can only be byte[] or String");
            Role role = ac.getUserAdmin().getRole(rolename);
            role.getProperties().put(key, value);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
        }

    }

    public void removeProperty(String key, String rolename) {
        try {
            Role role = ac.getUserAdmin().getRole(rolename);
            role.getProperties().remove(key);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
        }

    }

    public void addCredential(String key, Object value, String username)
            throws IllegalArgumentException {
        try {
            if (value instanceof Byte[]) {
                Byte[] ByteValue = (Byte[]) value;
                byte[] primitive = new byte[ByteValue.length];
                for (int i = 0; i < ByteValue.length; i++)
                    primitive[i] = ByteValue[i].byteValue();
                value = primitive;
            } else if (!(value instanceof String) && !(value instanceof byte[]))
                throw new IllegalArgumentException(
                        "Credentials can only be byte[] or String");
            User user = (User) ac.getUserAdmin().getRole(username);
            user.getCredentials().put(key, value);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
        }

    }

    public void removeCredential(String key, String username) {
        try {
            User user = (User) ac.getUserAdmin().getRole(username);
            user.getCredentials().remove(key);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
        } catch (Exception e) {
            ac.error("unexpected exception", e);
        }
    }

    public Hashtable getProperties(String rolename) {
        try {
            Role role = ac.getUserAdmin().getRole(rolename);
            Dictionary dic = role.getProperties();
            Hashtable props = new Hashtable();
            Enumeration keys = dic.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                props.put(key, dic.get(key));
            }
            return props;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }
    }

    public Hashtable getCredentials(String username) {
        try {
            User user = (User) ac.getUserAdmin().getRole(username);
            Dictionary dic = user.getCredentials();
            Hashtable credentials = new Hashtable();
            Enumeration keys = dic.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                credentials.put(key, dic.get(key));
            }
            return credentials;
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
            return null;
        }

    }

    // /////////////////////MBEANREGISTRATION
    // METHODS///////////////////////////////////////////////////
    public void postDeregister() {
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    /**
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer,
     *      javax.management.ObjectName)
     * @param server
     * @param name
     * @return
     * @throws java.lang.Exception
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        ac.getBundleContext().registerService(
                UserAdminListener.class.getName(), new UserAdminListener() {
                    public void roleChanged(UserAdminEvent e) {
                        um.notifyUserAdminEvent(e);
                    }
                }, null);
        try {
            ac.getUserAdmin().removeRole(AgentConstants.USER);
        } catch (NullPointerException npe) {
            ac.debug("UserAdmin not available. ");
        }
        return name;

    }

    // ///////PRIVATE METHODS//////////////////////////
    private void notifyUserAdminEvent(UserAdminEvent event) {
        String typedesc = null;
        switch (event.getType()) {
        case UserAdminEvent.ROLE_CREATED:
            typedesc = "created";
            break;
        case UserAdminEvent.ROLE_CHANGED:
            typedesc = "changed";
            break;
        case UserAdminEvent.ROLE_REMOVED:
            typedesc = "removed";
            break;
        }
        try {
            ObjectName source = new ObjectName(ObjectNames.UA_SERVICE);
            String message = "User Admin event: Role "
                    + event.getRole().getName() + typedesc;
            Notification notification = new Notification(
                    AgentConstants.USER_ADMIN_NOTIFICATION_TYPE, source,
                    sequenceNumber++, message);
            CompositeData userData = OSGi2JMXCodec.encodeUserAdminEvent(event);
            notification.setUserData(userData);
            sendNotification(notification);
        } catch (Exception e) {
            ac.error("Unexpected exception", e);
        }
    }
}
