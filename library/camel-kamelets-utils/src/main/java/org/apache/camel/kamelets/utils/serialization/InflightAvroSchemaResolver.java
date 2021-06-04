package org.apache.camel.kamelets.utils.serialization;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;

import org.apache.avro.Schema;
import org.apache.camel.Exchange;
import org.apache.camel.component.jackson.SchemaResolver;

public class InflightAvroSchemaResolver implements SchemaResolver {

    @Override
    public FormatSchema resolve(Exchange exchange) {
        String schemaJson = (String) exchange.getProperty("schema");
        Boolean validate = Boolean.valueOf((String) exchange.getProperty("validate"));
        Schema raw = new Schema.Parser().setValidate(validate).parse(schemaJson);
        AvroSchema schema = new AvroSchema(raw);
        return schema;
    }

}
