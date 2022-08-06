package plus.jdk.grpc.selector;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import plus.jdk.grpc.config.GrpcPlusProperties;
import plus.jdk.grpc.global.GlobalGrpcInterceptorRegistry;
import plus.jdk.grpc.global.GrpcServerFactory;
import plus.jdk.grpc.global.GrpcServerLifecycle;

@Configuration
public class GrpcServerSelector extends WebApplicationObjectSupport implements BeanFactoryAware, WebMvcConfigurer {

    private BeanFactory beanFactory;

    @Bean
    GrpcServerFactory getGrpcServerFactory(GrpcPlusProperties properties) {
        return new GrpcServerFactory(properties, beanFactory, getApplicationContext());
    }

    @Bean
    GlobalGrpcInterceptorRegistry getGlobalGrpcInterceptorRegistry(GrpcPlusProperties properties) {
        return new GlobalGrpcInterceptorRegistry(properties, getApplicationContext());
    }

    @Bean
    GrpcServerLifecycle getGrpcServerLifecycle(GrpcServerFactory grpcServerFactory) {
        return new GrpcServerLifecycle(grpcServerFactory);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
