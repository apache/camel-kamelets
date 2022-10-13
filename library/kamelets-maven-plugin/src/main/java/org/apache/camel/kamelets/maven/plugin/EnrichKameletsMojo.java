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
package org.apache.camel.kamelets.maven.plugin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.camelk.v1alpha1.Kamelet;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.kamelets.catalog.KameletsCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copy the properties from a source POM to a different destination POM for syncing purposes.
 */
@Mojo(name = "enrich", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class EnrichKameletsMojo extends AbstractMojo {

    /**
     * The base directory
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File baseDir;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Whether to fail if validation failed or not. By default true.
     */
    @Parameter(property = "kamelets.failOnError", defaultValue = "true")
    private boolean failOnError = true;

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        KameletsCatalog catalog = new KameletsCatalog();
        DefaultCamelCatalog cc = new DefaultCamelCatalog();
        File folder = new File("kamelets/");
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                try {
                    Kamelet local = MAPPER.readValue(file, Kamelet.class);
                    if (!(local.getMetadata().getLabels().get("camel.apache.org/kamelet.type").equalsIgnoreCase("action"))) {
                        String kameletName = local.getMetadata().getName();
                        int lastIndex = kameletName.lastIndexOf("-");
                        String schemeName = local.getMetadata().getName().substring(0, lastIndex);
                        Map<String, Object> selectedHeaders = new HashMap<>();
                        if (schemeName.equalsIgnoreCase("aws-s3")) {
                            List<ComponentModel.EndpointHeaderModel> headers = cc.componentModel("aws2-s3").getEndpointHeaders();
                            for (ComponentModel.EndpointHeaderModel e : headers
                            ) {
                                selectedHeaders.put(e.getName(), e.getDescription());
                            }
                            local.getMetadata().getAdditionalProperties().put("Headers", selectedHeaders);
                            MAPPER.writeValue(file, local);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    }
