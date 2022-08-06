package plus.jdk.grpc.model;

import io.grpc.ServerServiceDefinition;

public class GrpcServiceDefinition {

    private final String beanName;
    private final Class<?> beanClazz;
    private final ServerServiceDefinition definition;

    public GrpcServiceDefinition(final String beanName, final Class<?> beanClazz,
                                 final ServerServiceDefinition definition) {
        this.beanName = beanName;
        this.beanClazz = beanClazz;
        this.definition = definition;
    }

    public String getBeanName() {
        return this.beanName;
    }

    public Class<?> getBeanClazz() {
        return this.beanClazz;
    }

    public ServerServiceDefinition getDefinition() {
        return this.definition;
    }
}