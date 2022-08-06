package plus.jdk.grpc.client;

import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;

import java.lang.reflect.Method;

public abstract class GrpcClientStubFactory {

    public AbstractStub<?> createStub(final Class<? extends AbstractStub<?>> stubType, final Channel channel) {
        try {
            // Use the public static factory method
            final String methodName = getFactoryMethodName();
            final Class<?> enclosingClass = stubType.getEnclosingClass();
            final Method factoryMethod = enclosingClass.getMethod(methodName, Channel.class);
            return stubType.cast(factoryMethod.invoke(null, channel));
        } catch (final Exception e) {
            throw new BeanInstantiationException(stubType, "Failed to create gRPC client", e);
        }
    }

    /**
     * Derives the name of the factory method from the given stub type.
     *
     * @return The name of the factory method.
     */
    protected abstract String getFactoryMethodName();
}
