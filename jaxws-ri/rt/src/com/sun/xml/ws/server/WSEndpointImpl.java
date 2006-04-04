package com.sun.xml.ws.server;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.api.pipe.PipelineAssemblerFactory;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.TransportBackChannel;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.sandbox.fault.SOAPFaultBuilder;
import com.sun.xml.ws.api.server.Container;

import javax.annotation.PreDestroy;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@link WSEndpoint} implementation.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public final class WSEndpointImpl<T> extends WSEndpoint<T> {
    private final WSBinding binding;
    private final SEIModel seiModel;
    private final Container container;
    private final WSDLPort port;
    private final InstanceResolver<T> instanceResolver;

    private final Pipe masterPipeline;
    private final ServiceDefinitionImpl serviceDef;
    private final SOAPVersion soapVersion;

    /**
     * Set to true once we start shutting down this endpoint.
     * Used to avoid running the clean up processing twice.
     *
     * @see #dispose()
     */
    private boolean disposed;


    public WSEndpointImpl(WSBinding binding, Container container, SEIModel seiModel, WSDLPort port, InstanceResolver<T> instanceResolver, @Nullable ServiceDefinitionImpl serviceDef, Pipe terminalPipe) {
        this.binding = binding;
        this.soapVersion = binding.getSOAPVersion();
        this.container = container;
        this.port = port;
        this.instanceResolver = instanceResolver;
        this.serviceDef = serviceDef;
        this.seiModel = seiModel;
        if (serviceDef != null) {
            serviceDef.setOwner(this);
        }

        PipelineAssembler assembler = PipelineAssemblerFactory.create(
                Thread.currentThread().getContextClassLoader(), binding.getBindingId());
        assert assembler!=null;

        this.masterPipeline = assembler.createServer(port, this, terminalPipe);

        instanceResolver.start(this,webServiceContext);
    }

    public @NotNull WSBinding getBinding() {
        return binding;
    }

    public @NotNull Container getContainer() {
        return container;
    }

    public WSDLPort getPort() {
        return port;
    }

    /**
     * Gets the {@link SEIModel} that represents the relationship
     * between WSDL and Java SEI.
     *
     * <p>
     * This method returns a non-null value if and only if this
     * endpoint is ultimately serving an application through an SEI.
     *
     * @return
     *      maybe null. See above for more discussion.
     *      Always the same value.
     */
    public @Nullable SEIModel getSEIModel() {
        return seiModel;
    }


    public PipeHead createPipeHead() {
        return new PipeHead() {
            private final Pipe pipe = PipeCloner.clone(masterPipeline);

            public @NotNull Packet process(Packet request, WebServiceContextDelegate wscd, TransportBackChannel tbc) {
                request.webServiceContextDelegate = wscd;
                request.transportBackChannel = tbc;
                request.endpoint = WSEndpointImpl.this;
                packets.set(request);
                Packet response;
                try {
                    response = pipe.process(request);
                } catch (RuntimeException re) {
                    // Catch all runtime exceptions so that transport doesn't
                    // have to worry about converting to wire message
                    // TODO XML/HTTP binding
                    re.printStackTrace();
                    Message faultMsg = SOAPFaultBuilder.createSOAPFaultMessage(
                            soapVersion, null, re);
                    response = new Packet(faultMsg);
                }
                return response;
            }
        };
    }

    public synchronized void dispose() {
        if(disposed)
            return;
        disposed = true;

        masterPipeline.preDestroy();
        instanceResolver.dispose();

        List<Handler> handlerChain = binding.getHandlerChain();
        if(handlerChain!=null) {
            for (Handler handler : handlerChain) {
                for (Method method : handler.getClass().getMethods()) {
                    if (method.getAnnotation(PreDestroy.class) == null) {
                        continue;
                    }
                    try {
                        method.invoke(handler);
                    } catch (Exception e) {
                        logger.warning("exception ignored from handler " +
                            "@PreDestroy method: " +
                            e.getMessage());
                    }
                    break;
                }
            }
        }
    }

    public InstanceResolver<T> getInstanceResolver() {
        return instanceResolver;
    }

    public ServiceDefinitionImpl getServiceDefinition() {
        return serviceDef;
    }

    public void setCurrentPacket(Packet p) {
        packets.set(p);
    }

    /**
     * Heart of {@link WebServiceContext}.
     * Remembers which thread is serving which packet.
     * This needs to be called by the ties.
     */
    private final ThreadLocal<Packet> packets = new ThreadLocal<Packet>();

    private final WebServiceContext webServiceContext = new WebServiceContext() {

        public MessageContext getMessageContext() {
            return new EndpointMessageContextImpl(getCurrentPacket());
        }

        public Principal getUserPrincipal() {
            Packet packet = getCurrentPacket();
            return packet.webServiceContextDelegate.getUserPrincipal(packet);
        }

        private Packet getCurrentPacket() {
            Packet p = packets.get();
            assert p!=null; // invoker must set
            return p;
        }

        public boolean isUserInRole(String role) {
            Packet packet = getCurrentPacket();
            return packet.webServiceContextDelegate.isUserInRole(packet,role);
        }
    };


    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.endpoint");
}
