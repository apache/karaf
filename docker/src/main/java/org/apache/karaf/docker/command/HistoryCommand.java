/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.docker.command;

import org.apache.karaf.docker.ImageHistory;
import org.apache.karaf.docker.command.completers.ImagesRepoTagsCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.List;

@Command(scope = "docker", name = "history", description = "Show the history of an image")
@Service
public class HistoryCommand extends DockerCommandSupport {

    @Argument(index = 0, name = "image", description = "Name or ID of the image", required = true, multiValued = false)
    @Completion(ImagesRepoTagsCompleter.class)
    String image;

    @Override
    public Object execute() throws Exception {
        List<ImageHistory> histories = getDockerService().history(image, url);
        ShellTable table = new ShellTable();
        table.column("ID");
        table.column("Created");
        table.column("Created By");
        table.column("Tags");
        table.column("Size");
        for (ImageHistory history : histories) {
            table.addRow().addContent(history.getId(),
                    history.getCreated(),
                    history.getCreatedBy(),
                    history.getComment(),
                    history.getTags(),
                    history.getSize());
        }
        table.print(System.out);
        return null;
    }

}
