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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.kamelets.catalog.KameletsCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.v1.kameletspec.Template;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Copy the properties from a source POM to a different destination POM for syncing purposes.
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class ValidateKameletsMojo extends AbstractMojo {

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String[] bannedDeps = {"mvn:", "camel:gson", "camel:core", "camel:kamelet"};
        List<String> bannedDepsList = Arrays.asList(bannedDeps);
        KameletsCatalog catalog = new KameletsCatalog();
        DefaultCamelCatalog cc = new DefaultCamelCatalog();
        List<String> names = catalog.getKameletsName();
        ObjectMapper om = new ObjectMapper();
        for (String name: names) {
            Map<Object, Object> templateJson;
            Template kd = catalog.getKameletTemplate(name);
            templateJson = om.convertValue(kd, new TypeReference<Map<Object, Object>>(){});
            Map<String,Object> f = (Map) templateJson.get("from");
            Map<String,Object> p = (Map) f.get("parameters");
            List<String> deps = catalog.getKameletDependencies(name).stream()
                    .filter(Predicate.not(bannedDepsList::contains))
                    .collect(Collectors.toList());
            String cleanName;
            if (!deps.isEmpty()) {
                if (deps.get(0).equals("camel:jackson") && deps.size() > 1) {
                    cleanName = deps.get(1).replace("camel:", "");
                } else {
                    cleanName = deps.get(0).replace("camel:", "");
                }
                if (cleanName.equalsIgnoreCase("cassandraql")) {
                    cleanName = "cql";
                }
                if (cleanName.equalsIgnoreCase("aws2-ddb") && name.equals("aws-ddb-streams-source")) {
                    cleanName = "aws2-ddb-streams";
                }
                if (cleanName.equalsIgnoreCase("google-sheets") && name.equals("google-sheets-source")) {
                    cleanName = "google-sheets-stream";
                }
                if (cleanName.equalsIgnoreCase("google-mail") && name.equals("google-mail-source")) {
                    cleanName = "google-mail-stream";
                }
                if (cleanName.equalsIgnoreCase("google-calendar") && name.equals("google-calendar-source")) {
                    cleanName = "google-calendar-stream";
                }
                if (p != null && !p.isEmpty()) {
                    ComponentModel componentModel = cc.componentModel(cleanName);
                    if (componentModel != null) {
                        List<ComponentModel.EndpointOptionModel> ce = componentModel.getEndpointOptions();
                        List<String> ceInternal =
                                ce.stream()
                                        .map(ComponentModel.EndpointOptionModel::getName)
                                        .sorted()
                                        .collect(Collectors.toList());
                        StringBuilder availableParams = new StringBuilder();
                        ceInternal.forEach(_param -> availableParams.append(_param).append(" "));
                        for (Map.Entry<String, Object> entry : p.entrySet()) {
                            if (!entry.getKey().equals("period") && (!name.equals("set-kafka-key-action") && !name.equals("sftp-source") && !name.equals("kafka-ssl-source") && !name.equals("timer-source") && !name.equals("cron-source") && !name.equals("fhir-source") && !name.equals("beer-source") && !name.equals("cassandra-source") && !name.equals("cassandra-sink") && !name.equals("kafka-azure-schema-registry-source" ) && !name.equals("kafka-azure-schema-registy-sink"))) {
                                if (!ceInternal.contains(entry.getKey())) {
                                    getLog().error("Kamelet Name: " + name);
                                    getLog().error("Scheme Name: " + cleanName);
                                    getLog().error("Parameter: " + entry.getKey());
                                    getLog().error("The parameter " + entry.getKey() + " doesn't exist in the endpoint options of " + cleanName + " component");
                                    getLog().error("Available endpoint options: " + availableParams);
                                    if (failOnError) {
                                        throw new MojoExecutionException("The Kamelets Validation failed. See logs for more information." + "\n");
                                    }
                                    break;
                                }
                            }
                        }
                        }
                    }
                }
            }
            getLog().info("Validation passed");
        }
    }
