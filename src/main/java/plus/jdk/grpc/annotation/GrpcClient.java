package plus.jdk.grpc.annotation;

import io.grpc.ClientInterceptor;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcClient {

    /**
     * 指定调用的目标地址， 例如：grpc://127.0.0.1:10202
     */
    String value();

    /**
     * 指定客户端默认的interceptors 列表
     */
    Class<? extends ClientInterceptor>[] interceptors() default {};
}
