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
package com.sun.xml.ws.addressing;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.stream.buffer.XMLStreamBufferSource;
import com.sun.xml.stream.buffer.stax.StreamWriterBufferCreator;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.developer.MemberSubmissionEndpointReference;
import com.sun.xml.ws.util.DOMUtil;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.ws.wsdl.parser.WSDLConstants;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import org.w3c.dom.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rama Pulavarthi
 */

public class EndpointReferenceUtil {
    /**
     * Gives the EPR based on the clazz. It may need to perform tranformation from
     * W3C EPR to MS EPR or vise-versa.
     */
    public static <T extends EndpointReference> T transform(Class<T> clazz, @NotNull EndpointReference epr) {
        assert epr != null;
        if (clazz.isAssignableFrom(W3CEndpointReference.class)) {
            if (epr instanceof W3CEndpointReference) {
                return (T) epr;
            } else if (epr instanceof MemberSubmissionEndpointReference) {
                return (T) toW3CEpr((MemberSubmissionEndpointReference) epr);
            }
        } else if (clazz.isAssignableFrom(MemberSubmissionEndpointReference.class)) {
            if (epr instanceof W3CEndpointReference) {
                return (T) toMSEpr((W3CEndpointReference) epr);
            } else if (epr instanceof MemberSubmissionEndpointReference) {
                return (T) epr;
            }
        }

        //This must be an EPR that we dont know
        throw new WebServiceException("Unknwon EndpointReference: " + epr.getClass());
    }

    //TODO: bit of redundency on writes of w3c epr, should modularize it
    private static W3CEndpointReference toW3CEpr(MemberSubmissionEndpointReference msEpr) {
        StreamWriterBufferCreator writer = new StreamWriterBufferCreator();
        w3cMetadataWritten = false;
        try {
            writer.writeStartDocument();
            writer.writeStartElement(AddressingVersion.W3C.getPrefix(),
                    "EndpointReference", AddressingVersion.W3C.nsUri);
            writer.writeNamespace(AddressingVersion.W3C.getPrefix(),
                    AddressingVersion.W3C.nsUri);
            //write wsa:Address
            writer.writeStartElement(AddressingVersion.W3C.getPrefix(),
                    W3CAddressingConstants.WSA_ADDRESS_NAME, AddressingVersion.W3C.nsUri);
            writer.writeCharacters(msEpr.addr.uri);
            writer.writeEndElement();
            //TODO: write extension attributes on wsa:Address
            if ((msEpr.referenceProperties != null && msEpr.referenceProperties.elements.size() > 0) ||
                    (msEpr.referenceParameters != null && msEpr.referenceParameters.elements.size() > 0)) {

                writer.writeStartElement(AddressingVersion.W3C.getPrefix(), "ReferenceParameters", AddressingVersion.W3C.nsUri);

                //write ReferenceProperties
                if (msEpr.referenceProperties != null) {
                    for (Element e : msEpr.referenceProperties.elements) {
                        DOMUtil.serializeNode(e, writer);
                    }
                }
                //write referenceParameters
                if (msEpr.referenceParameters != null) {
                    for (Element e : msEpr.referenceParameters.elements) {
                        DOMUtil.serializeNode(e, writer);
                    }
                }
                writer.writeEndElement();
            }
            // Supress writing ServiceName and EndpointName in W3CEPR,
            // Until the ns for those metadata elements is resolved.
            /*
            //Write Interface info
            if (msEpr.portTypeName != null) {
                writeW3CMetadata(writer);
                writer.writeStartElement(AddressingVersion.W3C.getWsdlPrefix(),
                        W3CAddressingConstants.WSAW_INTERFACENAME_NAME,
                        AddressingVersion.W3C.wsdlNsUri);
                writer.writeNamespace(AddressingVersion.W3C.getWsdlPrefix(),
                        AddressingVersion.W3C.wsdlNsUri);
                String portTypePrefix = fixNull(msEpr.portTypeName.name.getPrefix());
                writer.writeNamespace(portTypePrefix, msEpr.portTypeName.name.getNamespaceURI());
                if (portTypePrefix.equals(""))
                    writer.writeCharacters(msEpr.portTypeName.name.getLocalPart());
                else
                    writer.writeCharacters(portTypePrefix + ":" + msEpr.portTypeName.name.getLocalPart());
                writer.writeEndElement();
            }
            if (msEpr.serviceName != null) {
                writeW3CMetadata(writer);
                //Write service and Port info
                writer.writeStartElement(AddressingVersion.W3C.getWsdlPrefix(),
                        W3CAddressingConstants.WSAW_SERVICENAME_NAME,
                        AddressingVersion.W3C.wsdlNsUri);
                writer.writeNamespace(AddressingVersion.W3C.getWsdlPrefix(),
                        AddressingVersion.W3C.wsdlNsUri);

                String servicePrefix = fixNull(msEpr.serviceName.name.getPrefix());
                if (msEpr.serviceName.portName != null)
                    writer.writeAttribute(W3CAddressingConstants.WSAW_ENDPOINTNAME_NAME,
                            msEpr.serviceName.portName);

                writer.writeNamespace(servicePrefix, msEpr.serviceName.name.getNamespaceURI());
                if (servicePrefix.length() > 0)
                    writer.writeCharacters(servicePrefix + ":" + msEpr.serviceName.name.getLocalPart());
                else
                    writer.writeCharacters(msEpr.serviceName.name.getLocalPart());
                writer.writeEndElement();
            }
            */
            //TODO: revisit this
            Element wsdlElement = null;
            //Check for wsdl in extension elements
            if ((msEpr.elements != null) && (msEpr.elements.size() > 0)) {
                for (Element e : msEpr.elements) {
                    if(e.getNamespaceURI().equals(MemberSubmissionAddressingConstants.MEX_METADATA.getNamespaceURI()) &&
                            e.getLocalName().equals(MemberSubmissionAddressingConstants.MEX_METADATA.getLocalPart())) {
                        NodeList nl = e.getElementsByTagNameNS(WSDLConstants.NS_WSDL,
                                WSDLConstants.QNAME_DEFINITIONS.getLocalPart());
                        if(nl != null)
                            wsdlElement = (Element) nl.item(0);
                    }
                }
            }
            //write WSDL
            if (wsdlElement != null) {
                DOMUtil.serializeNode(wsdlElement, writer);
            }

            if (w3cMetadataWritten)
                writer.writeEndElement();
            //TODO revisit this
            //write extension elements
            if ((msEpr.elements != null) && (msEpr.elements.size() > 0)) {
                for (Element e : msEpr.elements) {
                    if (e.getNamespaceURI().equals(WSDLConstants.NS_WSDL) &&
                            e.getLocalName().equals(WSDLConstants.QNAME_DEFINITIONS.getLocalPart())) {
                        // Don't write it as this is written already in Metadata
                    }
                    DOMUtil.serializeNode(e, writer);
                }
            }

            //TODO:write extension attributes

            //</EndpointReference>
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
        return new W3CEndpointReference(new XMLStreamBufferSource(writer.getXMLStreamBuffer()));
    }

    private static boolean w3cMetadataWritten = false;

    private static void writeW3CMetadata(StreamWriterBufferCreator writer) throws XMLStreamException {
        if (!w3cMetadataWritten) {
            writer.writeStartElement(AddressingVersion.W3C.getPrefix(), W3CAddressingConstants.WSA_METADATA_NAME, AddressingVersion.W3C.nsUri);
            w3cMetadataWritten = true;
        }
    }

    private static MemberSubmissionEndpointReference toMSEpr(W3CEndpointReference w3cEpr) {
        DOMResult result = new DOMResult();
        w3cEpr.writeTo(result);
        Node eprNode = result.getNode();
        Element e = DOMUtil.getFirstElementChild(eprNode);
        if (e == null)
            return null;

        MemberSubmissionEndpointReference msEpr = new MemberSubmissionEndpointReference();

        NodeList nodes = e.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) nodes.item(i);
                if (child.getNamespaceURI().equals(AddressingVersion.W3C.nsUri) &&
                        child.getLocalName().equals(W3CAddressingConstants.WSA_ADDRESS_NAME)) {
                    if (msEpr.addr == null)
                        msEpr.addr = new MemberSubmissionEndpointReference.Address();
                    msEpr.addr.uri = XmlUtil.getTextForNode(child);

                    //now add the attribute extensions
                    msEpr.addr.attributes = getAttributes(child);
                } else if (child.getNamespaceURI().equals(AddressingVersion.W3C.nsUri) &&
                        child.getLocalName().equals("ReferenceParameters")) {
                    NodeList refParams = child.getChildNodes();
                    for (int j = 0; j < refParams.getLength(); j++) {
                        if (refParams.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            if (msEpr.referenceParameters == null) {
                                msEpr.referenceParameters = new MemberSubmissionEndpointReference.Elements();
                                msEpr.referenceParameters.elements = new ArrayList<Element>();
                            }
                            msEpr.referenceParameters.elements.add((Element) refParams.item(i));
                        }
                    }
                } else if (child.getNamespaceURI().equals(AddressingVersion.W3C.nsUri) &&
                        child.getLocalName().equals(W3CAddressingConstants.WSA_METADATA_NAME)) {
                    NodeList metadata = child.getChildNodes();
                    for (int j = 0; j < metadata.getLength(); j++) {
                        Node node = metadata.item(j);
                        if (node.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        Element elm = (Element) node;
                        if (elm.getNamespaceURI().equals(AddressingVersion.W3C.wsdlNsUri) &&
                                elm.getLocalName().equals(W3CAddressingConstants.WSAW_SERVICENAME_NAME)) {
                            msEpr.serviceName = new MemberSubmissionEndpointReference.ServiceNameType();
                            msEpr.serviceName.portName = elm.getAttribute(W3CAddressingConstants.WSAW_ENDPOINTNAME_NAME);

                            String service = elm.getTextContent();
                            String prefix = XmlUtil.getPrefix(service);
                            String name = XmlUtil.getLocalPart(service);

                            //if there is no service name then its not a valid EPR but lets continue as its optional anyway
                            if (name == null)
                                continue;

                            if (prefix != null) {
                                String ns = elm.lookupNamespaceURI(prefix);
                                if (ns != null)
                                    msEpr.serviceName.name = new QName(ns, name, prefix);
                            } else {
                                msEpr.serviceName.name = new QName(null, name);
                            }
                            msEpr.serviceName.attributes = getAttributes(elm);
                        } else if (elm.getNamespaceURI().equals(AddressingVersion.W3C.wsdlNsUri) &&
                                elm.getLocalName().equals(W3CAddressingConstants.WSAW_INTERFACENAME_NAME)) {
                            msEpr.portTypeName = new MemberSubmissionEndpointReference.AttributedQName();

                            String portType = elm.getTextContent();
                            String prefix = XmlUtil.getPrefix(portType);
                            String name = XmlUtil.getLocalPart(portType);

                            //if there is no portType name then its not a valid EPR but lets continue as its optional anyway
                            if (name == null)
                                continue;

                            if (prefix != null) {
                                String ns = elm.lookupNamespaceURI(prefix);
                                if (ns != null)
                                    msEpr.portTypeName.name = new QName(ns, name, prefix);
                            } else {
                                msEpr.portTypeName.name = new QName(null, name);
                            }
                            msEpr.portTypeName.attributes = getAttributes(elm);
                        } else if(elm.getNamespaceURI().equals(WSDLConstants.NS_WSDL) &&
                                elm.getLocalName().equals(WSDLConstants.QNAME_DEFINITIONS.getLocalPart())) {
                            Document doc = DOMUtil.createDom();
                            Element mexEl = doc.createElementNS(MemberSubmissionAddressingConstants.MEX_METADATA.getNamespaceURI(),
                                    MemberSubmissionAddressingConstants.MEX_METADATA.getPrefix()+":"
                                            +MemberSubmissionAddressingConstants.MEX_METADATA.getLocalPart());
                            Element metadataEl = doc.createElementNS(MemberSubmissionAddressingConstants.MEX_METADATA_SECTION.getNamespaceURI(),
                                    MemberSubmissionAddressingConstants.MEX_METADATA_SECTION.getPrefix()+":"
                                            +MemberSubmissionAddressingConstants.MEX_METADATA_SECTION.getLocalPart());
                            metadataEl.setAttribute(MemberSubmissionAddressingConstants.MEX_METADATA_DIALECT_ATTRIBUTE,
                                    MemberSubmissionAddressingConstants.MEX_METADATA_DIALECT_VALUE);
                            metadataEl.appendChild(elm);
                            mexEl.appendChild(metadataEl);

                        } else {
                            //TODO : Revisit this
                            //its extensions in META-DATA and should be copied to extensions in MS EPR
                            if (msEpr.elements == null) {
                                msEpr.elements = new ArrayList<Element>();
                            }
                            msEpr.elements.add(elm);
                        }
                    }
                } else {
                    //its extensions
                    if (msEpr.elements == null) {
                        msEpr.elements = new ArrayList<Element>();
                    }
                    msEpr.elements.add((Element) child);

                }
            } else if (nodes.item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
                Node n = nodes.item(i);
                if (msEpr.attributes == null) {
                    msEpr.attributes = new HashMap<QName, String>();
                    String prefix = fixNull(n.getPrefix());
                    String ns = fixNull(n.getNamespaceURI());
                    String localName = n.getLocalName();
                    msEpr.attributes.put(new QName(ns, localName, prefix), n.getNodeValue());
                }
            }
        }

        return msEpr;
    }

    private static Map<QName, String> getAttributes(Node node) {
        Map<QName, String> attribs = null;

        NamedNodeMap nm = node.getAttributes();
        for (int i = 0; i < nm.getLength(); i++) {
            if (attribs == null)
                attribs = new HashMap<QName, String>();
            Node n = nm.item(i);
            String prefix = fixNull(n.getPrefix());
            String ns = fixNull(n.getNamespaceURI());
            String localName = n.getLocalName();
            if (prefix.equals("xmlns") || prefix.length() == 0 && localName.equals("xmlns"))
                continue;

            //exclude some attributes
            if (!localName.equals(W3CAddressingConstants.WSAW_ENDPOINTNAME_NAME))
                attribs.put(new QName(ns, localName, prefix), n.getNodeValue());
        }
        return attribs;
    }

    private static
    @NotNull
    String fixNull(@Nullable String s) {
        if (s == null) return "";
        else return s;
    }

}
