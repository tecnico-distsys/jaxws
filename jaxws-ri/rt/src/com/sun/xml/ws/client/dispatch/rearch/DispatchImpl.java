/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.client.dispatch.DispatchContext;

import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * TODO: update javadoc, use sandbox classes where can
 */

/**
 * The <code>DispatchImpl</code> abstract class provides support
 * for the dynamic invocation of a service endpoint operation using XML
 * constructs, JAXB objects or <code>SOAPMessage</code>. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>DispatchImpl</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */
public abstract class DispatchImpl<T> extends Stub implements Dispatch<T> {

    protected final Service.Mode mode;
    protected final QName portname;
    protected Class<T> clazz;
    protected final WSServiceDelegate owner;
    protected final SOAPVersion soapVersion;
    protected static final long AWAIT_TERMINATION_TIME = 800L;

    /**
     *
     * @param port    dispatch instance is asssociated with this wsdl port qName
     * @param aClass  represents the class of the Message type associated with this Dispatch instance
     * @param mode    Service.mode associated with this Dispatch instance - Service.mode.MESSAGE or Service.mode.PAYLOAD
     * @param owner   Service that created the Dispatch
     * @param pipe    Master pipe for the pipeline
     * @param binding Binding of this Dispatch instance, current one of SOAP/HTTP or XML/HTTP
     */
    protected DispatchImpl(QName port, Class<T> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        this(port, mode, owner, pipe, binding);
        this.clazz = aClass;
    }

    /**
     *
     * @param port    dispatch instance is asssociated with this wsdl port qName
     * @param mode    Service.mode associated with this Dispatch instance - Service.mode.MESSAGE or Service.mode.PAYLOAD
     * @param owner   Service that created the Dispatch
     * @param pipe    Master pipe for the pipeline
     * @param binding Binding of this Dispatch instance, current one of SOAP/HTTP or XML/HTTP
     */
    protected DispatchImpl(QName port, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        super(pipe, binding, owner.getEndpointAddress(port));
        this.portname = port;
        this.mode = mode;
        this.owner = owner;
        this.soapVersion = binding.getSOAPVersion();
    }

    /**
     * Abstract method that is implemented by each concrete Dispatch class
     * @param msg  message passed in from the client program on the invocation
     * @return  The Message created returned as the Interface in actuallity a
     *          concrete Message Type
     */
    protected abstract Message createMessage(T msg);

    /**
     * Obtains the value to return from the response message.
     */
    protected abstract T toReturnValue(Message response);

    public final Response<T> invokeAsync(final T param) {
        // snapshot the context now. this is necessary to avoid concurrency issue, and is required by the spec
        final RequestContext rc = getRequestContext().copy();

        ResponseImpl<T> ft = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                return doInvoke(param,rc);
            }
        });
        //executor used needs to be one set by client or default on Service
        //todo: site spec requirement
        owner.getExecutor().execute(ft);
        return ft;
    }

    /* todo: Not sure that this meets the needs of tango for async callback */
    /* todo: Need to review with team                                       */

    public final Future<?> invokeAsync(final T param, final AsyncHandler<T> asyncHandler) {
        // snapshot the context now. this is necessary to avoid concurrency issue, and is required by the spec
        final RequestContext rc = getRequestContext().copy();

        final ResponseImpl<T>[] r = new ResponseImpl[1];
        r[0] = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                T t = doInvoke(param,rc);
                //doesn't work as r[0] result t has not been set
                //asynhandler will block indefinately
                //asyncHandler.handleResponse(r[0]);
                return t;
            }
        });
        r[0].setHandler(asyncHandler);
        // temp needed so that unit tests run and complete otherwise they may
        //not. Need a way to put this in the test harness or other way to do this
        //todo: as above
        ExecutorService exec = (ExecutorService) owner.getExecutor();
        try {
            exec.awaitTermination(AWAIT_TERMINATION_TIME, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exec.execute(r[0]);
        return r[0];
    }

    public final T doInvoke(T in, RequestContext rc) {
        Message message = createMessage(in);
        setProperties(message.getProperties(),false);
        Message response = process(message,rc);
        return toReturnValue(response);
    }

    public final T invoke(T in) {
        return doInvoke(in,getRequestContext());
    }

    public final void invokeOneWay(T in) {
        Message message = createMessage(in);
        setProperties(message.getProperties(),true);
        Message response = process(message,getRequestContext());
    }


    private static class ResponseImpl<T> extends FutureTask<T> implements Response<T> {

        private AsyncHandler<T> handler;

        protected ResponseImpl(Callable<T> callable) {
            super(callable);
        }

        private void setHandler(AsyncHandler<T> handler) {
            this.handler = handler;
        }

        @Override
        protected void done() {
            if (handler == null)
                return;

            try {
                if (!isCancelled())
                    handler.handleResponse(this);
            } catch (Throwable e) {
                super.setException(e);
            } finally {
                handler = null;
            }
        }

        public Map<String, Object> getContext() {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    protected void setProperties(MessageProperties props, boolean isOneWay) {
        props.isOneWay = isOneWay;

        //not needed but leave for now --maybe mode is needed
        props.otherProperties.put(DispatchContext.DISPATCH_MESSAGE_MODE, mode);
        if (clazz != null)
            props.otherProperties.put(DispatchContext.DISPATCH_MESSAGE_CLASS, clazz);
    }
}