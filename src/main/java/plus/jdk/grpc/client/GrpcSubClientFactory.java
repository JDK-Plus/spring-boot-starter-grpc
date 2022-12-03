package plus.jdk.grpc.client;


import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import plus.jdk.grpc.client.factory.FallbackStubFactory;
import plus.jdk.grpc.client.factory.StandardGrpcStubFactory;

import java.util.ArrayList;
import java.util.List;

public class GrpcSubClientFactory {

    private final ApplicationContext applicationContext;

    private List<StandardGrpcStubFactory> stubFactories = null;

    private final ConfigurableBeanFactory configurableBeanFactory;

    public GrpcSubClientFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.configurableBeanFactory = ((ConfigurableApplicationContext) this.applicationContext).getBeanFactory();
    }

    public <T extends AbstractStub<T>>
    String getBeanName(final Class<T> stubClass) {
        return stubClass.getName();
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, final Channel channel) {
        final StandardGrpcStubFactory factory = getStubFactories().stream()
                .filter(stubFactory -> stubFactory.isApplicable(stubClass))
                .findFirst()
                .orElseThrow(() -> new BeanInstantiationException(stubClass,
                        "Unsupported stub type: " + stubClass.getName() + " -> Please report this issue."));
        try {
            String beanName = getBeanName(stubClass);
            if(configurableBeanFactory.containsBean(beanName)) {
                return configurableBeanFactory.getBean(stubClass);
            }
            T stub;
            synchronized (stubClass) {
                if(configurableBeanFactory.containsBean(beanName)) {
                    return configurableBeanFactory.getBean(stubClass);
                }
                stub = stubClass.cast(factory.createStub(stubClass, channel));
                configurableBeanFactory.registerSingleton(stubClass.getName(), stub);
            }
            return stub;
        } catch (final Exception exception) {
            throw new BeanInstantiationException(stubClass, "Failed to create gRPC stub of type " + stubClass.getName(),
                    exception);
        }
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, String address, Integer port, ClientInterceptor... interceptors) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(address, port).intercept(interceptors);
        channelBuilder.usePlaintext();
        ManagedChannel channel = channelBuilder.build();
        return createStub(stubClass, channel);
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, String address, ClientInterceptor... interceptors) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        return createStub(stubClass, channel);
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, ManagedChannelBuilder<?> channelBuilder) {
        ManagedChannel channel = channelBuilder.build();
        return createStub(stubClass, channel);
    }

    private List<StandardGrpcStubFactory> getStubFactories() {
        if (this.stubFactories == null) {
            this.stubFactories = new ArrayList<>(this.applicationContext.getBeansOfType(StandardGrpcStubFactory.class).values());
            this.stubFactories.add(new FallbackStubFactory());
        }
        return this.stubFactories;
    }
}
