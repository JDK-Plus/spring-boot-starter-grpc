package plus.jdk.grpc.client.factory;


import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;

public class BlockingStubFactory extends StandardGrpcStubFactory {

    @Override
    public boolean isApplicable(Class<? extends AbstractStub<?>> stubType) {
        return AbstractBlockingStub.class.isAssignableFrom(stubType);
    }

    @Override
    protected String getFactoryMethodName() {
        return "newBlockingStub";
    }
}
