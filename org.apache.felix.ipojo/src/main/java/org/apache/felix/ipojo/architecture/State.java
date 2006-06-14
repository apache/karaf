/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.ipojo.architecture;

/**
 * @author Clement Escoffier
 *
 */
public class State {

    /**
     * Return the String corresponding to a component state.
     * @param state : the state in int
     * @return : the string of the state (Stopped, Unresolved, Resolved) or "Unknow" if state is not revelant
     */
    public static String printComponentState(int state) {
        switch(state) {
        case(0) :
            return "STOPPED";
        case(1) :
            return "INVALID";
        case(2) :
            return  "VALID";
        default :
            return "UNKNOW";
        }
    }

    /**
     * Return the String corresponding to a dependency state.
     * @param state : the state in int
     * @return : the string of the state (Stopped, Valid, Invalid) or "Unknow" if state is not revelant
     */
    public static String printDependencyState(int state) {
        switch(state) {
        case(0) :
            return "STOPPED";
        case(1) :
            return "RESOLVED";
        case(2) :
            return  "UNRESOLVED";
        default :
            return "UNKNOW";
        }
    }

    /**
     * Return the String corresponding to a provided service state.
     * @param state : the state in int
     * @return : the string of the state (Unregistred, Registredu) or "Unknow" if state is not revelant
     */
    public static String printProvidedServiceState(int state) {
        switch(state) {
        case(0) :
            return "UNREGISTRED";
        case(1) :
            return "REGISTRED";
        default :
            return "UNKNOW";
        }
    }

}
