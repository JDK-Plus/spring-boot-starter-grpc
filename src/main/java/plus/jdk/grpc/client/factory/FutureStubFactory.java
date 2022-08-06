package plus.jdk.grpc.client.factory;

import io.grpc.stub.AbstractFutureStub;
import io.grpc.stub.AbstractStub;

public class FutureStubFactory extends StandardGrpcStubFactory {

    @Override
    public boolean isApplicable(Class<? extends AbstractStub<?>> stubType) {
        return AbstractFutureStub.class.isAssignableFrom(stubType);
    }

    @Override
    protected String getFactoryMethodName() {
        return "newFutureStub";
    }
}
