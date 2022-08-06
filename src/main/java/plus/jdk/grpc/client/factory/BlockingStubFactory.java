package plus.jdk.grpc.client;


import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;

public class BlockingStubFactory extends StandardJavaGrpcStubFactory {

    @Override
    public boolean isApplicable(Class<? extends AbstractStub<?>> stubType) {
        return AbstractBlockingStub.class.isAssignableFrom(stubType);
    }

    @Override
    protected String getFactoryMethodName() {
        return "newBlockingStub";
    }
}
