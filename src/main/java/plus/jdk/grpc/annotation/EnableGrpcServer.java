package plus.jdk.grpc.annotation;

import org.springframework.context.annotation.Import;
import plus.jdk.grpc.selector.GrpcServerSelector;

import java.lang.annotation.*;

@Import(GrpcServerSelector.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableGrpcServer {

}
