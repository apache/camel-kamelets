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

// Generates an RSA key pair for the OAuth 1.0a login of jira-oauth-source, so that no private key is kept
// in the repository. The private key goes to the Kamelet, in PKCS8 format and Base64 encoded on one line,
// as its privateKey option asks for.
//
//  - jira.private.key: the private key, for the Kamelet

import java.security.KeyPairGenerator

def generator = KeyPairGenerator.getInstance('RSA')
generator.initialize(2048)
def keyPair = generator.generateKeyPair()
context.setVariable('jira.private.key', Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()))
