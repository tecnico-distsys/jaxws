/*
 * $Id: HTTPBinding.java,v 1.3 2005-09-10 19:49:58 kohsuke Exp $
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

package com.sun.tools.ws.wsdl.document.http;

import javax.xml.namespace.QName;

import com.sun.tools.ws.wsdl.framework.Extension;

/**
 * A HTTP binding extension.
 *
 * @author WS Development Team
 */
public class HTTPBinding extends Extension {

    public HTTPBinding() {
    }

    public QName getElementName() {
        return HTTPConstants.QNAME_BINDING;
    }

    public String getVerb() {
        return _verb;
    }

    public void setVerb(String s) {
        _verb = s;
    }

    public void validateThis() {
        if (_verb == null) {
            failValidation("validation.missingRequiredAttribute", "verb");
        }
    }

    private String _verb;
}
