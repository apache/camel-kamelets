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


import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.citrusframework.util.SocketUtils;

/**
 * Picks the ports of the HTTP servers that the tests start.
 * <p>
 * The port is taken below 32768, outside the ephemeral port range of Linux (32768-60999) and macOS (49152-65535).
 * Docker publishes the ports of the test containers on ephemeral ports, and outgoing connections use them too, so a
 * port picked from that range can be taken between the check that it is free and the moment the server binds it (seen
 * in CI as {@code Failed to bind to 0.0.0.0:50072: Address already in use}). A port is also handed out only once per
 * test run, so that two servers of one test class never get the same port.
 */
final class ServerPorts {

    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 32767;

    private static final Set<Integer> USED = ConcurrentHashMap.newKeySet();

    private ServerPorts() {
    }

    static int next() {
        while (true) {
            int port = SocketUtils.findAvailableTcpPort(MIN_PORT, MAX_PORT);
            if (USED.add(port)) {
                return port;
            }
        }
    }
}
