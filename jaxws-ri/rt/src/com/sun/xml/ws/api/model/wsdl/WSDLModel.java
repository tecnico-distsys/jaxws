package com.sun.xml.ws.api.model.wsdl;


import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPBinding;
import java.util.List;
import java.util.Map;

/**
 * Provides abstraction of wsdl:definitions.
 *
 * @author Vivek Pandey
 */
public interface WSDLModel extends WSDLObject, WSDLExtensible{

    /**
     * Gets {@link WSDLPortType} that models <code>wsdl:portType</code>
     *
     * @param name non-null quaified name of wsdl:message, where the localName is the value of <code>wsdl:portType@name</code> and
     *             the namespaceURI is the value of wsdl:definitions@targetNamespace
     * @return A {@link com.sun.xml.ws.api.model.wsdl.WSDLPortType} or null if no wsdl:portType found.
     */
    WSDLPortType getPortType(QName name);

    /**
     * Gets {@link WSDLBoundPortType} that models <code>wsdl:binding</code>
     *
     * @param name non-null quaified name of wsdl:binding, where the localName is the value of <code>wsdl:binding@name</code> and
     *             the namespaceURI is the value of wsdl:definitions@targetNamespace
     * @return A {@link WSDLBoundPortType} or null if no wsdl:binding found
     */
    WSDLBoundPortType getBinding(QName name);

    /**
     * Give a {@link WSDLBoundPortType} for the given wsdl:service and wsdl:port names.
     *
     * @param serviceName non-null service QName
     * @param portName    non-null port QName
     * @return A {@link WSDLBoundPortType}. null if the Binding for the given wsd:service and wsdl:port name are not
     *         found.
     */
    WSDLBoundPortType getBinding(QName serviceName, QName portName);

    /**
     * Returns the bindings for the given bindingId
     *
     * @param service   non-null service
     * @param bindingId non-null - can be either {@link SOAPBinding#SOAP11HTTP_BINDING} or
     *                  {@link SOAPBinding#SOAP12HTTP_BINDING}
     * @return empty List if no wsdl:binding corresponding to the bindingId is found.
     */
    List<WSDLBoundPortType> getBindings(WSDLService service, String bindingId);

    /**
     * Gets {@link WSDLService} that models <code>wsdl:service</code>
     *
     * @param name non-null quaified name of wsdl:service, where the localName is the value of <code>wsdl:service@name</code> and
     *             the namespaceURI is the value of wsdl:definitions@targetNamespace
     * @return A {@link WSDLService} or null if no wsdl:service found
     */
    WSDLService getService(QName name);

    /**
     * Gives a {@link Map} of wsdl:portType {@link QName} and {@link WSDLPortType}
     *
     * @return an empty Map if the wsdl document has no wsdl:portType
     */
    Map<QName, ? extends WSDLPortType> getPortTypes();

    /**
     * Gives a {@link Map} of wsdl:binding {@link QName} and {@link WSDLBoundPortType}
     *
     * @return an empty Map if the wsdl document has no wsdl:binding
     */
    Map<QName, WSDLBoundPortType> getBindings();

    /**
     * Gives a {@link Map} of wsdl:service qualified name and {@link com.sun.xml.ws.api.model.wsdl.WSDLService}
     *
     * @return an empty Map if the wsdl document has no wsdl:service
     */
    Map<QName, ? extends WSDLService> getServices();    
}
