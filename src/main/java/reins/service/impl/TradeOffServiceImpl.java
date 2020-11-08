package reins.service.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reins.config.DecsAlgConfig;
import reins.config.GlobalVar;
import reins.domain.FileMeta;
import reins.domain.Node;
import reins.service.MetaDataService;
import reins.service.PopularityService;
import reins.service.TradeOffService;

import java.util.List;

@Service
public class TradeOffServiceImpl implements TradeOffService {
    @Autowired
    MetaDataService metaDataService;

    @Autowired
    PopularityService popularityService;

    @Autowired
    GlobalVar globalVar;

    @Autowired
    DecsAlgConfig decsAlgConfig;

    @Override
    public Node pickNodeToRead(String fileName) {
        // 从有该文件的节点列表中，选取一个最适合的去读的
        List<String> nodeNames = metaDataService.getNodesByFile(fileName).get();
        double max = Double.MIN_VALUE;
        String targetNode = null;
        if (nodeNames.size() == 1){
            targetNode = nodeNames.get(0);
        }
        else {
            for (String nodeId: nodeNames){
                double readScore = calculateReadScore(nodeId, fileName);
                if (readScore > max){
                    max = readScore;
                    targetNode = nodeId;
                }
            }
        }

        return metaDataService.getNodeByName(targetNode).get();
    }

    @Override
    public Node pickNodeToWrite(FileMeta file) {
        //return metaDataService.getNodeByName(globalVar.NODE_ID).get();
        List<String> nodeNames = metaDataService.getNodes().get();
        double max = Double.MIN_VALUE;
        String targetNode = null;
        for (String nodeId: nodeNames){
            double writeScore = calculateWriteScore(nodeId);
            if (writeScore > max){
                max = writeScore;
                targetNode = nodeId;
            }
        }
        return metaDataService.getNodeByName(targetNode).get();
    }

    private double calculateWriteScore(String nodeId){
        Node node = metaDataService.getNodeByName(nodeId)
                .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", nodeId)));
        // 空闲空间占比
        double r = node.getDiskMeta().getFreeSpaceRatio();
        // TODO: 原始代码其实就没有实现这一项，以后再说
        double p = 0;
        double score = decsAlgConfig.TC * ( decsAlgConfig.ALPHA * r +  (1 - decsAlgConfig.ALPHA) * p)
                / decsAlgConfig.BETA * (decsAlgConfig.TC + decsAlgConfig.TI);
        return score;
    }

    private double calculateReadScore(String nodeId, String fileName){
        double p = popularityService.calculatePopularityByFileAndByNodeForNextHour(fileName, nodeId);
        double score = decsAlgConfig.ALPHA * (1 - p)
                / decsAlgConfig.BETA * (1 + decsAlgConfig.TI / decsAlgConfig.TC);
        return score;
    }

    /**
     * 以下部分均为 replica 策略
     * replica 策略：
     *  计算 replica，migrate，
     */

    // 计算当前节点备份某文件的分数
    private double calculateReplicaScore(String nodeId, String fileName){
        Node node = metaDataService.getNodeByName(nodeId)
                .orElseThrow(() -> new RuntimeException(String.format("Node %s doesn't exist", nodeId)));
        // 1st replica score
        double r = node.getDiskMeta().getFreeSpaceRatio();
        // 有该文件访问记录的节点
        List<String> nodeNames = metaDataService.getAccessRecordIndexByFile(fileName).get();
        // 持有该文件的节点
        List<String> nodeNamesWithData = metaDataService.getNodesByFile(fileName).get();
        double sum = 0;
        // 没有数据的节点中，预测访问量最高的节点为 pj
        double pj = 0;
        // 有数据的节点中，预测访问量最高的节点为 pi
        double pi = 0;

        double maxJ = Double.MIN_VALUE;
        double maxI = Double.MIN_VALUE;
        for (String curNode: nodeNames){
            double p = popularityService.calculatePopularityByFileAndByNodeForNextHour(fileName, nodeId);
            sum += p;
            if (nodeNamesWithData.contains(curNode)){
                if (p > pi){
                    pi = p;
                }
            }
            else {
                if (p > pj){
                    pj = p;
                }
            }
        }
        double pjStar = pj / sum;
        double score = (decsAlgConfig.ALPHA * r + decsAlgConfig.BETA * pjStar)
                / ((pi + pj + 1) * decsAlgConfig.TC + decsAlgConfig.TI)
                    * decsAlgConfig.GAMMA * Math.pow(Math.E, nodeNames.size());
        return score;
    }
}
