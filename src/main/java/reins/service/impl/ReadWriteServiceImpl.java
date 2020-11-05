package reins.service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reins.config.GlobalVar;
import reins.domain.AccessRecord;
import reins.domain.FakeFile;
import reins.domain.Node;
import reins.service.MetaDataService;
import reins.service.ReadWriteService;
import reins.service.TradeOffService;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReadWriteServiceImpl implements ReadWriteService {

    @Autowired
    GlobalVar globalVar;

    @Autowired
    TradeOffService tradeOffService;

    @Autowired
    MetaDataService metaDataService;

    @Override
    public String read(String fileName) {
        Node targetNode;
        if (!selfContainsFile(fileName)){
            targetNode = tradeOffService.pickNodeToRead(fileName);
        }
        else
            targetNode = metaDataService.getNodeByName(globalVar.NODE_ID)
                    .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", globalVar.NODE_ID)));

        _readFromNode(targetNode, fileName);
        return targetNode.getId();
    }

    @Override
    public String write(FakeFile file) {
        Node targetNode = tradeOffService.pickNodeToWrite(file);
        _writeToNode(targetNode, file);
        return targetNode.getId();
    }

    private boolean selfContainsFile(String fileName){
        List<FakeFile> files = metaDataService.getFilesByNode(globalVar.NODE_ID)
                .orElse(new ArrayList<>());
        for (FakeFile file: files){
            if (file.getFileName().equals(fileName))
                return true;
        }
        return false;
    }



    private void _writeToNode(Node node, FakeFile file){
        String fileName = file.getFileName();
        String nodeName = node.getId();
        /*
         * 更新 Redis 中
         *      1. 节点名 -> 节点磁盘占用的 meta data
         *      2. 节点名_storage -> 节点所存储的文件的列表
         *      3. 文件名_storage -> 文件所在节点的列表
         */
        // 1
        Node newNode = metaDataService.getNodeByName(nodeName)
                .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", nodeName)));
        newNode.getDiskMeta().saveFile(file);
        metaDataService.updateNode(newNode);


        // 2
        List<FakeFile> fileList = metaDataService.getFilesByNode(nodeName)
                .orElse(new ArrayList<>());
        fileList.add(file);
        metaDataService.setFilesByNode(nodeName, fileList);

        // 3
//        if (keyValueService.exists(fileName)){
//            /*
//             * TODO: 更新文件
//             * 但是一旦有 replica 就要同时去更新 replica
//             * 暂时不考虑更新文件的情况
//             */
//            return -1;
//        }

        List<String> nodeList = new ArrayList<>(1);
        nodeList.add(nodeName);
        metaDataService.setNodesByFile(fileName, nodeList);

    }



    private int _readFromNode(Node node, String fileName){
        String nodeName = node.getId();
        String key = fileName + "_" + nodeName;
        Long currentHour = System.currentTimeMillis() / 1000 / (60 * 60);

        List<AccessRecord> records = metaDataService.getAccessRecordsByFileAndByNode(fileName, nodeName)
                .orElse(new ArrayList<>());
        int recordSize = records.size();

        if (recordSize == 0){
            records.add(AccessRecord.builder()
            .hour(currentHour)
            .accessAmount(1)
            .build());
        }
        else {
            AccessRecord lastRecord = records.get(recordSize - 1);
            if (lastRecord.getHour() == currentHour){
                lastRecord.increaseAccess();
            }
            else {
                records.add(AccessRecord.builder()
                        .hour(currentHour)
                        .accessAmount(1)
                        .build());
            }
        }

        metaDataService.setAccessRecordByFileAndByNode(fileName, nodeName, records);
        return 0;
    }
}
