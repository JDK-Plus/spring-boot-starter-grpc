package plus.jdk.grpc.client;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractStub;

public class AsyncStubFactory extends StandardJavaGrpcStubFactory {

    @Override
    public boolean isApplicable(Class<? extends AbstractStub<?>> stubType) {
        return AbstractAsyncStub.class.isAssignableFrom(stubType);
    }

    @Override
    protected String getFactoryMethodName() {
        return "newStub";
    }
}
