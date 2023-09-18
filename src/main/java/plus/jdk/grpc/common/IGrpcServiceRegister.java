package plus.jdk.grpc.common;

public interface IGrpcServiceRegister {

    /**
     * 服务启动时调用，用于注册节点
     */
    void registerServiceNode();

    /**
     * 服务运营过程中不断地去更新节点信息，默认每5秒调用一次
     * 例如向etcd等节点中写入活跃记录，保证其他节点知道该节点是运行中的
     */
    void updateNodeStatus();

    /**
     * 服务关闭时调用, 用户摘除节点
     */
    void deregisterServiceNode();
}
