/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.commands.basic;

import java.util.List;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.CommandWithAction;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;

@Deprecated
public abstract class AbstractCommand implements Function, CommandWithAction {

    public Object execute(CommandSession session, List<Object> arguments) throws Exception {
        Action action = createNewAction();
        try {
            if (getPreparator().prepare(action, session, arguments)) {
                return action.execute(session);
            } else {
                return null;
            }
        } finally {
            releaseAction(action);
        }
    }

    public Class<? extends Action> getActionClass() {
        return createNewAction().getClass();
    }

    public abstract Action createNewAction();

    /**
     * Release the used Action.
     * This method has to be overridden for pool based Actions.
     * @param action Action that was executed
     * @throws Exception if something went wrong during the Action release
     */
    public void releaseAction(Action action) throws Exception {
        // Do nothing by default (stateful)
    }

    protected ActionPreparator getPreparator() throws Exception {
        return new DefaultActionPreparator();
    }

}
