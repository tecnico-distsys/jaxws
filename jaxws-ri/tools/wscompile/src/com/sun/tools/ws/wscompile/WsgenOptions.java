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

package com.sun.tools.ws.wscompile;

import com.sun.mirror.apt.Filer;
import com.sun.tools.ws.resources.WscompileMessages;
import com.sun.xml.ws.api.BindingID;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vivek Pandey
 */
public class WsgenOptions extends Options {
    /**
     * -servicename
     */
    public QName serviceName;

    /**
     * -portname
     */
    public QName portName;

    /**
     * -r
     */
    public File nonclassDestDir = destDir;


    /**
     * -wsdl
     */
    public boolean genWsdl;

    /**
     * protocol value
     */
    public String protocol = "soap1.1";
    public String transport;

    /**
     * -XwsgenReport
     */
    public File wsgenReport;

    /**
     * -Xdonotoverwrite
     */
    public boolean doNotOverWrite;

    public Filer filer;


    /**
     * Tells if user specified a specific protocol
     */
    public boolean protocolSet = false;

    private static final String SERVICENAME_OPTION = "-servicename";
    private static final String PORTNAME_OPTION = "-portname";
    private static final String HTTP   = "http";
    private static final String SOAP11 = "soap1.1";
    public static final String X_SOAP12 = "Xsoap1.2";

    @Override
    protected int parseArguments(String[] args, int i) throws BadCommandLineException {
        int j = super.parseArguments(args, i);
        if (args[i].equals(SERVICENAME_OPTION)) {
            serviceName = QName.valueOf(requireArgument(SERVICENAME_OPTION, args, ++i));
            if (serviceName.getNamespaceURI() == null || serviceName.getNamespaceURI().length() == 0) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_SERVICENAME_MISSING_NAMESPACE(args[i]));
            }
            if (serviceName.getLocalPart() == null || serviceName.getLocalPart().length() == 0) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_SERVICENAME_MISSING_LOCALNAME(args[i]));
            }
            return 2;
        } else if (args[i].equals(PORTNAME_OPTION)) {
            portName = QName.valueOf(requireArgument(PORTNAME_OPTION, args, ++i));
            if (portName.getNamespaceURI() == null || portName.getNamespaceURI().length() == 0) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_PORTNAME_MISSING_NAMESPACE(args[i]));
            }
            if (portName.getLocalPart() == null || portName.getLocalPart().length() == 0) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_PORTNAME_MISSING_LOCALNAME(args[i]));
            }
            return 2;
        } else if (args[i].equals("-r")) {
            nonclassDestDir = new File(requireArgument("-r", args, ++i));
            if (!nonclassDestDir.exists()) {
                throw new BadCommandLineException(WscompileMessages.WSCOMPILE_NO_SUCH_DIRECTORY(nonclassDestDir.getPath()));
            }
            return 2;
        } else if (args[i].equals("-classpath") || args[i].equals("-cp")) {
            classpath = requireArgument("-classpath", args, ++i) + File.pathSeparator + System.getProperty("java.class.path");
            return 2;
        } else if (args[i].startsWith("-wsdl")) {
            genWsdl = true;
            //String value = requireArgument("-wsdl", args, ++i).substring(5);
            String value = args[i].substring(5);
            int index = value.indexOf(':');
            if (index == 0) {
                value = value.substring(1);
                index = value.indexOf('/');
                if (index == -1) {
                    protocol = value;
                    transport = HTTP;
                } else {
                    protocol = value.substring(0, index);
                    transport = value.substring(index + 1);
                }
                if (!protocol.equalsIgnoreCase(SOAP11) ||
                        !protocol.equalsIgnoreCase(X_SOAP12)) {
                    throw new BadCommandLineException(WscompileMessages.WSGEN_INVALID_PROTOCOL(protocol, SOAP11 + ", " + X_SOAP12));
                }
                protocolSet = true;
                if (transport.equalsIgnoreCase(HTTP)) {
                    throw new BadCommandLineException(WscompileMessages.WSGEN_INVALID_TRANSPORT(transport, HTTP));
                }
            }
            return 1;
        } else if (args[i].equals("-XwsgenReport")) {
            // undocumented switch for the test harness
            wsgenReport = new File(requireArgument("-XwsgenReport", args, ++i));
            return 2;
        } else if (args[i].equals("-Xdonotoverwrite")) {
            doNotOverWrite = true;
            return 1;
        }
        return j;
    }


    @Override
    protected void addFile(String arg) {
        endpoints.add(arg);
    }

    List<String> endpoints = new ArrayList<String>();

    public Class endpoint;


    private boolean isImplClass;
    private boolean noWebServiceEndpoint;

    public void validate() throws BadCommandLineException {
        if (endpoints.isEmpty()) {
            throw new BadCommandLineException(WscompileMessages.WSGEN_MISSING_FILE());
        }
        if (protocol == null || protocol.equalsIgnoreCase(X_SOAP12) && !isExtensionMode()) {
            throw new BadCommandLineException(WscompileMessages.WSGEN_SOAP_12_WITHOUT_EXTENSION());
        }

        validateEndpointClass();
        validateArguments();
    }
    /**
     * Get an implementation class annotated with @WebService annotation.
     */
    private void validateEndpointClass() throws BadCommandLineException {
        Class clazz = null;
        for(String cls : endpoints){
            clazz = getClass(cls);
            if (clazz == null)
                continue;

            if (clazz.isEnum() || clazz.isInterface() ||
                clazz.isPrimitive()) {
                continue;
            }
            isImplClass = true;
            WebService webService = (WebService) clazz.getAnnotation(WebService.class);
            if(webService == null)
                continue;
            break;
        }
        if(clazz == null){
            throw new BadCommandLineException(WscompileMessages.WSGEN_CLASS_NOT_FOUND(endpoints.get(0)));
        }
        if(!isImplClass){
            throw new BadCommandLineException(WscompileMessages.WSGEN_CLASS_MUST_BE_IMPLEMENTATION_CLASS(clazz.getName()));
        }
        if(noWebServiceEndpoint){
            throw new BadCommandLineException(WscompileMessages.WSGEN_NO_WEBSERVICES_CLASS(clazz.getName()));
        }
        endpoint = clazz;
        validateBinding();
    }

    private void validateBinding() throws BadCommandLineException {
        if (genWsdl) {
            BindingID binding = BindingID.parse(endpoint);
            if ((binding.equals(BindingID.SOAP12_HTTP) ||
                 binding.equals(BindingID.SOAP12_HTTP_MTOM)) &&
                    !(protocol.equals(X_SOAP12) && isExtensionMode())) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_CANNOT_GEN_WSDL_FOR_SOAP_12_BINDING(binding.toString(), endpoint.getName()));
            }
            if (binding.equals(BindingID.XML_HTTP)) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_CANNOT_GEN_WSDL_FOR_NON_SOAP_BINDING(binding.toString(), endpoint.getName()));
            }
        }
    }

    private void validateArguments() throws BadCommandLineException {
        if (!genWsdl) {
            if (serviceName != null) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_WSDL_ARG_NO_GENWSDL(SERVICENAME_OPTION));
            }
            if (portName != null) {
                throw new BadCommandLineException(WscompileMessages.WSGEN_WSDL_ARG_NO_GENWSDL(PORTNAME_OPTION));
            }
        }
    }

    public static BindingID getBindingID(String protocol) {
        if (protocol.equals(SOAP11))
            return BindingID.SOAP11_HTTP;
        if (protocol.equals(X_SOAP12))
            return BindingID.SOAP12_HTTP;
        return null;
    }


    private Class getClass(String className) {
        try {
            return getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
