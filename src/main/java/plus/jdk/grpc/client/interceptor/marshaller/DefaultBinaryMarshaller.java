package plus.jdk.grpc.client.interceptor.marshaller;

import io.grpc.Metadata;

public class DefaultBinaryMarshaller implements Metadata.BinaryMarshaller<byte[]> {
    @Override
    public byte[] toBytes(byte[] value) {
        return value;
    }

    @Override
    public byte[] parseBytes(byte[] serialized) {
        return serialized;
    }
}
