package plus.jdk.grpc.client;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import plus.jdk.grpc.config.GrpcPlusClientProperties;
import plus.jdk.grpc.model.GrpcNameResolverModel;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GrpcNameResolverProvider extends NameResolverProvider {

    private final GrpcPlusClientProperties properties;

    private String scheme = "grpc";

    private final Map<String, GrpcNameResolverModel> nameResolverMap = new HashMap<>();

    private final Collection<INameResolverConfigurer> nameResolverConfigurers;


    public GrpcNameResolverProvider(GrpcPlusClientProperties grpcClientProperties,
                                    String scheme, Collection<INameResolverConfigurer> nameResolverConfigurers) {
        this.properties = grpcClientProperties;
        this.scheme = scheme;
        this.nameResolverConfigurers = nameResolverConfigurers;
        for(GrpcNameResolverModel nameResolver: properties.getResolvers()) {
            if(!nameResolver.getScheme().equals(scheme)) {
                continue;
            }
            nameResolverMap.put(nameResolver.getServiceName(), nameResolver);
        }
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
        GrpcNameResolverModel nameResolverModel = nameResolverMap.get(targetUri.getHost());
        if(!(targetUri.getScheme().equals(scheme)) || nameResolverModel == null) {
            return null;
        }
        return new GrpcNameResolver(properties, targetUri, nameResolverModel, nameResolverConfigurers);
    }

    @Override
    public String getDefaultScheme() {
        return scheme;
    }
}
