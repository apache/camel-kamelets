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

// Starts an Apache FtpServer that speaks FTPS (explicit TLS, on the control and the data connections) with
// only the TLS versions in the variable ftp.tls.protocols, such as TLSv1.3, on port ftp.server.port of this
// machine. Otherwise it is the server of ftpServer.groovy: the user citrus, a new temporary home directory
// (variable ftp.home), a certificate for localhost made for the test, and ftp.truststore, a trust store with
// only this certificate. Apache FtpServer speaks only TLSv1.2 unless it is told otherwise.

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

if (context.getVariable('ftp.tls.protocols')) {
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

    // the trust store holds only the certificate of the server: the Kamelet uses it for its FTPS connection
    // alone (trustStoreFile), so the rest of the integration, such as the downloads of camel run, keeps the
    // trust store of the JVM; with the JVM-wide javax.net.ssl.trustStore instead, those downloads would fail
    // with "PKIX path building failed"
    def keys = KeyStore.getInstance('PKCS12')
    keyStore.toFile().withInputStream { keys.load(it, 'changeit'.toCharArray()) }
    def trusted = KeyStore.getInstance('PKCS12')
    trusted.load(null, null)
    trusted.setCertificateEntry('ftps', keys.getCertificate('ftps'))
    trustStore.toFile().withOutputStream { trusted.store(it, 'changeit'.toCharArray()) }
    context.setVariable('ftp.truststore', trustStore.toString())

    def ssl = new SslConfigurationFactory()
    ssl.setKeystoreFile(keyStore.toFile())
    ssl.setKeystoreType('PKCS12')
    ssl.setKeystorePassword('changeit')
    ssl.setKeyPassword('changeit')
    ssl.setSslProtocol(context.getVariable('ftp.tls.protocols').split(',') as String[])
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
