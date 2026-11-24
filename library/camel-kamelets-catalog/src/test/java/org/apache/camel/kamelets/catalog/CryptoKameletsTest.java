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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kamelet.KameletComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Encrypts with crypto-encrypt-action and decrypts with crypto-decrypt-action, with and without a mode.
 */
public class CryptoKameletsTest {

    // the source files, not the catalog jar
    private static final Path KAMELETS = Path.of("../../kamelets");
    // two equal blocks of 16 bytes, so ECB shows the repeat
    private static final String TEXT = "0123456789abcdef0123456789abcdef";

    private CamelContext context;

    @BeforeEach
    void start() throws Exception {
        context = new DefaultCamelContext();
        context.getComponent("kamelet", KameletComponent.class)
                .setLocation("file:" + KAMELETS.toAbsolutePath().normalize());
        context.start();
    }

    @AfterEach
    void stop() {
        if (context != null) {
            context.stop();
        }
    }

    @ParameterizedTest
    @CsvSource({ "AES,0123456789abcdef", "DESede,0123456789abcdef01234567" })
    void cbcGivesDifferentBytesForTheSameText(String algorithm, String key) {
        String options = "?algorithm=" + algorithm + "&key=RAW(" + key + ")&mode=CBC";

        byte[] first = encrypt(options);
        byte[] second = encrypt(options);

        assertFalse(Arrays.equals(first, second), "a new initialization vector for each message");
        assertEquals(TEXT, decrypt(options, first));
        assertEquals(TEXT, decrypt(options, second));
    }

    @ParameterizedTest
    @CsvSource({ "AES,0123456789abcdef,fedcba9876543210", "DESede,0123456789abcdef01234567,76543210" })
    void cbcGivesDifferentBytesForTheSameTextWithCryptoHeadersOnTheMessage(String algorithm, String key, String initVector) {
        String options = "?algorithm=" + algorithm + "&key=RAW(" + key + ")&mode=CBC";
        Map<String, Object> headers = Map.of("CamelCryptoInitVector", initVector.getBytes(StandardCharsets.US_ASCII));

        byte[] first = encrypt(options, headers);
        byte[] second = encrypt(options, headers);

        assertFalse(Arrays.equals(first, second), "the options decide, not the CamelCrypto headers of the message");
        assertEquals(TEXT, decrypt(options, first, headers));
        assertEquals(TEXT, decrypt(options, second, headers));
    }

    @ParameterizedTest
    @CsvSource({ "AES,0123456789abcdef" })
    void withoutModeTheDataOfEarlierVersionsIsStillDecrypted(String algorithm, String key) {
        String options = "?algorithm=" + algorithm + "&key=RAW(" + key + ")";

        byte[] first = encrypt(options);

        // ECB, as before: the same text gives the same bytes
        assertArrayEquals(first, encrypt(options));
        assertEquals(TEXT, decrypt(options, first));
    }

    private byte[] encrypt(String options) {
        return encrypt(options, Map.of());
    }

    private byte[] encrypt(String options, Map<String, Object> headers) {
        ProducerTemplate template = context.createProducerTemplate();
        return template.requestBodyAndHeaders("kamelet:crypto-encrypt-action" + options, TEXT, headers, byte[].class);
    }

    private String decrypt(String options, byte[] data) {
        return decrypt(options, data, Map.of());
    }

    private String decrypt(String options, byte[] data, Map<String, Object> headers) {
        ProducerTemplate template = context.createProducerTemplate();
        return template.requestBodyAndHeaders("kamelet:crypto-decrypt-action" + options, data, headers, String.class);
    }
}
