package plus.jdk.grpc.client;


import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.context.ApplicationContext;
import plus.jdk.grpc.client.factory.FallbackStubFactory;
import plus.jdk.grpc.client.factory.StandardGrpcStubFactory;

import java.util.ArrayList;
import java.util.List;

public class GrpcClientFactory {

    private final ApplicationContext applicationContext;

    private List<StandardGrpcStubFactory> stubFactories = null;

    public GrpcClientFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private AbstractStub<?> createStub(final Class<? extends AbstractStub<?>> stubClass, final Channel channel) {
        final StandardGrpcStubFactory factory = getStubFactories().stream()
                .filter(stubFactory -> stubFactory.isApplicable(stubClass))
                .findFirst()
                .orElseThrow(() -> new BeanInstantiationException(stubClass,
                        "Unsupported stub type: " + stubClass.getName() + " -> Please report this issue."));

        try {
            return factory.createStub(stubClass, channel);
        } catch (final Exception exception) {
            throw new BeanInstantiationException(stubClass, "Failed to create gRPC stub of type " + stubClass.getName(),
                    exception);
        }
    }

    private List<StandardGrpcStubFactory> getStubFactories() {
        if (this.stubFactories == null) {
            this.stubFactories = new ArrayList<>(this.applicationContext.getBeansOfType(StandardGrpcStubFactory.class).values());
            this.stubFactories.add(new FallbackStubFactory());
        }
        return this.stubFactories;
    }
}
