package plus.jdk.grpc.model;

import lombok.Data;

import java.util.List;

@Data
public class GrpcNameResolver {

    /**
     * grpc scheme地址
     */
    private String scheme = "grpc";

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 主机列表
     */
    private List<String> hosts;
}
