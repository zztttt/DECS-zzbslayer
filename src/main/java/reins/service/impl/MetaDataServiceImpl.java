package reins.service.impl;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import reins.config.GlobalVar;
import reins.domain.AccessRecord;
import reins.domain.DiskMeta;
import reins.domain.FakeFile;
import reins.domain.Node;
import reins.service.KeyValueService;
import reins.service.MetaDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@DependsOn("GlobalVar")
@Service
public class MetaDataServiceImpl implements MetaDataService, InitializingBean {
    @Autowired
    KeyValueService keyValueService;

    @Autowired
    GlobalVar globalVar;

    @Autowired
    DiscoveryClient discoveryClient;

    @Override
    public void afterPropertiesSet() {
        // initialize self meta data
        if (!keyValueService.exists(globalVar.NODE_ID)){
            Node self = Node.builder()
                    .id(globalVar.NODE_ID)
                    .diskMeta(DiskMeta.builder()
                            .max(globalVar.DISK_MAX)
                            .available(globalVar.DISK_MAX)
                            .build())
                    .build();
            keyValueService.put(globalVar.NODE_ID, self);
        }
    }

    @Override
    public Optional<List<String>> getNodes() {
        // TODO: 这个 API 会获取到 Dead Node 考虑访问 eureka/apps 解析
        List<String> res = new ArrayList<>();
        List<String> services = discoveryClient.getServices();
        for (String sv: services){
            List<ServiceInstance> instances = discoveryClient.getInstances(sv);
            for (ServiceInstance svi : instances){
                res.add(svi.getInstanceId());
            }
        }
        return Optional.of(res);
    }


    @Override
    public Optional<Node> getNodeByName(String nodeId) {
        Node res;
        if (!keyValueService.exists(nodeId))
            res = null;
        else
            res = keyValueService.get(nodeId, Node.class);
        return Optional.ofNullable(res);
    }

    @Override
    public void updateNode(Node node){
        keyValueService.put(node.getId(), node);
    }

    @Override
    public Optional<List<FakeFile>> getFilesByNode(String nodeId) {
        List<FakeFile> res;

        String storageKey = nodeId + "_storage";
        if (!keyValueService.exists(storageKey))
            res = null;
        else
            res = keyValueService.getList(storageKey, FakeFile.class);
        return Optional.ofNullable(res);
    }

    @Override
    public void setFilesByNode(String nodeName, List<FakeFile> files){
        String storageKey = nodeName + "_storage";
        keyValueService.put(storageKey, files);
    }

    @Override
    public Optional<List<String>> getNodesByFile(String fileName) {
        List<String> res;
        String storageKey = fileName + "_storage";
        if (!keyValueService.exists(storageKey))
            res = null;
        else
            res = keyValueService.getList(storageKey, String.class);
        return Optional.ofNullable(res);
    }

    @Override
    public void setNodesByFile(String fileName, List<String> nodeIds) {
        String storageKey = fileName + "_storage";
        keyValueService.put(storageKey, nodeIds);
    }

    @Override
    public Optional<List<AccessRecord>> getAccessRecordsByFileAndByNode(String fileName, String nodeId) {
        List<AccessRecord> res;
        String recordKey = fileName + "_" + nodeId;
        if (!keyValueService.exists(recordKey))
            res = null;
        else
            res = keyValueService.getList(recordKey, AccessRecord.class);
        return Optional.ofNullable(res);
    }

    @Override
    public void setAccessRecordByFileAndByNode(String fileName, String nodeId, List<AccessRecord> records) {
        String recordKey = fileName + "_" + nodeId;
        keyValueService.put(recordKey, records);
    }
}

