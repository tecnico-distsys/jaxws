/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.util;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents the version information.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Version {
    /**
     * Represents the build id, which is a string like "b13" or "hudson-250".
     */
    public final String BUILD_ID;
    /**
     * Represents the complete version string, such as "JAX-WS RI 2.0-b19"
     */
    public final String BUILD_VERSION;
    /**
     * Represents the major JAX-WS version, such as "2.0".
     */
    public final String MAJOR_VERSION;

    private Version(String buildId, String buildVersion, String majorVersion) {
        this.BUILD_ID = fixNull(buildId);
        this.BUILD_VERSION = fixNull(buildVersion);
        this.MAJOR_VERSION = fixNull(majorVersion);
    }

    public static Version create(InputStream is) {
        Properties props = new Properties();
        try {
            props.load(is);
        } catch (IOException e) {
            // ignore even if the property was not found. we'll treat everything as unknown
        } catch (Exception e) {
            //ignore even if property not found
        }

        return new Version(
            props.getProperty("build-id"),
            props.getProperty("build-version"),
            props.getProperty("major-version"));
    }

    private String fixNull(String v) {
        if(v==null) return "unknown";
        return v;
    }

    public String toString() {
        return BUILD_VERSION;
    }
}
