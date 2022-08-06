package plus.jdk.grpc.client.factory;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractStub;

public class AsyncStubFactory extends StandardGrpcStubFactory {

    @Override
    public boolean isApplicable(Class<? extends AbstractStub<?>> stubType) {
        return AbstractAsyncStub.class.isAssignableFrom(stubType);
    }

    @Override
    protected String getFactoryMethodName() {
        return "newStub";
    }
}
