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

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern
import java.util.Map
import java.util.Properties

def updateKameletDirectory(String directoryName, Map properties) {
    println "#### Update Dependencies Version Kamelets BEGIN"

    // compute version replacement
    def libVersions = [:]
    properties.each {prop -> 
        if (prop.key.startsWith("version.")) {
            String ga = prop.key.substring("version.".length())
            int split = ga.lastIndexOf(".")
            if (split > 0 && split < ga.length() - 1) {
                String groupId = ga.substring(0, split)
                String artifactId = ga.substring(split + 1)
                String libSelector = "mvn:" + Pattern.quote(groupId) + ":" + Pattern.quote(artifactId) + ":[A-Za-z0-9-.]+"
                String libNewVersion = "mvn:" + groupId + ":" + artifactId + ":" + prop.value
                libVersions[libSelector] = libNewVersion
                println "#### Going to replace in all files \"${libSelector}\" with \"${libNewVersion}\""
            }
        }
    }

    File directory = new File(directoryName)
    File[] files = directory.listFiles()

    for (File f in files) {
        if (f.getName().endsWith(".kamelet.yaml")) {
            String kameletFile = f.getName() 
            new File( kameletFile + ".bak" ).withWriter { w ->
                new File( directoryName + kameletFile ).eachLine { line ->
                    libVersions.each { line = line.replaceAll(it.key, it.value) }
                                     w << line +  System.getProperty("line.separator") 
                }
            }
            Files.copy(Paths.get(kameletFile + ".bak"), Paths.get(directoryName + kameletFile), StandardCopyOption.REPLACE_EXISTING)
            boolean deleted = new File(kameletFile + ".bak").delete()
        }
    }

    println "#### Update Dependencies Version Kamelets END"
}
