/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests.examples;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.itests.BaseTest;
import org.apache.karaf.itests.util.SimpleSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class GraphQLExampleTest extends BaseTest {

    private void setUp() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-graphql-example-features/" + System.getProperty("karaf.version") + "/xml");
        installAndAssertFeature("karaf-graphql-example");
    }

    @Test
    public void testServlet() throws Exception {
        setUp();

        String getBooksQuery = "{ books { name id } }";
        String booksRequestResult = sendGetRequest(getBooksQuery);

        assertContains("\"name\":\"Apache Karaf Cookbook\"", booksRequestResult);
        assertContains("\"id\":\"1\"", booksRequestResult);
        assertContains("\"name\":\"Effective Java\"", booksRequestResult);
        assertContains("\"id\":\"2\"", booksRequestResult);

        String addBookQuery = "mutation { addBook(name:\"Lord of the Rings\" pageCount:100) { id name } }";
        String addBookRequestResult = sendPostRequest(addBookQuery);
        assertContains("id", addBookRequestResult);
        assertContains("Lord of the Rings", addBookRequestResult);

        String getBooksByIdQuery = "{ bookById(id:4) { name } }";
        String getBookByIdRequestResult = sendGetRequest(getBooksByIdQuery);
        assertContains("Lord of the Rings", getBookByIdRequestResult);
    }

    @Test
    public void testCommand() throws Exception {
        setUp();
        String getBooksQuery = "\"{ books { name id } }\"";
        String output = executeCommand("graphql:query " + getBooksQuery);
        System.out.println(output);
    }

    @Test
    public void testWebSocket() throws Exception {
        setUp();

        WebSocketClient client = new WebSocketClient();
        SimpleSocket socket = new SimpleSocket();
        client.start();
        URI uri = new URI("ws://localhost:" + getHttpPort() + "/graphql-websocket");
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        client.connect(socket, uri, request);

        sendPostRequest("mutation { addBook(name:\"Lord of the Rings\" pageCount:100) { id name } }");

        socket.awaitClose(10, TimeUnit.SECONDS);

        assertTrue(socket.messages.size() > 0);

        assertContains("Lord of the Rings", socket.messages.get(0));

        client.stop();
    }

    private String sendGetRequest(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        URL url = new URL("http://localhost:" + getHttpPort() + "/graphql?query=" + encodedQuery);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setRequestProperty("Accept", "application/json");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder buffer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }

        return buffer.toString();
    }

    private String sendPostRequest(String query) throws Exception {
        URL url = new URL("http://localhost:" + getHttpPort() + "/graphql");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("query", query);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = node.toString().getBytes(StandardCharsets.UTF_8.name());
            os.write(input, 0, input.length);
        }


        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder buffer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }

        return buffer.toString();
    }
}
