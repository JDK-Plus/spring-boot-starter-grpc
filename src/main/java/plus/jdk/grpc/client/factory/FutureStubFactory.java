package plus.jdk.grpc.client;

import io.grpc.stub.AbstractFutureStub;
import io.grpc.stub.AbstractStub;

public class FutureStubFactory extends StandardJavaGrpcStubFactory {

    @Override
    public boolean isApplicable(Class<? extends AbstractStub<?>> stubType) {
        return AbstractFutureStub.class.isAssignableFrom(stubType);
    }

    @Override
    protected String getFactoryMethodName() {
        return "newFutureStub";
    }
}
