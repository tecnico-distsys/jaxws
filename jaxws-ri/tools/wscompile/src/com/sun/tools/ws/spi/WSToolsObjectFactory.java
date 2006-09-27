/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.tools.ws.spi;

import com.sun.tools.ws.util.WSToolsObjectFactoryImpl;
import com.sun.xml.ws.api.server.Container;

import java.io.OutputStream;


/**
 * Singleton abstract factory used to produce JAX-WS tools related objects.
 *
 * @author JAX-WS Development Team
 */
public abstract class WSToolsObjectFactory {

    private static final WSToolsObjectFactory factory = new WSToolsObjectFactoryImpl();

    /**
     * Obtain an instance of a factory. Don't worry about synchronization(at the
     * most, one more factory is created).
     */
    public static WSToolsObjectFactory newInstance() {
        return factory;
    }

    /**
     * Invokes wsimport on the wsdl URL argument, and generates the necessary
     * portable artifacts like SEI, Service, Bean classes etc.
     *
     * @param logStream Stream used for reporting log messages like errors, warnings etc
     * @param container gives an environment for tool if it is run during appserver
     *                  deployment
     * @param args arguments with various options and wsdl url
     *
     * @return true if there is no error, otherwise false
     */
    public abstract boolean wsimport(OutputStream logStream, Container container, String[] args);

    /**
     * Invokes wsimport on the wsdl URL argument, and generates the necessary
     * portable artifacts like SEI, Service, Bean classes etc.
     *
     * @return true if there is no error, otherwise false
     *
     * @see {@link #wsimport(OutputStream, Container, String[])}
     */
    public boolean wsimport(OutputStream logStream, String[] args) {
        return wsimport(logStream, Container.NONE, args);
    }

    /**
     * Invokes wsgen on the endpoint implementation, and generates the necessary
     * artifacts like wrapper, exception bean classes etc.
     *
     * @param logStream Stream used for reporting log messages like errors, warnings etc
     * @param container gives an environment for tool if it is run during appserver
     *                  deployment
     * @param args arguments with various options and endpoint class
     *
     * @return true if there is no error, otherwise false
     */
    public abstract boolean wsgen(OutputStream logStream, Container container, String[] args);
    
    /**
     * Invokes wsgen on the endpoint implementation, and generates the necessary
     * artifacts like wrapper, exception bean classes etc.
     *
     * @return true if there is no error, otherwise false
     * @see {@link #wsgen(OutputStream, Container, String[])}
     */
    public boolean wsgen(OutputStream logStream, String[] args) {
        return wsgen(logStream, Container.NONE, args);
    }

}
