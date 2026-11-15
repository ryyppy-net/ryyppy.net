package drinkcounter.mcp;

import io.modelcontextprotocol.spec.McpError;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * The MCP transport answers a rejected request by serializing the {@link McpError} itself, which
 * is an exception. This writes it as a JSON-RPC error response instead, without the stack trace.
 */
@JacksonComponent
public class McpErrorSerializer extends StdSerializer<McpError> {

    public McpErrorSerializer() {
        super(McpError.class);
    }

    @Override
    public void serialize(McpError error, JsonGenerator generator, SerializationContext context) {
        generator.writeStartObject();
        generator.writeStringProperty("jsonrpc", "2.0");
        generator.writeNullProperty("id");
        generator.writeObjectPropertyStart("error");
        generator.writeNumberProperty("code", error.getJsonRpcError().code());
        generator.writeStringProperty("message", error.getJsonRpcError().message());
        generator.writeEndObject();
        generator.writeEndObject();
    }
}
