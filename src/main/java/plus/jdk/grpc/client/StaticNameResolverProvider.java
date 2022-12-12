package plus.jdk.grpc.client;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import plus.jdk.grpc.config.GrpcPlusClientProperties;
import plus.jdk.grpc.model.GrpcNameResolver;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class StaticNameResolverProvider  extends NameResolverProvider {

    private final GrpcPlusClientProperties properties;

    private String scheme = "grpc";

    private final Map<String, GrpcNameResolver> nameResolverMap = new HashMap<>();


    public StaticNameResolverProvider(GrpcPlusClientProperties grpcClientProperties, String scheme) {
        this.properties = grpcClientProperties;
        this.scheme = scheme;
        for(GrpcNameResolver nameResolver: properties.getResolvers()) {
            if(!nameResolver.getScheme().equals(scheme)) {
                continue;
            }
            nameResolverMap.put(nameResolver.getServiceName(), nameResolver);
        }
    }

    private String getResolverKey(String scheme, String serviceName) {
        return String.format("%s://%s", scheme, serviceName);
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        GrpcNameResolver nameResolver = nameResolverMap.get(targetUri.getHost());
        if(!(targetUri.getScheme().equals(scheme)) || nameResolver == null) {
            return null;
        }
        return new StaticNameResolver(properties, targetUri, nameResolver);
    }

    @Override
    public String getDefaultScheme() {
        return scheme;
    }
}
