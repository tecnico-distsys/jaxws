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
package com.sun.xml.ws.wsdl.writer;

import javax.xml.transform.Result;
import javax.xml.ws.Holder;


/**
 *
 *
 * TODO: please document! -KK
 *   for example,
 *      1) how the errors were supposed to be reported?
 *      2) what are "filenames"? are they absolute?
 *
 * @author WS Development Team
 */
public interface WSDLResolver {
    public Result getWSDL(String suggestedFilename);

    /**
     * Updates filename if the suggested filename need to be changed in
     * wsdl:import
     *
     * return null if abstract WSDL need not be generated
     */
    public Result getAbstractWSDL(Holder<String> filename);

    /**
     * Updates filename if the suggested filename need to be changed in
     * xsd:import
     *
     * return null if schema need not be generated
     */
    public Result getSchemaOutput(String namespace, Holder<String> filename);

}
