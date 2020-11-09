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
import reins.utils.TimeUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        long now = TimeUtil.getCurrentAbsoluteHour();
        Optional<Map<String, Map<String, Double>>> optionalResult = metaDataService.getPredictionResultByHour(now);

        double p;
        if (optionalResult.isPresent() &&
            optionalResult.get().containsKey(fileName) &&
            optionalResult.get().get(fileName).containsKey(nodeId)){
            p = optionalResult.get().get(fileName).get(nodeId);
        }
        else
            p = popularityService.calculatePopularityByFileAndByNodeForHour(fileName, nodeId, now);

        double score = decsAlgConfig.ALPHA * (1 - p)
                / decsAlgConfig.BETA * (1 + decsAlgConfig.TI / decsAlgConfig.TC);
        return score;
    }

}
