package plus.jdk.grpc.selector;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import plus.jdk.grpc.client.factory.AsyncStubFactory;
import plus.jdk.grpc.client.factory.BlockingStubFactory;
import plus.jdk.grpc.client.factory.FutureStubFactory;
import plus.jdk.grpc.client.GrpcSubClientFactory;

@Configuration
public class GrpcClientSelector extends WebApplicationObjectSupport implements BeanFactoryAware, WebMvcConfigurer {

    private BeanFactory beanFactory;

    @Bean
    GrpcSubClientFactory grpcClientBeanPostProcessor() {
        return new GrpcSubClientFactory(getApplicationContext());
    }

    @Bean
    AsyncStubFactory asyncStubFactory() {
        return new AsyncStubFactory();
    }

    @Bean
    BlockingStubFactory blockingStubFactory() {
        return new BlockingStubFactory();
    }

    @Bean
    FutureStubFactory futureStubFactory() {
        return new FutureStubFactory();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
