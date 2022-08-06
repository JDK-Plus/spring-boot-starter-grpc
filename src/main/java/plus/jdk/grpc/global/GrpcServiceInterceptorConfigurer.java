package plus.jdk.grpc.global;

import io.grpc.ServerInterceptor;

import java.util.List;

public interface GrpcServiceInterceptorConfigurer {

    void configureServerInterceptors(List<ServerInterceptor> interceptors);
}
