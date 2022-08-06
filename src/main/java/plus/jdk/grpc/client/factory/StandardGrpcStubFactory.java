package plus.jdk.grpc.client.factory;

/*
 * Copyright (c) 2016-2021 Michael Zhang <yidongnan@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.lang.reflect.Method;

import org.springframework.beans.BeanInstantiationException;

import io.grpc.Channel;
import io.grpc.stub.AbstractStub;

/**
 * A factory for creating stubs provided by standard grpc Java library. This is an abstract super-type that can be
 * extended to support the different provided types.
 */
public abstract class StandardGrpcStubFactory {

    public <T extends AbstractStub<T>> AbstractStub<T> createStub(final Class<T> stubType, final Channel channel) {
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
    protected String getFactoryMethodName() {
        return null;
    };

    /**
     * Used to resolve a factory that matches the particular stub type.
     *
     * @param stubType The type of the stub that needs to be created.
     * @return True if this particular factory is capable of creating instances of this stub type. False otherwise.
     */
    public abstract boolean isApplicable(Class<? extends AbstractStub<?>> stubType);

}
