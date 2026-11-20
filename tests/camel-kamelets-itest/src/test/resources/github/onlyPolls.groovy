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

// Checks that the source asked the stand-in of the GitHub API for nothing but the paths in the variable
// github.api.expected (separated by commas), that is, nothing more than its polls: no request per message.
// Only the requests of the repository of the test, the variable github.api.repo, and those outside of a
// repository, such as users/<login>, are counted; the other repositories belong to the integrations of other
// tests, which may still be stopping.

def expected = context.getVariable('github.api.expected').split(',').collect { it.trim() } as Set
def repo = 'repos/' + context.getVariable('github.api.repo')
def requests = context.getReferenceResolver().resolve('gitHubApi').requests()
def counted = requests.findAll { it == repo || it.startsWith(repo + '/') || !it.startsWith('repos/') }
def unexpected = counted.findAll { !expected.contains(it) }.unique()
if (!unexpected.isEmpty()) {
    throw new AssertionError('The source asked the GitHub API for ' + unexpected + ' besides its polls ' + expected
            + '; all its requests: ' + counted)
}
