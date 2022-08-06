package plus.jdk.grpc.annotation;


import org.springframework.context.annotation.Import;
import plus.jdk.grpc.selector.GrpcClientSelector;
import plus.jdk.grpc.selector.GrpcServerSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Import(GrpcClientSelector.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableGrpcClient {
}
