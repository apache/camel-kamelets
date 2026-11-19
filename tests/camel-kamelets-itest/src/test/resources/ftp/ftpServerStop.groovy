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

// Stops the FTP server and deletes its home directory and certificate files.

import java.nio.file.Files
import java.nio.file.Paths

context.getReferenceResolver().resolve('ftpServer', org.apache.ftpserver.FtpServer.class).stop()

def home = Paths.get(context.getVariable('ftp.home'))
home.toFile().deleteDir()
['-keystore.p12', '-truststore.p12'].each {
    Files.deleteIfExists(home.resolveSibling(home.getFileName().toString() + it))
}
