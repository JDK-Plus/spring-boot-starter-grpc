package plus.jdk.grpc.global;

import io.grpc.ClientInterceptor;

import java.util.List;

public interface GrpcClientInterceptorConfigurer {

    void configureClientInterceptors(List<ClientInterceptor> interceptors);
}
