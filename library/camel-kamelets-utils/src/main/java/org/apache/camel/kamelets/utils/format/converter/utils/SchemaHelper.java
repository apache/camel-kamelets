package org.apache.camel.kamelets.utils.format.converter.utils;

import org.apache.camel.Exchange;

public class SchemaHelper {

    public static final String SCHEMA = "schema";
    public static final String VALIDATE = "validate";
    public static final String CONTENT_SCHEMA = "X-Content-Schema";
    public static final String CONTENT_SCHEMA_TYPE = "X-Content-Schema-Type";
    public static final String CONTENT_CLASS = "X-Content-Class";

    private SchemaHelper() {
    }

    /**
     * Helper resolves content class from exchange properties and as a fallback tries to retrieve the content class
     * from the payload body type.
     * @param exchange the Camel exchange eventually holding content class information in its properties.
     * @param fallback the fallback content class information when no exchange property is set.
     * @return the content class as String representation.
     */
    public static String resolveContentClass(Exchange exchange, String fallback) {
        String contentClass = exchange.getProperty(CONTENT_CLASS, fallback, String.class);
        if (contentClass == null) {
            Object payload = exchange.getMessage().getBody();
            if (payload != null && PojoHelper.isPojo(payload.getClass())) {
                contentClass = payload.getClass().getName();
            }
        }

        return contentClass;
    }
}
