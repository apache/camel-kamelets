package org.apache.camel.kamelets.utils.transform;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.InvalidPayloadException;

public class InsertField {

    public Map<?, ?> process(@ExchangeProperty("field") String field, @ExchangeProperty("value") String value, Exchange ex) throws InvalidPayloadException {
        Map<Object, Object> body = ex.getMessage().getBody(Map.class);
        if (body == null) {
            String val = ex.getMessage().getMandatoryBody(String.class);
            body = new HashMap<>();
            // TODO: make this configurable
            body.put("content", val);
        }
        body.put(field, value);
        return body;
    }

}
