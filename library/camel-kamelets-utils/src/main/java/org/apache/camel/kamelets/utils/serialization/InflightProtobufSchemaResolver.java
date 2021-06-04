package org.apache.camel.kamelets.utils.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.jackson.SchemaResolver;

public class InflightProtobufSchemaResolver implements SchemaResolver {

    @Override
    public FormatSchema resolve(Exchange exchange) {
        String schemaStr = (String) exchange.getProperty("schema");
        try {
            ProtobufSchema schema = ProtobufSchemaLoader.std.parse(schemaStr);
            return schema;
        } catch(IOException e) {
            throw new RuntimeCamelException("Cannot parse protobuf schema", e);
        }
    }
}
