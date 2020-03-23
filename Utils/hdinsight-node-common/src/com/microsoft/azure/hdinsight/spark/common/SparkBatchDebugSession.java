/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.jcraft.jsch.*;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import rx.Subscription;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.UnknownServiceException;
import java.util.Arrays;
import java.util.regex.Pattern;

/*
 * Spark Batch Job debug session with SSH tunnel
 */
public class SparkBatchDebugSession implements ILogger{
    @NotNull
    private final DebugUserInfo debugUserInfo;
    @NotNull
    private Session portForwardingSession;
    private JSch jsch;
    private Subscription logSubscription;
    private SparkBatchRemoteDebugJobSshAuth auth;
    @Nullable
    private String whoami;

    class DebugUserInfo implements UserInfo {
        @Nullable
        private String password;

        @Override
        public String getPassphrase() {
            throw new NotImplementedException("SSH key pass phrase hasn't been supported yet");
        }

        public void setPassword(@Nullable String password) {
            this.password = password;
        }

        @Nullable
        @Override
        public String getPassword() {
            return this.password;
        }

        @Override
        public boolean promptPassword(String message) {
            log().info(message);

            return true;
        }

        @Override
        public boolean promptPassphrase(String message) {
            log().info(message);

            return false;
        }

        @Override
        public boolean promptYesNo(String message) {
            log().info(message);

            return true;
        }

        @Override
        public void showMessage(String message) {
            log().info(message);
        }
    }

    SparkBatchDebugSession(JSch jsch, @NotNull Session portForwardingSession) {
        this.jsch = jsch;
        this.portForwardingSession = portForwardingSession;
        this.debugUserInfo = new DebugUserInfo();

        this.portForwardingSession.setUserInfo(this.debugUserInfo);
    }

    /**
     * Getter of the log subscription
     *
     * @return Subscription instance
     */
    public Subscription getLogSubscription() {
        return logSubscription;
    }

    /**
     * Setter of the log subscription
     *
     * @param logSubscription log Rx Subscription instance
     */
    public void setLogSubscription(Subscription logSubscription) {
        this.logSubscription = logSubscription;
    }

    /**
     * Getter of the jsch instance
     *
     * @return jsch instance
     */
    public JSch getJsch() {
        return jsch;
    }

    /**
     * Getter of the port forwarding session instance
     *
     * @return portForwardingSession instance
     */
    @NotNull
    public Session getPortForwardingSession() {
        return portForwardingSession;
    }

    /**
     * Specify the private key file for establish SSH connection
     *
     * @param file The private key file
     * @return the current instance for chain calling
     * @throws JSchException JSch operation exceptions
     */
    public SparkBatchDebugSession setPrivateKeyFile(File file) throws JSchException {
        this.getJsch().addIdentity(file.getPath());

        return this;
    }

    /**
     * Specify the password for SSH connection user
     *
     * @param password password to set
     * @return the current instance for chain calling
     */
    public SparkBatchDebugSession setPassword(String password) {
        getDebugUserInfo().setPassword(password);

        return this;
    }

    /**
     * Verify the user certificate with trying to talk with SSH server
     *
     * @return verified debug session
     * @throws JSchException
     * @throws IOException
     */
    public SparkBatchDebugSession verifyCertificate() throws JSchException, IOException {
        Session session = this.getPortForwardingSession();

        if (!session.isConnected()) {
            throw new JSchException("The session isn't connected, call the function after open()");
        }

        ChannelExec chan = (ChannelExec) session.openChannel("exec");

        // `whoami` command would return the current user,
        // which returns local user name in Linux, <domain>\<user> in Windows
        chan.setCommand("whoami");

        InputStream err = chan.getErrStream();
        InputStream stdout = chan.getInputStream();

        chan.connect();

        this.whoami = IOUtils.toString(stdout, "utf8").trim();

        log().info("Executing 'whoami' got the user: " + this.whoami);

        String errMessage = IOUtils.toString(err, "utf8");

        chan.disconnect();
        if (StringUtils.isNotBlank(errMessage)) {
            log().warn("Executing 'whoami' got error message: " + errMessage);
        }

        // For password expired case, the error message would be like:
        //
        //    WARNING: Your password has expired.
        //    Password change required but no TTY available.
        if (Pattern.compile("WARNING.*password.*expired").matcher(errMessage).find()) {
            throw new SshPasswordExpiredException(
                    "The user " + session.getUserName() + " password is expired. Error message:" + errMessage);
        }

        return this;
    }

    @Nullable
    public String getWhoami() {
        return whoami;
    }

    @NotNull
    public DebugUserInfo getDebugUserInfo() {
        return debugUserInfo;
    }

    /**
     * Close the SSH port forwarding session
     *
     * @return the current instance for chain calling
     */
    public SparkBatchDebugSession close() {
        if (getLogSubscription() != null) {
            getLogSubscription().unsubscribe();
        }

        this.getPortForwardingSession().disconnect();

        return this;
    }

    /**
     * Open the SSH port forwarding session
     *
     * @return the current instance for chain calling
     * @throws JSchException JSch operation exceptions
     */
    public SparkBatchDebugSession open() throws JSchException {
        this.getPortForwardingSession().connect();

        return this;
    }

    /**
     * Create an ephemeral Forward to remote host and port
     *
     * @param remoteHost the remote host in SSH server network
     * @param remotePort the remote port of host
     * @return the current instance for chain calling
     * @throws JSchException JSch operation exceptions
     */
    public SparkBatchDebugSession forwardToRemotePort(String remoteHost, int remotePort) throws JSchException {
        // 0 means to select the local automatically
        this.getPortForwardingSession().setPortForwardingL(0, remoteHost, remotePort);

        return this;
    }

    /**
     * Get the local ephemeral port to be forward by remote host and port
     *
     * @param remoteHost the remote host in SSH server network
     * @param remotePort the remote port of host
     * @return the local ephemeral port
     * @throws JSchException JSch operation exceptions
     * @throws UnknownServiceException if no local port found
     */
    public int getForwardedLocalPort(String remoteHost, int remotePort)
            throws JSchException,
                   UnknownServiceException {
        String localPort = Arrays.stream(this.getPortForwardingSession().getPortForwardingL())
                .filter((forwarding) -> forwarding.matches("\\d+:" + remoteHost + ":" + remotePort))
                .findFirst()
                .map((forwarding) -> forwarding.split(":")[0])
                .orElseThrow(() -> new UnknownServiceException(
                        "No local forwarded port found for " + remoteHost + ":" + remotePort));

        return Integer.parseInt(localPort);
    }

    /**
     * Create a SparkBatchDebugSession instance for specified host and user
     *
     * @param host The SSH host
     * @param user The SSH user
     * @return an SparkBatchDebugSession instance
     * @throws JSchException JSch operation exceptions
     */
    private static SparkBatchDebugSession factory(String host, String user) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host);

        java.util.Properties config = new java.util.Properties() {{
            put("StrictHostKeyChecking", "no");
            put("MaxAuthTries", "2");
        }};

        session.setConfig(config);

        return new SparkBatchDebugSession(jsch, session);
    }

    /*
     * Create a Spark Batch Job Debug Session with SSH certification
     */
    static public SparkBatchDebugSession factoryByAuth(String connectionUrl,
                                                       @NotNull SparkBatchRemoteDebugJobSshAuth auth)
            throws SparkJobException, JSchException {
        String sshServer = getSshHost(connectionUrl);

        SparkBatchDebugSession session = SparkBatchDebugSession.factory(sshServer, auth.getSshUserName());

        switch (auth.getSshAuthType()) {
            case UseKeyFile:
                session.setPrivateKeyFile(auth.getSshKeyFile());
                break;
            case UsePassword:
                session.setPassword(auth.getSshPassword());
                break;
            default:
                throw new SparkBatchRemoteDebugJobSshAuth.UnknownSSHAuthTypeException(
                        "Unknown SSH authentication type: " + auth.getSshAuthType().name());
        }

        session.auth = auth;

        return session;
    }

    /**
     * Get SSH Host from the HDInsight connection URL
     *
     * @param connectionUrl the HDInsight connection URL, such as: https://spkdbg.azurehdinsight.net/batch
     * @return SSH host
     */
    private static String getSshHost(String connectionUrl) {
        URI connectUri = URI.create(connectionUrl);
        String segs[] = connectUri.getHost().split("\\.");
        segs[0] = segs[0].concat("-ssh");
        return StringUtils.join(segs, ".");
    }

    public SparkBatchRemoteDebugJobSshAuth getAuth() {
        return auth;
    }

    /**
     * The SSH password expired exception
     */
    public class SshPasswordExpiredException extends JSchException {
        public SshPasswordExpiredException(String message) {
            super(message);
        }
    }
}
