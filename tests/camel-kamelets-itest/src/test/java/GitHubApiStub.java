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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.citrusframework.base.endpoint.adapter.StaticEndpointAdapter;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.message.HttpMessageHeaders;
import org.citrusframework.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Stands in for the GitHub API in the tests of the GitHub sources: answers a request with the canned JSON in the
 * resource {@code github/api/<path>.json}, where the path is the path of the request without its query, such as
 * {@code repos/citrus/demo/commits}. The addresses of the GitHub API in the answers, which the GitHub client
 * follows to read more of an object, are changed to the address of the stand-in. When the resource
 * {@code github/api/<path>.first.json} exists, it is the answer to the first request for that path after
 * {@link #reset()} instead, so a source that remembers what exists when it starts can be shown something new
 * afterwards. Other requests get 404, as from GitHub, and are listed in {@link #unknown()}. Every request is
 * listed in {@link #requests()}, so a test can check that a source asked for nothing more than it should.
 */
public class GitHubApiStub extends StaticEndpointAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubApiStub.class);

    private static final String GITHUB_API = "https://api.github.com";

    private final String apiUrl;

    private final List<String> unknown = new CopyOnWriteArrayList<>();

    private final List<String> requests = new CopyOnWriteArrayList<>();

    private final Set<String> answered = ConcurrentHashMap.newKeySet();

    public GitHubApiStub(int port) {
        this.apiUrl = "http://localhost:" + port;
    }

    public void reset() {
        unknown.clear();
        requests.clear();
        answered.clear();
    }

    public List<String> unknown() {
        return unknown;
    }

    public List<String> requests() {
        return requests;
    }

    @Override
    protected Message handleMessageInternal(Message request) {
        String path = String.valueOf(request.getHeader(HttpMessageHeaders.HTTP_REQUEST_URI));
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        path = path.replaceAll("^/+|/+$", "");
        LOG.info("GitHub API request {}", path);
        requests.add(path);

        String json = null;
        if (answered.add(path)) {
            json = load("github/api/" + path + ".first.json");
        }
        if (json == null) {
            json = load("github/api/" + path + ".json");
        }
        if (json != null) {
            return new HttpMessage(json.replace(GITHUB_API, apiUrl))
                    .status(HttpStatus.OK).contentType("application/json; charset=utf-8");
        }
        LOG.warn("No canned answer for GitHub API request {}", path);
        unknown.add(path);
        return new HttpMessage("{\"message\":\"Not Found\",\"documentation_url\":\"https://docs.github.com/rest\"}")
                .status(HttpStatus.NOT_FOUND).contentType("application/json; charset=utf-8");
    }

    private static String load(String resource) {
        try (InputStream in = GitHubApiStub.class.getClassLoader().getResourceAsStream(resource)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + resource, e);
        }
    }
}
