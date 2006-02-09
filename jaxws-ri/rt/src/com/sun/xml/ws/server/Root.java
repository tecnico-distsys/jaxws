package com.sun.xml.ws.server;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSEndpoint;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.model.wsdl.WSDLModelImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.sandbox.server.InstanceResolver;
import com.sun.xml.ws.sandbox.server.SDDocument;
import com.sun.xml.ws.sandbox.server.SDDocumentSource;
import com.sun.xml.ws.sandbox.server.ServiceDefinition;
import com.sun.xml.ws.server.provider.ProviderInvokerPipe;
import com.sun.xml.ws.server.sei.SEIInvokerPipe;
import com.sun.xml.ws.spi.runtime.Container;
import com.sun.xml.ws.util.HandlerAnnotationInfo;
import com.sun.xml.ws.util.HandlerAnnotationProcessor;
import com.sun.xml.ws.util.ServiceConfigurationError;
import com.sun.xml.ws.util.ServiceFinder;
import com.sun.xml.ws.util.localization.LocalizableMessageFactory;
import com.sun.xml.ws.util.localization.Localizer;
import com.sun.xml.ws.wsdl.parser.RuntimeWSDLParser;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver.Parser;
import com.sun.xml.ws.wsdl.writer.WSDLGenerator;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Entry point to the JAX-WS RI server-side runtime.
 * TODO: rename.
 *
 * TODO: more create method.
 *
 * @author Kohsuke Kawaguchi
 */
public class Root {
    /*
    no need to take WebServiceContext implementation. That's hidden inside our system.
    We shall only take delegate to getUserPrincipal and isUserInRole from adapter. 
    */

    /**
     * Used for "from-Java" deployment.
     *
     * <p>
     * This method works like the following:
     * <ol>
     * <li>{@link ServiceDefinition} is modeleed from the given SEI type.
     * <li>{@link InstanceResolver} that always serves <tt>implementationObject</tt> will be used.
     * <li>TODO: where does the binding come from?
     * </ol>
     *
     * @param resolver
     *      Optional resolver used to de-reference resources referenced from
     *      WSDL. Must be null if the {@code url} is null.
     * @param serviceName
     *      Optional service name to override the one given by the implementation clas.
     * @param portName
     *      Optional port name to override the one given by the implementation clas.
     *      
     * TODO: DD has a configuration for MTOM threshold.
     * Maybe we need something more generic so that other technologies
     * like Tango can get information from DD.
     *
     * TODO: does it really make sense for this to take EntityResolver?
     * Given that all metadata has to be given as a list anyway.
     *
     * @param primaryWsdl
     *      The {@link ServiceDefinition#getPrimary() primary} WSDL.
     *      If null, it'll be generated based on the SEI (if this is an SEI)
     *      or no WSDL is associated (if it's a provider.)
     *      TODO: shouldn't the implementation find this from the metadata list?
     * @param metadata
     *      Other documents that become {@link SDDocument}s. Can be null.
     *
     * @return newly constructed {@link WSEndpoint}.
     * @throws WebServiceException
     *      if the endpoint set up fails.
     */
    public <T> WSEndpoint<T> createSEIEndpoint(
        Class<T> implType, InstanceResolver<T> ir, QName serviceName, QName portName,
        Container container, WSBinding binding,
        SDDocumentSource primaryWsdl, Collection<? extends SDDocumentSource> metadata, EntityResolver resolver) {

        if(ir==null || implType ==null)
            throw new IllegalArgumentException();

        List<SDDocumentSource> md = new ArrayList<SDDocumentSource>();
        if(metadata!=null)
            md.addAll(metadata);

        if(primaryWsdl!=null && !md.contains(primaryWsdl))
            md.add(primaryWsdl);

        if(serviceName==null)
            serviceName = getDefaultServiceName(implType);
        // TODO: no error check is done about serviceName==null

        if(portName==null)
            portName = getDefaultPortName(serviceName,implType);
        // TODO: no error check is done about portName==null

        QName portTypeName = null; // TODO: the way this is used is very unclear to me - KK

        // setting a default binding
        if (binding == null)
            binding = BindingImpl.getBinding(null,implType,serviceName,true);

        Pipe terminal;
        WSDLPort wsdlPort = null;
        AbstractSEIModelImpl seiModel = null;

        {// create terminal pipe that invokes the application
            if (implType.getAnnotation(WebServiceProvider.class)!=null) {
                if (!Provider.class.isAssignableFrom(implType))
                    throw new ServerRtException("not.implement.provider",implType);

                // TODO: parse the primary WSDL to get 'wsdlPort'?
                terminal =  new ProviderInvokerPipe((Class)implType,(InstanceResolver)ir,binding);
            } else {
                // Create runtime model for non Provider endpoints
                seiModel = createSEIModel(primaryWsdl, md, implType, serviceName, portName, binding);
                if (serviceName == null)
                    serviceName = seiModel.getServiceQName();
                if (portName == null)
                    portName = seiModel.getPortName();
                portTypeName = seiModel.getPortTypeName();

                wsdlPort = seiModel.getPort();

                if (binding.getHandlerChain() == null) {
                    HandlerAnnotationInfo chainInfo =
                        HandlerAnnotationProcessor.buildHandlerInfo(
                        implType, serviceName, portName, binding);
                    if (chainInfo != null) {
                        binding.setHandlerChain(chainInfo.getHandlers());
                        if (binding instanceof SOAPBinding) {
                            ((SOAPBinding)binding).setRoles(chainInfo.getRoles());
                        }
                    }
                }
                //set momt processing
                if(binding instanceof SOAPBindingImpl){
                    seiModel.enableMtom(((SOAPBinding)binding).isMTOMEnabled());
                }
                terminal= new SEIInvokerPipe(seiModel,ir,binding);
            }
        }


        List<SDDocumentImpl> docList = buildMetadata(md, serviceName, portTypeName);



        {// error check
            String serviceNS = serviceName.getNamespaceURI();
            String portNS = portName.getNamespaceURI();
            if (!serviceNS.equals(portNS)) {
                throw new ServerRtException("wrong.tns.for.port",portNS, serviceNS);
            }
        }


        return new WSEndpointImpl<T>(binding,container,seiModel,wsdlPort,ir,new ServiceDefinitionImpl(docList),terminal);
    }


    private AbstractSEIModelImpl createSEIModel(
        SDDocumentSource primaryWsdl, List<SDDocumentSource> metadata,
        Class<?> implType, QName serviceName, QName portName, WSBinding binding) {

        // Create runtime model for non Provider endpoints

        // wsdlURL will be null, means we will generate WSDL. Hence no need to apply
        // bindings or need to look in the WSDL
        if(primaryWsdl == null){
            RuntimeModeler rap = new RuntimeModeler(implType,serviceName, binding.getBindingId(), false);
            if (portName != null) {
                rap.setPortName(portName);
            }
            return rap.buildRuntimeModel();
        }else {
            URL wsdlUrl = primaryWsdl.getSystemId();
            try {
                // TODO: delegate to another entity resolver
                WSDLModelImpl wsdlDoc = RuntimeWSDLParser.parse(
                    new Parser(primaryWsdl), new EntityResolverImpl(metadata),
                    ServiceFinder.find(WSDLParserExtension.class).toArray());
                WSDLPortImpl wsdlPort = null;
                if(serviceName == null)
                    serviceName = RuntimeModeler.getServiceName(implType);
                if(portName != null){
                    wsdlPort = wsdlDoc.getService(serviceName).get(portName);
                    if(wsdlPort == null)
                        throw new ServerRtException("runtime.parser.wsdl.incorrectserviceport", serviceName, portName, wsdlUrl);
                }else{
                    WSDLService service = wsdlDoc.getService(serviceName);
                    if(service == null)
                        throw new ServerRtException("runtime.parser.wsdl.noservice", serviceName, wsdlUrl);

                    String bindingId = binding.getBindingId();
                    List<WSDLBoundPortType> bindings = wsdlDoc.getBindings(service, bindingId);
                    if(bindings.size() == 0)
                        throw new ServerRtException("runtime.parser.wsdl.nobinding", bindingId, serviceName, wsdlUrl);

                    if(bindings.size() > 1)
                        throw new ServerRtException("runtime.parser.wsdl.multiplebinding", bindingId, serviceName, wsdlUrl);
                }
                //now we got the Binding so lets build the model
                RuntimeModeler rap = new RuntimeModeler(implType, serviceName, wsdlPort, false);
                if (portName != null) {
                    rap.setPortName(portName);
                }
                return rap.buildRuntimeModel();
            } catch (IOException e) {
                throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
            } catch (XMLStreamException e) {
                throw new ServerRtException("runtime.saxparser.exception", e.getMessage(), e.getLocation(), e);
            } catch (SAXException e) {
                throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
            } catch (ServiceConfigurationError e) {
                throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
            }
        }
    }


    /**
     * Checks {@link @WebServiceProvider} and determines the service name.
     */
    private QName getDefaultServiceName(Class<?> implType) {
        WebServiceProvider wsProvider = implType.getAnnotation(WebServiceProvider.class);
        if (wsProvider!=null) {
            String tns = wsProvider.targetNamespace();
            String local = wsProvider.serviceName();
            if (local.length() > 0)
                return new QName(tns, local);
        } else {
            return RuntimeModeler.getServiceName(implType);
        }
        return null;
    }

    /*
     * If portName is not already set via DD or programmatically, it uses
     * annotations on implementorClass to set PortName.
     */
    private QName getDefaultPortName(QName serviceName, Class<?> implType) {
        WebServiceProvider wsProvider = implType.getAnnotation(WebServiceProvider.class);
        if (wsProvider!=null) {
            String tns = wsProvider.targetNamespace();
            String local = wsProvider.portName();
            if (local.length() > 0)
                return new QName(tns, local);
        } else {
            return RuntimeModeler.getPortName(implType, serviceName.getNamespaceURI());
        }
        return null;
    }

    /**
     * Generates the WSDL and XML Schema for the endpoint if necessary
     * It generates WSDL only for SOAP1.1, and for XSOAP1.2 bindings
     */
    private SDDocumentSource generateWSDL(WSBinding binding, AbstractSEIModelImpl seiModel, List<SDDocumentImpl> docs) {
        BindingImpl bindingImpl = (BindingImpl)binding;
        String bindingId = bindingImpl.getActualBindingId();
        if (!bindingId.equals(SOAPBinding.SOAP11HTTP_BINDING) &&
            !bindingId.equals(SOAPBindingImpl.X_SOAP12HTTP_BINDING)) {
            throw new ServerRtException("can.not.generate.wsdl", bindingId);
        }

        if (bindingId.equals(SOAPBindingImpl.X_SOAP12HTTP_BINDING)) {
            String msg = localizer.localize(
                messageFactory.getMessage("generate.non.standard.wsdl"));
            logger.warning(msg);
        }

        // Generate WSDL and schema documents using runtime model
        WSDLGenResolver wsdlResolver = new WSDLGenResolver(docs,seiModel.getServiceQName(),seiModel.getPortName());
        WSDLGenerator wsdlGen = new WSDLGenerator(seiModel, wsdlResolver, binding.getBindingId());
        wsdlGen.doGeneration();

        // TODO: feed back the generated documents into the metadata list.
        throw new UnsupportedOperationException();
    }

    /**
     * Builds {@link SDDocumentImpl} from {@link SDDocumentSource}.
     */
    private List<SDDocumentImpl> buildMetadata(
        List<SDDocumentSource> src, QName serviceName, QName portTypeName) {

        List<SDDocumentImpl> r = new ArrayList<SDDocumentImpl>(src.size());

        for (SDDocumentSource doc : src) {
            r.add(SDDocumentImpl.create(doc,serviceName,portTypeName));
        }

        return r;
    }

    /**
     * {@link XMLEntityResolver} that can resolve to {@link SDDocumentSource}s.
     */
    private static final class EntityResolverImpl implements XMLEntityResolver {
        private Map<String,SDDocumentSource> metadata = new HashMap<String,SDDocumentSource>();

        public EntityResolverImpl(List<SDDocumentSource> metadata) {
            for (SDDocumentSource doc : metadata) {
                this.metadata.put(doc.getSystemId().toExternalForm(),doc);
            }
        }

        public Parser resolveEntity (String publicId, String systemId) throws IOException, XMLStreamException {
            if (systemId != null) {
                SDDocumentSource doc = metadata.get(systemId);
                if (doc != null)
                    return new Parser(doc);
            }
            return null;
        }

    }

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.endpoint");
    private static final Localizer localizer = new Localizer();
    private static final LocalizableMessageFactory messageFactory =
        new LocalizableMessageFactory("com.sun.xml.ws.resources.server");
}
