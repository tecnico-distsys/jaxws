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
package com.sun.xml.ws.developer;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.xml.ws.spi.WebServiceFeatureAnnotation;

/**
 * This feature represents the use of WS-Addressing with either
 * the SOAP 1.1/HTTP or SOAP 1.2/HTTP binding.  Using this feature
 * with any other binding is NOT required.
 * <p>
 * Enabling this feature will result in the
 * <code>wsaw:UsingAddressing</code> element being added to the
 * <code>wsdl:Binding</code> for
 * the endpoint and in the runtime being capable of responding to
 * WS-Addressing headers.
 * <p>
 * The following describes the affects of this feature with respect
 * to be enabled or disabled:
 * <ul>
 *  <li> ENABLED: In this Mode, Addressing will be enabled.
 *       If there is not a WSDL associated with the Endpoint and
 *       a WSDL is to be generated, it MUST be generated with the
 *       wsaw:UsingAddressing element. At runtime, Addressing headers
 *       MUST be consumed by the receiver and generated by the
 *       sender even if the WSDL declares otherwise. The
 *       mustUnderstand="0" attribute MUST be used on the Addressing
 *       headers.
 *  <li> DISABLED: In this Mode, Addressing will be disabled
 *       even if an associated WSDL specifies otherwise. At runtime,
 *       Addressing headers MUST NOT be used.
 * </ul>
 * <p>
 * The {@link #required} property can be used to
 * specify if the <code>required</code> attribute on the
 * <code>wsaw:UsingAddressing</code> element should
 * be <code>true</code> or <code>false</code>.  By default the
 * <code>wsdl:required</code> parameter is <code>false</code>.
 *
 * See <a href="http://www.w3.org/TR/2006/REC-ws-addr-core-20060509/">WS-Addressing</a>
 * for more information on WS-Addressing.
 * See <a href="http://www.w3.org/TR/2006/CR-ws-addr-wsdl-20060529/">WS-Addressing - WSDL 1.0
 * </a> for more information on <code>wsaw:UsingAddressing</code>.
 *
 * @since JAX-WS 2.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebServiceFeatureAnnotation(id= MemberSubmissionAddressingFeature.ID,bean=MemberSubmissionAddressingFeature.class)
public @interface MemberSubmissionAddressing {
    /**
     * Specifies if this feature is enabled or disabled.
     */
    boolean enabled() default true;

    /**
     * Property to determine the value of the
     * <code>wsdl:required</code> attribute on
     * <code>wsaw:UsingAddressing</code> element in the WSDL.
     */
    boolean required() default false;
}
