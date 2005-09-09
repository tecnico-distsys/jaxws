/**
 * $Id: JAXWSProperties.java,v 1.2 2005-09-09 18:00:14 vivekp Exp $
 */

/**
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.ws.developer;

public interface JAXWSProperties {
    // Content negotiation property: values "none", "pessimistic" and "optimistic"
    public static final String CONTENT_NEGOTIATION_PROPERTY = "com.sun.xml.ws.client.ContentNegotiation";
    public static final String MTOM_THRESHOLOD_VALUE =  "com.sun.xml.ws.common.MtomThresholdValue";
}
