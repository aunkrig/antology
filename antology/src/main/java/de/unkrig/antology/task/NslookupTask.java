
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2013, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.antology.task;

import java.net.InetAddress;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Converts domain names into IP addresses and vice versa.
 */
public
class NslookupTask extends Task {

    @Nullable private String host;
    @Nullable private String addressProperty;
    @Nullable private String addressesProperty;
    @Nullable private String hostNameProperty;
    @Nullable private String canonicalHostNameProperty;
    private boolean          failOnError = true;

    /**
     * Either a machine name, such as "{@code java.sun.com}", or a textual representation of its IP address.
     * For hosts specified in literal IPv6 address, either the form defined in <a
     * href="//tools.ietf.org/html/rfc2732">RFC 2732</a> or the literal IPv6 address format defined in <a
     * href="//tools.ietf.org/html/rfc2373">RFC 2373</a> is accepted.
     * <a href="http://docs.oracle.com/javase/6/docs/api/java/net/Inet6Address.html#scoped">IPv6 scoped addresses</a>
     * are also supported.
     */
    public void
    setHost(String host) { this.host = host; }

    /**
     * The IP address of the host is determined and stored in the designated property.
     */
    public void
    setAddressProperty(String propertyName) { this.addressProperty = propertyName; }

    /**
     * The IP addresses of the host are determined, based on the configured name service on the system, and stored in
     * the designated property, separated by commas.
     */
    public void
    setAddressesProperty(String propertyName) { this.addressesProperty = propertyName; }

    /**
     * Performs a reverse name lookup, based on the system configured name lookup service, and the result is stored in
     * the designated property.
     */
    public void
    setHostNameProperty(String propertyName) { this.hostNameProperty = propertyName; }

    /**
     * Performs a reverse name lookup, based on the system configured name lookup service, and the result is stored in
     * the designated property.
     */
    public void
    setCanonicalHostNameProperty(String propertyName) { this.canonicalHostNameProperty = propertyName; }

    /**
     * Whether or not the execution of the current target should fail if the host name or address cannot resolved.
     *
     * @ant.defaultValue true
     */
    public void
    setFailOnError(boolean value) { this.failOnError  = value; }

    @Override public void
    execute() {
        try {
            this.execute2();
        } catch (BuildException be) {
            throw be;
        } catch (Exception e) {
            if (!this.failOnError) return;
            throw new BuildException(e);
        }
    }

    private void
    execute2() throws Exception {

        String host = this.host;
        if (host == null) throw new BuildException("Attribute 'host' missing");

        if (this.addressProperty != null) {
            this.getProject().setProperty(this.addressProperty, InetAddress.getByName(host).getHostAddress());
        }

        if (this.addressesProperty != null) {

            InetAddress[] inetAddresses = InetAddress.getAllByName(host);

            StringBuilder sb = new StringBuilder(inetAddresses[0].getHostAddress());
            for (int i = 1; i < inetAddresses.length; i++) {
                sb.append(",").append(inetAddresses[i].getHostAddress());
            }
            this.getProject().setProperty(this.addressesProperty, sb.toString());
        }

        if (this.hostNameProperty != null) {
            this.getProject().setProperty(this.hostNameProperty, InetAddress.getByName(host).getHostName());
        }

        if (this.canonicalHostNameProperty != null) {
            this.getProject().setProperty(
                this.canonicalHostNameProperty,
                InetAddress.getByName(host).getCanonicalHostName()
            );
        }
    }
}
