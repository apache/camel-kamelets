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
}
