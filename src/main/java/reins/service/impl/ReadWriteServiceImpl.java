package reins.service.impl;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reins.config.GlobalVar;
import reins.domain.AccessRecord;
import reins.domain.FileMeta;
import reins.domain.Node;
import reins.service.MetaDataService;
import reins.service.ReadWriteService;
import reins.service.TradeOffService;
import reins.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ReadWriteServiceImpl implements ReadWriteService {

    @Autowired
    GlobalVar globalVar;

    @Autowired
    TradeOffService tradeOffService;

    @Autowired
    MetaDataService metaDataService;

    @Autowired
    TimeUtil timeUtil;

    @Override
    public String _readWithTimeWindow(String fileName, long timeWindow) {
        if (fileName.contains("_")){
            throw new RuntimeException("File name contains illegal charactor \"_\"");
        }

        Node targetNode;
        if (!selfContainsFile(fileName)){
            Optional<String> forwardNode = metaDataService.getForwardRule(fileName, globalVar.NODE_ID);
            if (forwardNode.isPresent()){
                targetNode = metaDataService.getNodeByName(forwardNode.get())
                        .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", globalVar.NODE_ID)));
            }
            else
                targetNode = tradeOffService._pickNodeToReadWithTimeWindow(fileName, timeWindow);
        }
        else
            targetNode = metaDataService.getNodeByName(globalVar.NODE_ID)
                    .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", globalVar.NODE_ID)));

        _readFromNodeWithTimeWindow(targetNode, fileName, timeWindow);
        return targetNode.getId();
    }


    @Override
    public String read(String fileName) {
        return _readWithTimeWindow(fileName, timeUtil.getCurrentTimeWindow());
    }

    /**
     * 挑选剩余空间更多的节点写入
     * @param file
     * @return
     */
    @Override
    public String write(FileMeta file) {
        if (file.getFileName().contains("_")){
            throw new RuntimeException("File name contains illegal charactor \"_\"");
        }
        Node targetNode = tradeOffService.pickNodeToWrite(file);
        _writeToNode(targetNode, file);
        return targetNode.getId();
    }

    private boolean selfContainsFile(String fileName){
        List<FileMeta> files = metaDataService.getFilesByNode(globalVar.NODE_ID)
                .orElse(new ArrayList<>());
        log.info(JSON.toJSONString(files));
        for (FileMeta file: files){
            if (file.getFileName().equals(fileName))
                return true;
        }
        return false;
    }



    @Override
    public void _writeToNode(Node node, FileMeta file){
        log.info("Writing file {} to node {}", JSON.toJSONString(file), JSON.toJSONString(node));
        String fileName = file.getFileName();
        String nodeId = node.getId();
        /*
         * 更新 Redis 中
         *      1. 节点名 -> 节点磁盘占用的 meta data
         *      2. 节点名_storage -> 节点所存储的文件的列表
         *      3. 文件名_storage -> 文件所在节点的列表
         */
        // 1
        Node newNode = metaDataService.getNodeByName(nodeId)
                .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", nodeId)));
        newNode.getDiskMeta().saveFile(file);
        metaDataService.updateNode(newNode);


        // 2
        List<FileMeta> fileList = metaDataService.getFilesByNode(nodeId)
                .orElse(new ArrayList<>());
        if (!fileList.contains(file)) // replica 的情况，不需要更新全体文件的列表
            fileList.add(file);
        metaDataService.setFilesByNode(nodeId, fileList);

        // 3
        // 可能是个 replica
        List<String> nodeList = metaDataService.getNodesByFile(fileName)
            .orElse(new ArrayList<>(1));
        nodeList.add(nodeId);
        metaDataService.setNodesByFile(fileName, nodeList);

        List<FileMeta> allFiles = metaDataService.getAllFiles().orElse(new ArrayList<>());
        allFiles.add(file);
        metaDataService.setAllFiles(allFiles);
    }

    /**
     * _writeToNode 的逆操作
     */
    @Override
    public void _removeFromNode(Node node, FileMeta file) {
        log.info("Removing file {} from node {}", JSON.toJSONString(file), JSON.toJSONString(node));
        String nodeId = node.getId();
        String fileName = file.getFileName();

        Node newNode = metaDataService.getNodeByName(nodeId)
                .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", nodeId)));
        newNode.getDiskMeta().removeFile(file);
        metaDataService.updateNode(newNode);


        List<String> nodeList = metaDataService.getNodesByFile(fileName)
                .get();
        nodeList.remove(nodeId);
        metaDataService.setNodesByFile(file.getFileName(), nodeList);

        List<FileMeta> fileList = metaDataService.getFilesByNode(nodeId)
                .orElse(new ArrayList<>());
        fileList.remove(file);
        metaDataService.setFilesByNode(nodeId, fileList);


        // 如果这是唯一一个持有数据的节点
        if (nodeList.size() == 1){
            List<FileMeta> allFiles = metaDataService.getAllFiles().orElse(new ArrayList<>());
            allFiles.remove(file);
            metaDataService.setAllFiles(allFiles);
        }
    }

    private int _readFromNodeWithTimeWindow(Node node, String fileName, long timeWindow){
        log.info("Reading file {} at node {} from node {}", fileName, node.getId(), globalVar.NODE_ID);
        String nodeId = globalVar.NODE_ID;

        List<AccessRecord> records = metaDataService.getAccessRecordsByFileAndByNode(fileName, nodeId)
                .orElse(new ArrayList<>());
        int recordSize = records.size();

        if (recordSize == 0){
            records.add(AccessRecord.builder()
                    .timeWindow(timeWindow)
                    .accessAmount(1)
                    .build());
        }
        else {
            AccessRecord lastRecord = records.get(recordSize - 1);
            if (lastRecord.getTimeWindow() == timeWindow){
                lastRecord.increaseAccess();
            }
            else {
                records.add(AccessRecord.builder()
                        .timeWindow(timeWindow)
                        .accessAmount(1)
                        .build());
            }
        }

        // file1_node1 -> [ (timeWindow=1, accessAmount=1), ...]
        metaDataService.setAccessRecordByFileAndByNode(fileName, nodeId, records);
        // file1_storage -> [ node1, ...]
        if (recordSize == 0){
            List<String> nodeNames = metaDataService.getAccessRecordIndexByFile(fileName)
                    .orElse(new ArrayList<>());
            nodeNames.add(nodeId);
            metaDataService.setAccessRecordIndexByFile(fileName, nodeNames);
        }

        return 0;
    }

    /**
     * 从当前节点读取位于 node 的某文件
     * 需要更新当前节点访问该文件的访问记录
     * @param node
     * @param fileName
     * @return
     */
    @Override
    public int _readFromNode(Node node, String fileName){
        return _readFromNodeWithTimeWindow(node, fileName, timeUtil.getCurrentTimeWindow());
    }
}
