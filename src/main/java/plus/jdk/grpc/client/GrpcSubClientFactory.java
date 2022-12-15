package plus.jdk.grpc.client;


import io.grpc.*;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import plus.jdk.grpc.client.factory.FallbackStubFactory;
import plus.jdk.grpc.client.factory.StandardGrpcStubFactory;
import plus.jdk.grpc.config.GrpcPlusClientProperties;
import plus.jdk.grpc.global.GrpcClientInterceptorConfigurer;
import plus.jdk.grpc.model.GrpcNameResolverModel;

import java.util.*;

public class GrpcSubClientFactory {

    private final ApplicationContext applicationContext;

    private List<StandardGrpcStubFactory> stubFactories = null;

    private final GrpcPlusClientProperties properties;

    private final ConfigurableBeanFactory configurableBeanFactory;

    private final Collection<INameResolverConfigurer> nameResolverConfigurers;

    public GrpcSubClientFactory(ApplicationContext applicationContext, GrpcPlusClientProperties properties) {
        this.applicationContext = applicationContext;
        this.configurableBeanFactory = ((ConfigurableApplicationContext) this.applicationContext).getBeanFactory();
        this.properties = properties;
        this.nameResolverConfigurers = applicationContext.getBeansOfType(INameResolverConfigurer.class).values();
        for (INameResolverConfigurer configurer : nameResolverConfigurers) {
            configurer.configureNameResolvers(properties.getResolvers());
        }
        HashMap<String, GrpcNameResolverModel> nameResolverMap = new HashMap<>();
        for (GrpcNameResolverModel nameResolver : properties.getResolvers()) {
            if (nameResolverMap.containsKey(nameResolver.getScheme())) {
                continue;
            }
            NameResolverRegistry.getDefaultRegistry().register(new GrpcNameResolverProvider(properties, nameResolver.getScheme(), nameResolverConfigurers));
            nameResolverMap.put(nameResolver.getScheme(), nameResolver);
        }
    }

    public <T extends AbstractStub<T>>
    String getBeanName(final Class<T> stubClass, String address) {
        return stubClass.getName() + "->" + address;
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, final ManagedChannelBuilder<?> channelBuilder, String address) {
        final StandardGrpcStubFactory factory = getStubFactories().stream()
                .filter(stubFactory -> stubFactory.isApplicable(stubClass))
                .findFirst()
                .orElseThrow(() -> new BeanInstantiationException(stubClass,
                        "Unsupported stub type: " + stubClass.getName() + " -> Please report this issue."));
        try {
            String beanName = getBeanName(stubClass, address);
            if (configurableBeanFactory.containsBean(beanName)) {
                return configurableBeanFactory.getBean(stubClass);
            }
            T stub;
            synchronized (stubClass) {
                if (configurableBeanFactory.containsBean(beanName)) {
                    return configurableBeanFactory.getBean(stubClass);
                }
                List<ClientInterceptor> interceptors = new ArrayList<>();
                for (GrpcClientInterceptorConfigurer configurer : this.applicationContext
                        .getBeansOfType(GrpcClientInterceptorConfigurer.class).values()) {
                    configurer.configureClientInterceptors(interceptors);
                }
                channelBuilder.intercept(interceptors);
                channelBuilder.usePlaintext();
                ManagedChannel channel = channelBuilder.build();
                stub = stubClass.cast(factory.createStub(stubClass, channel));
                configurableBeanFactory.registerSingleton(beanName, stub);
            }
            return stub;
        } catch (final Exception exception) {
            throw new BeanInstantiationException(stubClass, "Failed to create gRPC stub of type " + stubClass.getName(),
                    exception);
        }
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, String address, ClientInterceptor... interceptors) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(address);
        channelBuilder.usePlaintext();
        return createStub(stubClass, channelBuilder, address);
    }

    private List<StandardGrpcStubFactory> getStubFactories() {
        if (this.stubFactories == null) {
            this.stubFactories = new ArrayList<>(this.applicationContext.getBeansOfType(StandardGrpcStubFactory.class).values());
            this.stubFactories.add(new FallbackStubFactory());
        }
        return this.stubFactories;
    }
}
