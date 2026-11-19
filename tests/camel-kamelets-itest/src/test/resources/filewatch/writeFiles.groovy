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

// Writes two files into the watched directory, as another program would: each file is written next to
// the directory first and then moved into it at once, so that the Kamelet never sees a half-written file.
// Only the CSV file matches the antInclude of the test.

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

def dir = Paths.get(context.getVariable('watch.dir'))
def staging = Files.createTempDirectory(dir.getParent(), 'kamelets-file-watch-staging-')
[['notes.txt', 'not a CSV file'], ['order 1.csv', 'id,item\n1,book\n']].each { name, content ->
    def file = staging.resolve(name)
    Files.writeString(file, content)
    Files.move(file, dir.resolve(name), StandardCopyOption.ATOMIC_MOVE)
}
Files.delete(staging)
