/*
 * $Id: LocalizableSupport.java,v 1.3 2005-09-10 19:48:18 kohsuke Exp $
 */

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package com.sun.xml.ws.util.localization;

/**
 * @author WS Development Team
 */
public class LocalizableSupport {
    protected String key;
    protected Object[] arguments;

    public LocalizableSupport(String key) {
        this(key, (Object[]) null);
    }

    public LocalizableSupport(String key, String argument) {
        this(key, new Object[] { argument });
    }

    public LocalizableSupport(String key, Localizable localizable) {
        this(key, new Object[] { localizable });
    }

    public LocalizableSupport(String key, Object[] arguments) {
        this.key = key;
        this.arguments = arguments;
    }

    public String getKey() {
        return key;
    }
    public Object[] getArguments() {
        return arguments;
    }

    //abstract public String getResourceBundleName();
}
