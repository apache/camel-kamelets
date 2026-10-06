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
package org.apache.camel.kamelets.catalog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every Kamelet is written in the canonical YAML DSL: Camel logs "YAML DSL compact notation detected" for a template
 * in the compact notation, on every load and reload, to a user who cannot change a file inside the catalog jar. A
 * Kamelet is normalized with: camel validate normalize --output=kamelets kamelets/my-kamelet.kamelet.yaml
 */
public class KameletsCanonicalTest {

    // the source files, not the catalog jar: Camel does not warn about a file inside a jar
    private static final Path KAMELETS = Path.of("../../kamelets");

    private final List<String> warnings = new ArrayList<>();
    private AbstractAppender appender;

    @BeforeEach
    void captureWarnings() {
        appender = new AbstractAppender("compact-notation", null, null, false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (event.getLevel() == Level.WARN
                        && event.getMessage().getFormattedMessage().contains("compact notation")) {
                    warnings.add(event.getMessage().getFormattedMessage());
                }
            }
        };
        appender.start();
        ((Logger) LogManager.getRootLogger()).addAppender(appender);
    }

    @AfterEach
    void stopCapturing() {
        ((Logger) LogManager.getRootLogger()).removeAppender(appender);
        appender.stop();
    }

    @Test
    void everyKameletIsCanonical() throws Exception {
        List<Path> files;
        try (Stream<Path> s = Files.list(KAMELETS)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".kamelet.yaml")).sorted().toList();
        }
        assertTrue(files.size() > 200, "the Kamelets of " + KAMELETS.toAbsolutePath().normalize());
        List<String> compact = new ArrayList<>();
        for (Path file : files) {
            warnings.clear();
            try (DefaultCamelContext context = new DefaultCamelContext()) {
                PluginHelper.getRoutesLoader(context)
                        .loadRoutes(ResourceHelper.resolveResource(context, "file:" + file.toAbsolutePath()));
            }
            if (!warnings.isEmpty()) {
                compact.add(file.getFileName().toString());
            }
        }
        assertTrue(compact.isEmpty(), compact.size() + " Kamelet(s) in the compact notation, normalize them with"
                + " camel validate normalize --output=kamelets kamelets/<name>.kamelet.yaml: " + compact);
    }
}
