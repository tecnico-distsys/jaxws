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

package com.sun.xml.ws.server.sei;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.util.QNameMap;

import javax.xml.namespace.QName;

/**
 * An {@link com.sun.xml.ws.server.sei.EndpointMethodDispatcher} that uses
 * SOAP payload first child's QName as the key for dispatching.
 * <p/>
 * A map of all payload QNames on the port and the corresponding {@link EndpointMethodHandler}
 * is initialized in the constructor. The payload QName is extracted from the
 * request {@link Packet} and used as the key to return the correct
 * handler.
 *
 * @author Arun Gupta
 */
final class PayloadQNameBasedDispatcher implements EndpointMethodDispatcher {
    private final QNameMap<EndpointMethodHandler> methodHandlers;
    private static final String EMPTY_PAYLOAD_LOCAL = "";
    private static final String EMPTY_PAYLOAD_NSURI = "";
    private static final QName EMPTY_PAYLOAD = new QName(EMPTY_PAYLOAD_NSURI, EMPTY_PAYLOAD_LOCAL);
    private WSBinding binding;

    public PayloadQNameBasedDispatcher(AbstractSEIModelImpl model, WSBinding binding, SEIInvokerTube invokerTube) {
        this.binding = binding;
        methodHandlers = new QNameMap<EndpointMethodHandler>();
        for( JavaMethodImpl m : model.getJavaMethods() ) {
            QName name = m.getRequestPayloadName();
            if (name == null)
                name = EMPTY_PAYLOAD;
            methodHandlers.put(name, new EndpointMethodHandler(invokerTube,m,binding));
        }
    }

    /**
     * {@link PayloadQNameBasedDispatcher} never returns null because this is always
     * the last {@link EndpointMethodHandler} to kick in.
     */
    public @NotNull EndpointMethodHandler getEndpointMethodHandler(Packet request) throws DispatchException {
        Message message = request.getMessage();
        String localPart = message.getPayloadLocalPart();
        String nsUri;
        if (localPart == null) {
            localPart = EMPTY_PAYLOAD_LOCAL;
            nsUri = EMPTY_PAYLOAD_NSURI;
        } else {
            nsUri = message.getPayloadNamespaceURI();
        }

        EndpointMethodHandler h = methodHandlers.get(nsUri, localPart);

        if (h==null) {
            String dispatchKey = "{" + nsUri + "}" + localPart;
            String faultString = ServerMessages.DISPATCH_CANNOT_FIND_METHOD(dispatchKey, "Payload QName-based Dispatcher");
            throw new DispatchException(SOAPFaultBuilder.createSOAPFaultMessage(
                binding.getSOAPVersion(), faultString, binding.getSOAPVersion().faultCodeClient));
        }

        return h;
    }

}
