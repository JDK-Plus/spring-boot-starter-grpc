package plus.jdk.grpc.client.factory;

import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

public final class FallbackStubFactory extends StandardGrpcStubFactory {

    @Override
    public boolean isApplicable(final Class<? extends AbstractStub<?>> stubType) {
        return true;
    }

    @Override
    public <T extends AbstractStub<T>> AbstractStub<T> createStub(final Class<T> stubType, final Channel channel) {
        try {
            // Search for public static *Grpc#new*Stub(Channel)
            final Class<?> declaringClass = stubType.getDeclaringClass();
            if (declaringClass != null) {
                for (final Method method : declaringClass.getMethods()) {
                    final String name = method.getName();
                    final int modifiers = method.getModifiers();
                    final Parameter[] parameters = method.getParameters();
                    if (name.startsWith("new") && name.endsWith("Stub")
                            && Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
                            && method.getReturnType().isAssignableFrom(stubType)
                            && parameters.length == 1
                            && Channel.class.equals(parameters[0].getType())) {
                        return AbstractStub.class.cast(method.invoke(null, channel));
                    }
                }
            }

            // Search for a public constructor *Stub(Channel)
            final Constructor<T> constructor = stubType.getConstructor(Channel.class);
            return constructor.newInstance(channel);

        } catch (final Exception e) {
            throw new BeanInstantiationException(stubType, "Failed to create gRPC client via FallbackStubFactory", e);
        }
    }
}
