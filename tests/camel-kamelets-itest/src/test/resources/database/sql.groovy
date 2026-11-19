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

// Runs SQL with the command line client of the database inside its container, so the
// test module needs no JDBC driver of its own, and checks what it prints.
//
//  - db.container: the name of the container, and so which database it is
//  - db.sql:       the SQL to run, one or more statements
//  - db.expected:  optional; what the SQL must print, with the columns of a row joined by |
//
// The variables are read with context.getVariable rather than as dollar-brace
// expressions, because Citrus would replace those inside the SQL text.

import org.citrusframework.exceptions.ValidationException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.Transferable

def name = context.getVariable('db.container')
def sql = context.getVariable('db.sql')
def container = context.getReferenceResolver().resolve(name, GenericContainer)

def file = '/tmp/citrus.sql'
def command
switch (name) {
    case 'mariadb':
    case 'mysql':
        container.copyFileToContainer(Transferable.of(sql), file)
        command = ['bash', '-c', name + ' -utest -ptest testdb --batch --skip-column-names < ' + file]
        break
    case 'sqlserver':
        container.copyFileToContainer(Transferable.of('SET NOCOUNT ON\n' + sql), file)
        command = ['/opt/mssql-tools18/bin/sqlcmd', '-S', 'localhost', '-U', 'sa', '-P', 'Citrus-Test-1', '-C', '-b',
                   '-h', '-1', '-W', '-s', '|', '-i', file]
        break
    case 'oracle':
        container.copyFileToContainer(Transferable.of('WHENEVER SQLERROR EXIT FAILURE\nSET HEADING OFF FEEDBACK OFF PAGESIZE 0\n'
                + sql + '\nEXIT\n'), file)
        command = ['sqlplus', '-s', '-L', 'test/test@//localhost/FREEPDB1', '@' + file]
        break
    default:
        throw new IllegalArgumentException('Unknown database container: ' + name)
}

def result = container.execInContainer(command as String[])
if (result.exitCode != 0) {
    throw new ValidationException('SQL failed in ' + name + ' with exit code ' + result.exitCode + ': ' + result.stdout + result.stderr)
}

// sqlcmd reports a USE statement with "Changed database context to ..."
def output = result.stdout.readLines()*.trim().findAll { !it.isEmpty() && !it.startsWith('Changed database context') }.collect { it.replace('\t', '|') }.join('\n')
if (context.getVariables().containsKey('db.expected')) {
    def expected = context.getVariable('db.expected')
    if (output != expected) {
        throw new ValidationException('Expected ' + name + ' to give "' + expected + '", but it gave "' + output + '"')
    }
}
