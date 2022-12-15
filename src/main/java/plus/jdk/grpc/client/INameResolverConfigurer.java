package plus.jdk.grpc.client;


import io.grpc.EquivalentAddressGroup;
import plus.jdk.grpc.model.GrpcNameResolverModel;

import java.net.URI;
import java.util.List;

public interface INameResolverConfigurer {

    /**
     * 刷新对应的uri对应的实例列表, 默认每10秒执行一次
     */
    List<EquivalentAddressGroup> configurationName(URI targetUri);

    /**
     * 初始化所有自定义的地址
     */
    void configureNameResolvers(List<GrpcNameResolverModel> resolverModels);
}
