/**
 * $Id: ServerEncoderDecoder.java,v 1.14 2005-08-25 20:20:52 vivekp Exp $
 */
/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.ws.encoding.soap;

import com.sun.pept.ept.MessageInfo;
import com.sun.pept.presentation.MessageStruct;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.encoding.internal.InternalEncoder;
import com.sun.xml.ws.encoding.jaxb.JAXBBridgeInfo;
import com.sun.xml.ws.encoding.soap.internal.AttachmentBlock;
import com.sun.xml.ws.encoding.soap.internal.BodyBlock;
import com.sun.xml.ws.encoding.soap.internal.HeaderBlock;
import com.sun.xml.ws.encoding.soap.internal.InternalMessage;
import com.sun.xml.ws.model.*;
import com.sun.xml.ws.model.soap.SOAPBinding;
import com.sun.xml.ws.model.soap.SOAPRuntimeModel;
import com.sun.xml.ws.server.RuntimeContext;
import com.sun.xml.ws.util.StringUtils;
import com.sun.xml.ws.util.exception.LocalizableExceptionAdapter;

import javax.xml.ws.Holder;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Vivek Pandey
 *
 * Server SOAP encoder decoder
 */
public class ServerEncoderDecoder extends EncoderDecoder implements InternalEncoder {
    public ServerEncoderDecoder() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.xml.rpc.encoding.util.EncoderDecoderBase#toMessageInfo(java.lang.Object,
     *      com.sun.pept.ept.MessageInfo)
     */
    public void toMessageInfo(Object intMessage, MessageInfo mi) {
        InternalMessage im = (InternalMessage) intMessage;
        RuntimeContext rtContext = (RuntimeContext) mi.getMetaData(BindingProviderProperties.JAXWS_RUNTIME_CONTEXT);

        BodyBlock bodyBlock = im.getBody();
        JavaMethod jm = rtContext.getModel().getJavaMethod(mi.getMethod());
        mi.setMEP(jm.getMEP());
        List<HeaderBlock> headers = im.getHeaders();
        Map<String, AttachmentBlock> attachments = im.getAttachments();

        Iterator<Parameter> iter = jm.getRequestParameters().iterator();
        Object bodyValue = (bodyBlock == null) ? null :  bodyBlock.getValue();

        // TODO process exceptions

        int numInputParams = jm.getInputParametersCount();
        Object data[] = new Object[numInputParams];
        SOAPBinding soapBinding = (SOAPBinding)jm.getBinding();
        while (iter.hasNext()) {
            Parameter param = iter.next();
            ParameterBinding paramBinding = param.getBinding();
            Object obj = null;
            if (paramBinding.isBody()) {
                obj = bodyValue;
            } else if (headers != null && paramBinding.isHeader()) {
                HeaderBlock header = getHeaderBlock(param.getName(), headers);
                obj = (header != null)?header.getValue():null;
            } else if (paramBinding.isAttachment()) {
              obj = getAttachment(rtContext, attachments, param);
            }
            fillData(rtContext, param, obj, data, soapBinding);
        }
        Iterator<Parameter> resIter = jm.getResponseParameters().iterator();
        while(resIter.hasNext()){
            Parameter p = resIter.next();
            createOUTHolders(p, data);
        }
        mi.setData(data);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.xml.rpc.encoding.util.EncoderDecoderBase#toInternalMessage(com.sun.pept.ept.MessageInfo)
     */
    public Object toInternalMessage(MessageInfo mi) {
        RuntimeContext rtContext = (RuntimeContext) mi.getMetaData(BindingProviderProperties.JAXWS_RUNTIME_CONTEXT);
        RuntimeModel model = rtContext.getModel();
        JavaMethod jm = model.getJavaMethod(mi.getMethod());
        Object[] data = mi.getData();
        Object result = mi.getResponse();
        InternalMessage im = new InternalMessage();
        SOAPBinding soapBinding = (SOAPBinding)jm.getBinding();

        switch (mi.getResponseType()) {
            case MessageStruct.CHECKED_EXCEPTION_RESPONSE:
                if (result instanceof java.rmi.RemoteException) {
                    if(soapBinding.getSOAPVersion().equals(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING))
                    SOAPRuntimeModel.createFaultInBody(result, getActor(), null, im);
                else if(soapBinding.getSOAPVersion().equals(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING))
                    SOAPRuntimeModel.createSOAP12FaultInBody(result, null, null, null, im);
                    return im;
                }
                if(!(result instanceof java.lang.Exception)){
                    //TODO its error, throw excetion?
                    break;
                }
                CheckedException ce = jm.getCheckedException(result.getClass());
                if(ce == null){
                    //TODO: throw exception
                    System.out.println("Error: Couldnt find model for: " + result.getClass());
                    break;
                }
                Object detail = getDetail(jm.getCheckedException(result.getClass()), result);
                JAXBBridgeInfo di = new JAXBBridgeInfo(model.getBridge(ce.getDetailType()), detail);
                if(ce.isHeaderFault())
                    SOAPRuntimeModel.createHeaderFault(result, null, di, im);
                else {
                    if(soapBinding.getSOAPVersion().equals(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING))
                        SOAPRuntimeModel.createFaultInBody(result, null, di, im);
                    else if(soapBinding.getSOAPVersion().equals(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)){
                        SOAPRuntimeModel.createSOAP12FaultInBody(result, null, null, di, im);
                    }
                }
                return im;
            case MessageStruct.UNCHECKED_EXCEPTION_RESPONSE:
                if(soapBinding.getSOAPVersion().equals(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING))
                    SOAPRuntimeModel.createFaultInBody(result, getActor(), null, im);
                else if(soapBinding.getSOAPVersion().equals(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING))
                    SOAPRuntimeModel.createSOAP12FaultInBody(result, null, null, null, im);
                return im;
        }

        Iterator<Parameter> iter = jm.getResponseParameters().iterator();
        while (iter.hasNext()) {
            Parameter param = iter.next();
            ParameterBinding paramBinding = param.getBinding();
            Object obj = createPayload(rtContext, param, data, result, soapBinding);
            if (paramBinding.isBody()) {
                im.setBody(new BodyBlock(obj));
            } else if (paramBinding.isHeader()) {
                im.addHeader(new HeaderBlock((JAXBBridgeInfo)obj));
            } else if (paramBinding.isAttachment()) {
                addAttachmentPart(rtContext, im, obj, param);
            }
        }
        return im;
    }

    private Object getDetail(CheckedException ce, Object exception) {
        if(ce.getExceptionType().equals(ExceptionType.UserDefined)){
            return createDetailFromUserDefinedException(ce, exception);
        }
        try {
            Method m = exception.getClass().getMethod("getFaultInfo");
            return m.invoke(exception);
        } catch(Exception e){
            throw new SerializationException(new LocalizableExceptionAdapter(e));
        }
    }

    private Object createDetailFromUserDefinedException(CheckedException ce, Object exception) {
        Class detailBean = ce.getDetailBean();
        Field[] fields = detailBean.getDeclaredFields();
        try {
            Object detail = detailBean.newInstance();
            for(Field f : fields){
                Method em = exception.getClass().getMethod(getReadMethod(f));
                Method sm = detailBean.getMethod(getWriteMethod(f), em.getReturnType());
                sm.invoke(detail, em.invoke(exception));
            }
            return detail;
        } catch(Exception e){
            throw new SerializationException(new LocalizableExceptionAdapter(e));
        }
    }

    private String getReadMethod(Field f){
        if(f.getType().isAssignableFrom(boolean.class))
            return "is" + StringUtils.capitalize(f.getName());
        return "get" + StringUtils.capitalize(f.getName());
    }
    
    private String getWriteMethod(Field f){
        return "set" + StringUtils.capitalize(f.getName());
    }    

    /**
     * @return the actor
     */
    public String getActor() {
        return null;
    }

    /**
     * To be used by the incoming message on the server side to set the OUT
     * holders with Holder instance.
     *
     * @param data
     */
    private void createOUTHolders(Parameter param, Object[] data) {
        if(param.isWrapperStyle()){
            for(Parameter p : ((WrapperParameter)param).getWrapperChildren()){
                if(!p.isResponse() && p.isOUT())
                    data[p.getIndex()] = new Holder();
            }
            return;
        }
        //its BARE
        if (!param.isResponse() && param.isOUT())
            data[param.getIndex()] = new Holder();
    }
}
