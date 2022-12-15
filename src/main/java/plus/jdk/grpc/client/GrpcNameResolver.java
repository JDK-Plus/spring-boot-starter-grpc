package plus.jdk.grpc.client;

import com.google.common.collect.ImmutableList;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import org.springframework.util.CollectionUtils;
import plus.jdk.grpc.config.GrpcPlusClientProperties;
import plus.jdk.grpc.model.GrpcNameResolverModel;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GrpcNameResolver extends NameResolver {


    private ResolutionResult result;

    private final URI targetUri;

    private NameResolver.Listener2 listener;

    private final GrpcPlusClientProperties properties;

    private List<EquivalentAddressGroup> addressGroupList = new ArrayList<>();

    private final Collection<INameResolverConfigurer> nameResolverConfigurers;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

    public GrpcNameResolver(GrpcPlusClientProperties properties, URI targetUri,
                            GrpcNameResolverModel nameResolver,
                            Collection<INameResolverConfigurer> nameResolverConfigurers) {
        this.nameResolverConfigurers = nameResolverConfigurers;
        this.properties = properties;
        this.addressGroupList = nameResolver.toEquivalentAddressGroups();
        this.targetUri = targetUri;
        this.result = ResolutionResult.newBuilder()
                .setAddresses(ImmutableList.copyOf(addressGroupList))
                .build();
    }

    @Override
    public String getServiceAuthority() {
        if(targetUri.getAuthority() == null) {
            return targetUri.getHost();
        }
        return targetUri.getAuthority();
    }

    @Override
    public void start(final Listener2 listener) {
        this.listener = listener;
        this.listener.onResult(this.result);
        if(CollectionUtils.isEmpty(nameResolverConfigurers)) {
            return;
        }
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            List<EquivalentAddressGroup> results = new ArrayList<>();
            for(INameResolverConfigurer configurer: nameResolverConfigurers) {
                results.addAll(configurer.configurationName(targetUri));
            }
            if(results.size() == 0) {
                return;
            }
            this.addressGroupList = results;
            this.result = ResolutionResult.newBuilder()
                    .setAddresses(ImmutableList.copyOf(addressGroupList))
                    .build();
            this.listener.onResult(this.result);
        }, properties.getNameRefreshRate(), properties.getNameRefreshRate(), TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        scheduledExecutorService.shutdown();
    }
}