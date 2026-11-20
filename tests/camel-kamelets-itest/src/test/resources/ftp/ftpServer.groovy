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

// Starts an Apache FtpServer on port ftp.server.port of this machine, with the user citrus, whose password
// has characters that have a meaning in an endpoint address, and whose home directory is a new temporary
// directory (variable ftp.home). With the variable ftp.tls set to true, the server speaks FTPS (explicit
// TLS, on the control and the data connections), with a certificate for localhost made for the test;
// ftp.truststore is then a trust store with this certificate and the certificates the JDK trusts, for the
// integration.

import java.nio.file.Files
import java.security.KeyStore

import org.apache.ftpserver.DataConnectionConfigurationFactory
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.ssl.SslConfigurationFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission

def home = Files.createTempDirectory('kamelets-ftp-').toRealPath()
context.setVariable('ftp.home', home.toString())

def listener = new ListenerFactory()
listener.setServerAddress('localhost')
listener.setPort(Integer.parseInt(context.getVariable('ftp.server.port')))

if (context.getVariables().containsKey('ftp.tls') && Boolean.parseBoolean(context.getVariable('ftp.tls'))) {
    def keyStore = home.resolveSibling(home.getFileName().toString() + '-keystore.p12')
    def trustStore = home.resolveSibling(home.getFileName().toString() + '-truststore.p12')
    def keytool = new File(System.getProperty('java.home'), 'bin/keytool').getAbsolutePath()
    def process = [keytool, '-genkeypair', '-alias', 'ftps', '-keyalg', 'RSA', '-keysize', '2048',
                   '-dname', 'CN=localhost', '-ext', 'SAN=dns:localhost,ip:127.0.0.1', '-validity', '2',
                   '-storetype', 'PKCS12', '-keystore', keyStore.toString(),
                   '-storepass', 'changeit', '-keypass', 'changeit'].execute()
    process.waitForProcessOutput(System.out, System.err)
    if (process.exitValue() != 0) {
        throw new IllegalStateException('keytool failed with exit code ' + process.exitValue())
    }

    // the trust store of the integration holds the certificate of the server AND the certificates the JDK trusts:
    // javax.net.ssl.trustStore applies to the whole JVM of the integration, so camel run downloads its
    // dependencies (such as a new camel-ftp SNAPSHOT from https://repository.apache.org) with it too, and a trust
    // store with only the test certificate fails those downloads with "PKIX path building failed"
    def keys = KeyStore.getInstance('PKCS12')
    keyStore.toFile().withInputStream { keys.load(it, 'changeit'.toCharArray()) }
    def trusted = KeyStore.getInstance('PKCS12')
    trusted.load(null, null)
    def jdkTrusted = KeyStore.getInstance(KeyStore.getDefaultType())
    new File(System.getProperty('java.home'), 'lib/security/cacerts').withInputStream { jdkTrusted.load(it, null) }
    for (alias in jdkTrusted.aliases().toList()) {
        if (jdkTrusted.isCertificateEntry(alias)) {
            trusted.setCertificateEntry(alias, jdkTrusted.getCertificate(alias))
        }
    }
    trusted.setCertificateEntry('ftps', keys.getCertificate('ftps'))
    trustStore.toFile().withOutputStream { trusted.store(it, 'changeit'.toCharArray()) }
    context.setVariable('ftp.truststore', trustStore.toString())

    def ssl = new SslConfigurationFactory()
    ssl.setKeystoreFile(keyStore.toFile())
    ssl.setKeystoreType('PKCS12')
    ssl.setKeystorePassword('changeit')
    ssl.setKeyPassword('changeit')
    def sslConfiguration = ssl.createSslConfiguration()
    listener.setSslConfiguration(sslConfiguration)
    listener.setImplicitSsl(false)
    def dataConnection = new DataConnectionConfigurationFactory()
    dataConnection.setSslConfiguration(sslConfiguration)
    listener.setDataConnectionConfiguration(dataConnection.createDataConnectionConfiguration())
}

def user = new BaseUser()
user.setName('citrus')
user.setPassword('s3cr&t+pw')
user.setHomeDirectory(home.toString())
user.setAuthorities([new WritePermission()])

def serverFactory = new FtpServerFactory()
serverFactory.addListener('default', listener.createListener())
serverFactory.getUserManager().save(user)

def server = serverFactory.createServer()
server.start()
context.getReferenceResolver().bind('ftpServer', server)
