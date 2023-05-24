package org.apache.camel.kamelets.utils.format.converter.json;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.camel.kamelets.utils.format.SchemaType;

public class JsonFormatSchema implements FormatSchema {
    private final JsonNode schema;

    public JsonFormatSchema(JsonNode schema) {
        this.schema = schema;
    }

    @Override
    public String getSchemaType() {
        return SchemaType.JSON.type();
    }

    public JsonNode getSchema() {
        return schema;
    }
}
