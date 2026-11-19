/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
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

// Puts two emails into the INBOX of the user citrus on the GreenMail server, over IMAPS: one that is already
// read, which the source skips, and one that is not. GreenMail has a self-signed certificate, which this
// client trusts.

import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

def props = new Properties()
props.put('mail.imaps.ssl.trust', '*')
props.put('mail.imaps.ssl.checkserveridentity', 'false')
def session = Session.getInstance(props)

def mail = { String subject, String text, boolean seen ->
    def message = new MimeMessage(session)
    message.setFrom(new InternetAddress('jane@example.org'))
    message.setRecipient(Message.RecipientType.TO, new InternetAddress('citrus@example.org'))
    message.setRecipient(Message.RecipientType.CC, new InternetAddress('team@example.org'))
    message.setSubject(subject)
    message.setSentDate(new Date())
    message.setText(text)
    message.saveChanges()
    message.setFlag(Flags.Flag.SEEN, seen)
    return message
}

def store = session.getStore('imaps')
store.connect(context.getVariable('CITRUS_TESTCONTAINERS_GREENMAIL_HOST'),
        Integer.parseInt(context.getVariable('CITRUS_TESTCONTAINERS_GREENMAIL_PORT')), 'citrus', 's3cr&t+pw')
try {
    def inbox = store.getFolder('INBOX')
    inbox.open(Folder.READ_WRITE)
    inbox.appendMessages([mail('Old order', 'Already handled.', true),
                          mail('Order 42', 'Please ship order 42.', false)] as Message[])
    inbox.close(false)
} finally {
    store.close()
}
