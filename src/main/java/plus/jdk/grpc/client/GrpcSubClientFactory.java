package plus.jdk.grpc.client;


import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.context.ApplicationContext;
import plus.jdk.grpc.client.factory.FallbackStubFactory;
import plus.jdk.grpc.client.factory.StandardGrpcStubFactory;

import java.util.ArrayList;
import java.util.List;

public class GrpcSubClientFactory {

    private final ApplicationContext applicationContext;

    private List<StandardGrpcStubFactory> stubFactories = null;

    public GrpcSubClientFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, final Channel channel) {
        final StandardGrpcStubFactory factory = getStubFactories().stream()
                .filter(stubFactory -> stubFactory.isApplicable(stubClass))
                .findFirst()
                .orElseThrow(() -> new BeanInstantiationException(stubClass,
                        "Unsupported stub type: " + stubClass.getName() + " -> Please report this issue."));
        try {
            return stubClass.cast(factory.createStub(stubClass, channel));
        } catch (final Exception exception) {
            throw new BeanInstantiationException(stubClass, "Failed to create gRPC stub of type " + stubClass.getName(),
                    exception);
        }
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, String address, Integer port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();
        return createStub(stubClass, channel);
    }

    public <T extends AbstractStub<T>>
    T createStub(final Class<T> stubClass, String address) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
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
