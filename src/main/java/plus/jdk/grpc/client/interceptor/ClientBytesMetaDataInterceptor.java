package plus.jdk.grpc.client.interceptor;

import io.grpc.*;
import plus.jdk.grpc.client.interceptor.marshaller.DefaultAsciiMarshaller;

import java.util.Map;

public class ClientBytesMetaDataInterceptor implements ClientInterceptor {

    private final Map<String, byte[]> metaHeaders;

    public ClientBytesMetaDataInterceptor(Map<String,  byte[]> headers) {
        this.metaHeaders = headers;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                for (String key : metaHeaders.keySet()) {
                    headers.put(Metadata.Key.of(key, new DefaultAsciiMarshaller()), metaHeaders.get(key));
                }
                super.start(responseListener, headers);
            }
        };
    }
}
