package plus.jdk.grpc.model;

import io.grpc.EquivalentAddressGroup;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Data
public class GrpcNameResolverModel {

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

    public List<EquivalentAddressGroup> toEquivalentAddressGroups() {
        List<EquivalentAddressGroup> addressGroupList = new ArrayList<>();
        for(String host: getHosts()) {
            String[] tempArr = host.split(":");
            if(tempArr.length == 2 && tempArr[1].matches("[0-9]+")) {
                int port = Integer.parseInt(tempArr[1]);
                String hostAddress = tempArr[0];
                addressGroupList.add(new EquivalentAddressGroup(new InetSocketAddress(hostAddress, port)));
            }
        }
        return addressGroupList;
    }
}
