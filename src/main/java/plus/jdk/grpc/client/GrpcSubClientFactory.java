package plus.jdk.grpc.client;


import io.grpc.*;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import plus.jdk.grpc.client.factory.FallbackStubFactory;
import plus.jdk.grpc.client.factory.StandardGrpcStubFactory;
import plus.jdk.grpc.config.GrpcPlusClientProperties;
import plus.jdk.grpc.model.GrpcNameResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class GrpcSubClientFactory {

    private final ApplicationContext applicationContext;

    private List<StandardGrpcStubFactory> stubFactories = null;

    private final GrpcPlusClientProperties properties;

    private final ConfigurableBeanFactory configurableBeanFactory;

    public GrpcSubClientFactory(ApplicationContext applicationContext, GrpcPlusClientProperties properties) {
        this.applicationContext = applicationContext;
        this.configurableBeanFactory = ((ConfigurableApplicationContext) this.applicationContext).getBeanFactory();
        this.properties = properties;
        HashMap<String, GrpcNameResolver> nameResolverMap = new HashMap<>();
        for(GrpcNameResolver nameResolver: properties.getResolvers()) {
            if(nameResolverMap.containsKey(nameResolver.getScheme())) {
                continue;
            }
            NameResolverRegistry.getDefaultRegistry().register(new StaticNameResolverProvider(properties, nameResolver.getScheme()));
            nameResolverMap.put(nameResolver.getScheme(), nameResolver);
        }
    }

    public <T extends AbstractStub<T>>
    String getBeanName(final Class<T> stubClass) {
        return stubClass.getName();
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, final ManagedChannelBuilder<?> channelBuilder) {
        final StandardGrpcStubFactory factory = getStubFactories().stream()
                .filter(stubFactory -> stubFactory.isApplicable(stubClass))
                .findFirst()
                .orElseThrow(() -> new BeanInstantiationException(stubClass,
                        "Unsupported stub type: " + stubClass.getName() + " -> Please report this issue."));
        try {
            String beanName = getBeanName(stubClass);
            if (configurableBeanFactory.containsBean(beanName)) {
                return configurableBeanFactory.getBean(stubClass);
            }
            T stub;
            synchronized (stubClass) {
                if (configurableBeanFactory.containsBean(beanName)) {
                    return configurableBeanFactory.getBean(stubClass);
                }
                channelBuilder.usePlaintext();
                ManagedChannel channel = channelBuilder.build();
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
        return createStub(stubClass, channelBuilder);
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, String address, ClientInterceptor... interceptors) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(address);
        channelBuilder.usePlaintext();
        return createStub(stubClass, channelBuilder);
    }

    private List<StandardGrpcStubFactory> getStubFactories() {
        if (this.stubFactories == null) {
            this.stubFactories = new ArrayList<>(this.applicationContext.getBeansOfType(StandardGrpcStubFactory.class).values());
            this.stubFactories.add(new FallbackStubFactory());
        }
        return this.stubFactories;
    }
}
