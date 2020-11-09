package reins.service.impl;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import reins.config.DecsAlgConfig;
import reins.config.GlobalVar;
import reins.domain.*;
import reins.service.KeyValueService;
import reins.service.MetaDataService;
import reins.utils.KeyUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

@DependsOn("GlobalVar")
@Service
public class MetaDataServiceImpl implements MetaDataService, InitializingBean {
    @Autowired
    KeyValueService keyValueService;

    @Autowired
    GlobalVar globalVar;

    @Autowired
    DecsAlgConfig decsAlgConfig;

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
        return getOptional(nodeId, Node.class);
    }

    @Override
    public void updateNode(Node node){
        keyValueService.put(node.getId(), node);
    }

    @Override
    public Optional<List<FileMeta>> getFilesByNode(String nodeId) {
        String storageKey = KeyUtil.generateKey(nodeId, "storage");
        return getOptionalList(storageKey, FileMeta.class);
    }

    @Override
    public void setFilesByNode(String nodeId, List<FileMeta> files){
        String storageKey = KeyUtil.generateKey(nodeId, "storage");
        keyValueService.put(storageKey, files);
    }

    @Override
    public Optional<List<String>> getNodesByFile(String fileName) {
        String storageKey = KeyUtil.generateKey(fileName, "storage");
        return getOptionalList(storageKey, String.class);
    }

    @Override
    public void setNodesByFile(String fileName, List<String> nodeIds) {
        String storageKey = KeyUtil.generateKey(fileName, "storage");
        keyValueService.put(storageKey, nodeIds);
    }

    @Override
    public Optional<List<AccessRecord>> getAccessRecordsByFileAndByNode(String fileName, String nodeId) {
        String recordKey = KeyUtil.generateKey(fileName, nodeId);
        return getOptionalList(recordKey, AccessRecord.class);
    }

    @Override
    public void setAccessRecordByFileAndByNode(String fileName, String nodeId, List<AccessRecord> records) {
        String recordKey = fileName + "_" + nodeId;
        keyValueService.put(recordKey, records);
    }

    @Override
    public Optional<List<String>> getAccessRecordIndexByFile(String fileName) {
        String indexKey = KeyUtil.generateKey(fileName, "acIndex");
        return getOptionalList(indexKey, String.class);
    }

    @Override
    public void setAccessRecordIndexByFile(String fileName, List<String> nodeNames) {
        String indexKey = KeyUtil.generateKey(fileName, "acIndex");
        keyValueService.put(indexKey, nodeNames);
    }

    @Override
    public Optional<List<FileMeta>> getAllFiles() {
        String key = KeyUtil.generateInternalKey("allFiles");
        return getOptionalList(key, FileMeta.class);
    }

    @Override
    public void setAllFiles(List<FileMeta> files) {
        String key = KeyUtil.generateInternalKey("allFiles");
        keyValueService.put(key, files);
    }

    @Override
    public Optional<String> getForwardRule(String fileName, String nodeId) {
        return Optional.empty();
    }

    @Override
    public void setForwardRule(String fileName, String srcNode, String dstNode) {
        String key = KeyUtil.generateInternalKey(fileName, srcNode, "forwardRule");
        keyValueService.put(key, dstNode,
                decsAlgConfig.TIME_WINDOW, TimeUnit.MINUTES
        );
    }

    @Override
    public Optional<Map<String, Map<String, Double>>> getPredictionResultByHour(long hour) {
        String key = KeyUtil.generateInternalKey("predictionResult", Long.toString(hour));
        Map<String, Map<String, Double>> res;
        if (!keyValueService.exists(key))
            res = null;
        else
            // JSON parsed Object can be converted to map directly
            res = (Map<String, Map<String, Double>>)JSON.parse(keyValueService.get(key));
        return Optional.ofNullable(res);
    }

    @Override
    public void setPredictionResultByHour(long hour, Map<String, Map<String, Double>> predictionResult) {
        String key = KeyUtil.generateInternalKey("predictionResult", Long.toString(hour));
        keyValueService.put(key, predictionResult
                , decsAlgConfig.TIME_WINDOW, TimeUnit.MINUTES);
    }

    @Override
    public Optional<PCF> getPCFByFileAndByNode(String fileName, String nodeId) {
        String key = KeyUtil.generateInternalKey(fileName, nodeId, "PCF");
        return getOptional(key, PCF.class);
    }

    @Override
    public void setPCFByFileAndByNode(String fileName, String nodeId, PCF pcfs) {
        String key = KeyUtil.generateInternalKey(fileName, nodeId, "PCF");
        keyValueService.put(key, pcfs);
    }

    private <T> Optional<T> getOptional(String key, Class<T> clazz){
        if (key == null || !keyValueService.exists(key)){
            return Optional.empty();
        }
        else
            return Optional.of(keyValueService.get(key, clazz));
    }

    private <T> Optional<List<T>> getOptionalList(String key, Class<T> clazz){
        if (key == null || !keyValueService.exists(key)){
            return Optional.empty();
        }
        else
            return Optional.of(keyValueService.getList(key, clazz));
    }

    public static void main(String[] args){
        Map<String, Map<String, Double>> test = new HashMap<>();
        Map<String, Double> item = new HashMap<>();
        item.put("test", 1.0);
        test.put("test", item);
        String json = JSON.toJSONString(test);
        System.out.println(json);
        Map<String, Map<String, Double>> revert = (Map) JSON.parse(json);
        System.out.println(revert.get("test").get("test"));
    }
}

