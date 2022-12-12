package plus.jdk.grpc.client;

import com.google.common.collect.ImmutableList;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import plus.jdk.grpc.config.GrpcPlusClientProperties;
import plus.jdk.grpc.model.GrpcNameResolver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class StaticNameResolver extends NameResolver {


    private final ResolutionResult result;

    private final URI targetUri;

    private NameResolver.Listener2 listener;

    private List<EquivalentAddressGroup> addressGroupList = new ArrayList<>();

    public StaticNameResolver(GrpcPlusClientProperties properties, URI targetUri, GrpcNameResolver nameResolver) {
        for(String host: nameResolver.getHosts()) {
            String[] tempArr = host.split(":");
            if(tempArr.length == 2 && tempArr[1].matches("[0-9]+")) {
                int port = Integer.parseInt(tempArr[1]);
                String hostAddress = tempArr[0];
                addressGroupList.add(new EquivalentAddressGroup(new InetSocketAddress(hostAddress, port)));
            }
        }
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
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void refresh() {

    }
}