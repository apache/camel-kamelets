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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CatalogValidatorTest {

    private static final String SENTENCE = "Sends each message to somewhere. ";

    @Test
    void aShortSummaryFollowedByMoreParagraphs() {
        String description = "Sends each message to somewhere.\n\n" + SENTENCE.repeat(20);

        assertEquals(List.of(), verifySummary(description));
    }

    @Test
    void aSummaryOfSeveralLongSentences() {
        List<String> errors = verifySummary(SENTENCE.repeat(7).strip());

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("kamelet \"a-sink\" has a summary of 230 characters, more than 200"),
                errors.get(0));
    }

    @Test
    void aSummaryWrappedOverSeveralLinesIsOneParagraph() {
        List<String> errors = verifySummary((SENTENCE.strip() + "\n").repeat(7));

        assertEquals(1, errors.size());
    }

    @Test
    void noDescription() {
        assertEquals(List.of(), verifySummary(null));
    }

    @Test
    void anAttributeReferenceInTheDescription() {
        List<String> errors = verifyAttributeReferences("Sets the body to `${body}` again.", "The body.");

        assertEquals(List.of("kamelet \"a-sink\" has \"{body}\" in its description, which the documentation reads as an"
                + " AsciiDoc attribute: write \"\\{body}\" instead"), errors);
    }

    @Test
    void anAttributeReferenceInAPropertyDescription() {
        List<String> errors = verifyAttributeReferences("Sends each message.", "The template sees `{{body}}`.");

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("in its description of property \"template\""), errors.get(0));
    }

    @Test
    void escapedBracesAndOtherBracesAreFine() {
        assertEquals(List.of(), verifyAttributeReferences(
                "Sets `$\\{body}` and `{\\{body}}`, with `{{headers.name}}`, `{ \"id\": 1 }` and `{}`.",
                "A path such as `/pets/\\{petId}`."));
    }

    @Test
    void aPropertyNamedLikeAnOptionOfTheKameletComponent() {
        List<String> errors = verifyReservedPropertyNames("bucketName", "location");

        assertEquals(List.of("property \"location\" in kamelet \"a-sink\" has the name of the \"location\" option of the"
                + " kamelet: component, which takes it for itself and does not pass it on to the template: choose"
                + " another name"), errors);
    }

    @Test
    void everyOptionOfTheKameletComponentIsReservedWhateverItsCase() {
        assertEquals(1, verifyReservedPropertyNames("templateId").size());
        assertEquals(1, verifyReservedPropertyNames("routeId").size());
        assertEquals(1, verifyReservedPropertyNames("uuid").size());
        assertEquals(1, verifyReservedPropertyNames("timeout").size());
        assertEquals(1, verifyReservedPropertyNames("lazyStartProducer").size());
        assertEquals(1, verifyReservedPropertyNames("Location").size());
    }

    @Test
    void otherPropertyNamesAreFine() {
        assertEquals(List.of(), verifyReservedPropertyNames("region", "lazyStart", "connectTimeout", "locationId"));
    }

    @Test
    void aSourceWithoutDataTypes() {
        assertEquals(List.of("kamelet \"a-source\" does not declare spec.dataTypes.out: declare what it gives, or an"
                + " empty \"headers: {}\" with a comment that says why it has no default"),
                verifyDataTypes("source", null));
    }

    @Test
    void aSourceThatDeclaresOnlyEmptyHeaders() {
        assertEquals(List.of(), verifyDataTypes("source", Map.of("out", Map.of("headers", Map.of()))));
    }

    @Test
    void aSinkThatDeclaresOnlyItsOutput() {
        List<String> errors = verifyDataTypes("sink", Map.of("out", Map.of("default", "json")));

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("kamelet \"a-sink\" does not declare spec.dataTypes.in: declare what it"
                + " takes"), errors.get(0));
    }

    @Test
    void aSinkWithADefault() {
        assertEquals(List.of(), verifyDataTypes("sink", Map.of("in", Map.of("default", "binary"))));
    }

    @Test
    void anActionNeedsBothSides() {
        List<String> errors = verifyDataTypes("action", Map.of("in", Map.of("default", "json")));

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("spec.dataTypes.out"), errors.get(0));
        assertEquals(2, verifyDataTypes("action", Map.of()).size());
    }

    @Test
    void aSideWithoutAnyContentIsNotDeclared() {
        Map<String, Object> dataTypes = new LinkedHashMap<>();
        dataTypes.put("in", null);

        assertEquals(1, verifyDataTypes("sink", dataTypes).size());
    }

    private static List<String> verifyReservedPropertyNames(String... names) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String name : names) {
            properties.put(name, Map.of("type", "string", "title", "Option", "description", "An option."));
        }
        Map<String, Object> doc = Map.of("spec", Map.of("definition", Map.of("properties", properties)));
        List<String> errors = new ArrayList<>();
        CatalogValidator.verifyReservedPropertyNames(
                List.of(new CatalogValidator.KameletInfo("a-sink", new File("a-sink.kamelet.yaml"), doc)), errors);
        return errors;
    }

    private static List<String> verifyAttributeReferences(String description, String propertyDescription) {
        Map<String, Object> property = Map.of("type", "string", "title", "Template", "description", propertyDescription);
        Map<String, Object> definition = Map.of("title", "A Sink", "description", description,
                "properties", Map.of("template", property));
        Map<String, Object> doc = Map.of("spec", Map.of("definition", definition));
        List<String> errors = new ArrayList<>();
        CatalogValidator.verifyAttributeReferences(
                List.of(new CatalogValidator.KameletInfo("a-sink", new File("a-sink.kamelet.yaml"), doc)), errors);
        return errors;
    }

    private static List<String> verifySummary(String description) {
        Map<String, Object> definition = description != null ? Map.of("description", description) : Map.of();
        Map<String, Object> doc = Map.of("spec", Map.of("definition", definition));
        List<String> errors = new ArrayList<>();
        CatalogValidator.verifySummary(
                List.of(new CatalogValidator.KameletInfo("a-sink", new File("a-sink.kamelet.yaml"), doc)), errors);
        return errors;
    }

    private static List<String> verifyDataTypes(String type, Map<String, Object> dataTypes) {
        String name = "a-" + type;
        Map<String, Object> spec = new LinkedHashMap<>();
        if (dataTypes != null) {
            spec.put("dataTypes", dataTypes);
        }
        Map<String, Object> doc = Map.of("metadata", Map.of("labels", Map.of(CatalogValidator.TYPE_LABEL, type)),
                "spec", spec);
        List<String> errors = new ArrayList<>();
        CatalogValidator.verifyDataTypes(
                List.of(new CatalogValidator.KameletInfo(name, new File(name + ".kamelet.yaml"), doc)), errors);
        return errors;
    }
}
