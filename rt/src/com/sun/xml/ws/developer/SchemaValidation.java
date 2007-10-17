package com.sun.xml.ws.developer;

import com.sun.xml.ws.server.DraconianValidationErrorHandler;

import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.ws.spi.WebServiceFeatureAnnotation;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Validates all request and response messages payload(SOAP:Body) for a {@link WebService}
 * against the XML schema. To use this feature, annotate the endpoint class with
 * this annotation.
 *
 * <pre>
 * for e.g.:
 *
 * &#64;WebService
 * &#64;SchemaValidation
 * public class HelloImpl {
 *   ...
 * }
 * </pre>
 *
 * At present, schema validation works for doc/lit web services only.
 *
 * @since JAX-WS 2.1.3
 * @author Jitendra Kotamraju
 * @see SchemaValidationFeature
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
@WebServiceFeatureAnnotation(id = SchemaValidationFeature.ID, bean = SchemaValidationFeature.class)
public @interface SchemaValidation {

    /**
     * Configure the validation behaviour w.r.t error handling. The default handler
     * just rejects any invalid schema intances. If the application want to change
     * this default behaviour(say just log the errors), it can do so by providing
     * a custom implementation of {@link ValidationErrorHandler}.
     */
    Class<? extends ValidationErrorHandler> handler() default DraconianValidationErrorHandler.class;

    /**
     * Does validation for bound headers in a SOAP message.
     *
    boolean headers() default false;
     */

    /**
     * Additional schema documents that are used to create {@link Schema} object. Useful
     * when the application adds additional SOAP headers to the message. This is a list
     * of system-ids, that are used to create {@link Source} objects and used in creation
     * of {@link Schema} object
     *
     * for e.g.:
     * @SchemaValidation(schemaLocations={"http://bar.foo/b.xsd", "http://foo.bar/a.xsd"}
     *
    String[] schemaLocations() default {};
     */

}
