package plus.jdk.grpc.annotation;

import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

@Bean
@Service
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcService {

    Class<? extends ServerInterceptor>[] interceptors() default {};

    /**
     * Whether the custom interceptors should be mixed with the global interceptors and sorted afterwards. Use this
     * option if you want to add a custom interceptor between global interceptors.
     *
     * @return True, if the custom interceptors should be merged with the global ones and sorted afterwards. False
     *         otherwise.
     */
    boolean sortInterceptors() default false;

}
