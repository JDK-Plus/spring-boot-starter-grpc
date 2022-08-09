package plus.jdk.grpc.client.interceptor.marshaller;

import io.grpc.Metadata;

import java.nio.charset.StandardCharsets;

public class DefaultAsciiMarshaller implements Metadata.AsciiMarshaller<byte[]>{

    @Override
    public String toAsciiString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    @Override
    public  byte[] parseAsciiString(String serialized) {
        return serialized.getBytes(StandardCharsets.UTF_8);
    }
}
