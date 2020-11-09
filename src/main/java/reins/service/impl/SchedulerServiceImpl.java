package reins.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reins.config.AdaptiveAlgConfig;
import reins.config.DecsAlgConfig;
import reins.config.GlobalVar;
import reins.domain.AccessRecord;
import reins.domain.FileMeta;
import reins.domain.Node;
import reins.domain.PCF;
import reins.service.MetaDataService;
import reins.service.PopularityService;
import reins.service.ReadWriteService;
import reins.utils.TimeUtil;

import java.util.*;

@Slf4j
@Service
public class SchedulerServiceImpl {

    @Autowired
    GlobalVar globalVar;

    @Autowired
    DecsAlgConfig decsAlgConfig;

    @Autowired
    AdaptiveAlgConfig adaptiveAlgConfig;

    @Autowired
    MetaDataService metaDataService;

    @Autowired
    PopularityService popularityService;

    @Autowired
    ReadWriteService readWriteService;
    private List<AccessRecord> records;

    @Scheduled(cron = "${decs.scheduler.lstm}")
    public void replicaScheduler(){
        // 首先计算出所有 <file, <node, accessAmount>> 预测访问量
        Map<String, Map<String, Double>> predictionResult = new HashMap<>();

        List<FileMeta> allFiles = metaDataService.getAllFiles().orElse(new ArrayList<>());
        for (FileMeta file: allFiles){
            String fileName = file.getFileName();
            List<String> nodes = metaDataService.getAccessRecordIndexByFile(fileName)
                    .orElse(null);
            Map<String, Double> middleResult = new HashMap<>();

            if (nodes == null){
                continue;
            }
            else {
                for (String nodeId: nodes){
                    double p = popularityService.calculatePopularityByFileAndByNodeForHour(fileName, nodeId,
                            TimeUtil.getCurrentAbsoluteHour() + 1);
                    middleResult.put(nodeId, p);
                }
            }



            predictionResult.put(fileName, middleResult);
        }

        // 这个函数执行完之后一段时间内，可以根据实时情况 自适应调整，不用重新训练
        // 所以要把结果放到redis里，供后续使用
        metaDataService.setPredictionResultByHour(
                TimeUtil.getCurrentAbsoluteHour() + 1,
                predictionResult
        );

        log.info("Prediction result of replica scheduler: {}", JSON.toJSONString(predictionResult));
        adjustReplicaNumber(predictionResult);
    }

    @Scheduled(cron = "${decs.scheduler.adaptive1}")
    public void adaptiveScheduler1() {
        _adaptiveScheduler(1.0/3);
    }

    @Scheduled(cron = "${decs.scheduler.adaptive2}")
    public void adaptiveScheduler2() {
        _adaptiveScheduler(2.0/3);
    }


    public void _adaptiveScheduler(double percentage){
        Optional<Map<String, Map<String, Double>>> optionalPredictionResult = metaDataService.getPredictionResultByHour(TimeUtil.getCurrentAbsoluteHour());
        if (!optionalPredictionResult.isPresent())
            return;
        Map<String, Map<String, Double>> predictionResult = optionalPredictionResult.get();

        for (Map.Entry<String, Map<String, Double>> filePrediction: predictionResult.entrySet()){
            log.info("=============Start=============");
            String fileName = filePrediction.getKey();
            log.info("Calculating adaptive policy for file {}", fileName);


            for (Map.Entry<String, Double> fileNodePrediction: filePrediction.getValue().entrySet()) {
                String nodeId = fileNodePrediction.getKey();
                List<AccessRecord> records = metaDataService.getAccessRecordsByFileAndByNode(fileName, nodeId)
                        .orElse(new ArrayList<>());
                Optional<AccessRecord> accessAmountForCurrentHour = records.stream().filter(r -> r.getHour() == TimeUtil.getCurrentAbsoluteHour())
                        .findFirst();
                /**
                 * TODO：目前实现是 hard code 的
                 * 目前设置的是一小时一次 LSTM
                 * 中间每20分钟一次自适应
                 * 若假设访问量时均匀分布，第一次自适应期望值是 1\3 的预测值
                 * 同理第二次自适应期望值是 2\3 的预测值
                 */

                PCF pcf = metaDataService.getPCFByFileAndByNode(fileName, nodeId)
                        .orElse(PCF.builder()
                                .value(adaptiveAlgConfig.PCF_INIT)
                                .build());
                double pcfValue = pcf.getValue();
                double realAmountTillNow = accessAmountForCurrentHour.isPresent() ?
                        accessAmountForCurrentHour.get().getAccessAmount(): 0;
                double predictedAmount = fileNodePrediction.getValue();
                double predictedPercentageAmount = predictedAmount * percentage;

                // TODO 如何处理 RealAmountTillNow
                double adaptive = pcfValue * predictedAmount + (1 - pcfValue) * (realAmountTillNow / percentage);
                // 误差过大的情况
                if (loss(realAmountTillNow / percentage, predictedAmount) > adaptiveAlgConfig.PCF_VALUE_THRESHOLD) {
                    if (pcf.getNegativeCount() > 0){
                        pcf.clearCount();
                    }
                    pcf.increasePostiveCount();
                    if (pcf.getPositiveCount() > adaptiveAlgConfig.PCF_COUNT_THRESHOLD){
                        pcf.decreaseValueByStep(adaptiveAlgConfig.STEP);
                    }
                }
                // 误差较小的情况，若一直预测比较精准，则提高 PCF 值，即预测值的占比
                else {
                    if (pcf.getPositiveCount() > 0){
                        pcf.clearCount();
                    }
                    pcf.increaseNegativeCount();
                    if (pcf.getNegativeCount() > adaptiveAlgConfig.PCF_COUNT_THRESHOLD){
                        pcf.increaseValueByStep(adaptiveAlgConfig.STEP);
                    }
                }
                // 更新自适应的预测结果
                fileNodePrediction.setValue(adaptive);
            }
        }

        // 根据自适应结果，计算新的 replica 策略
        adjustReplicaNumber(predictionResult);
    }

    private double loss(double real, double expected) {
        return Math.abs(real - expected);
    }

    private void adjustReplicaNumber(Map<String, Map<String, Double>> predictionResult){
        List<FileMeta> allFiles = metaDataService.getAllFiles().orElse(new ArrayList<>());
        for (Map.Entry<String, Map<String, Double>> filePrediction: predictionResult.entrySet()){
            log.info("=============Start=============");
            String fileName = filePrediction.getKey();
            log.info("Calculating replica policy for file {}", fileName);

            List<String> nodesContainsThisFile = metaDataService.getNodesByFile(fileName).get();

            String nodeContainsFileWithMaxAccessAmount = null; // 持有数据的，访问量最多的节点
            String nodeContainsFileWithMinAccessAmount = null; // 原始副本节点 也就是持有数据的，访问量最低的节点
            String replicaCandidate = null; // 未持有数据，访问量最多的节点

            double piMax = Double.MIN_VALUE; // popularity of nodeContainsFileWithMaxAccessAmount
            double piMin = Double.MAX_VALUE; // popularity
            double pj = Double.MIN_VALUE;; // popularity of replica candidata
            double sum = 0;

            //log.info(JSON.toJSONString(filePrediction.getValue()));
            for (Map.Entry<String, Double> fileNodePrediction: filePrediction.getValue().entrySet()){


                // TODO 似乎这里要做标准化?
                String nodeId = fileNodePrediction.getKey();
                double p = fileNodePrediction.getValue();
                sum += p;

                if (nodesContainsThisFile.contains(nodeId)){
                    if (p > piMax){
                        piMax = p;
                        nodeContainsFileWithMaxAccessAmount = nodeId;
                    }
                    if (p < piMin){
                        piMin = p;
                        nodeContainsFileWithMinAccessAmount = nodeId;
                    }
                }
                else {
                    if (p > pj){
                        pj = p;
                        replicaCandidate = nodeId;
                    }
                }
            }

            log.info("Calculating replica policy for file {}, node {}", fileName, replicaCandidate);
            log.info("Files on nodes: {}", JSON.toJSONString(nodesContainsThisFile));

            log.info("-------------result-------------");
            if (replicaCandidate == null){
                log.info("Skip for null replica candidate");
                log.info("==============end==============\n");
                continue;
            }

            Node nodeiMin = metaDataService.getNodeByName(nodeContainsFileWithMinAccessAmount).get();
            Node nodej = metaDataService.getNodeByName(replicaCandidate)
                    .orElse(Node.NULL);

            // TODO 暂不考虑每个节点都有副本的情况，也就是 replicaCandidate 不存在的情况
            double riMin = nodeiMin.getDiskMeta().getFreeSpaceRatio();
            double rj = nodej.getDiskMeta().getFreeSpaceRatio();

            double replicaScore = calculateReplicaScore(piMax, pj, rj, sum, nodesContainsThisFile.size());
            double forwardScore = calculateForwardScore(piMin, pj, sum);
            double migrateScore = calculateMigrateScore(piMin, pj, riMin, rj, sum);
            double decreaseScore = nodeContainsFileWithMinAccessAmount == nodeContainsFileWithMaxAccessAmount ?
                    Double.MIN_VALUE: calculateDecreaseScore(piMin, pj, riMin, sum); // 如果这是唯一一个持有数据节点，则不能删除副本

            log.info("Node_i max: {}", nodeContainsFileWithMaxAccessAmount);
            log.info("Node_i min: {}", nodeContainsFileWithMinAccessAmount);
            log.info("Replica score: {}", replicaScore);
            log.info("Forward score: {}", forwardScore);
            log.info("Migrate score: {}", migrateScore);
            log.info("Decrease score: {}", decreaseScore);
            FileMeta file = allFiles.stream()
                    .filter(f -> f.getFileName().equals(fileName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(String.format("File doesn't exist %s", fileName)));

            if (replicaScore >= forwardScore &&
                    replicaScore >= migrateScore &&
                    replicaScore >= decreaseScore){
                log.info("Scheduler decides to replica file {} from node {} to node {}"
                        , file, nodeContainsFileWithMaxAccessAmount, replicaCandidate);
                //readWriteService._writeToNode(nodej, file);
            }
            else if (forwardScore >= replicaScore &&
                    forwardScore >= migrateScore &&
                    forwardScore >= decreaseScore){
                // 记录这条转发规则。然后 ReadWrite 那边采用这里的规则
                log.info("Scheduler decides to forward read request of file {} on node {} to node {}",
                        file, replicaCandidate, nodeContainsFileWithMinAccessAmount);
                //metaDataService.setForwardRule(fileName, replicaCandidate, nodeContainsFileWithMinAccessAmount);
            }
            else if (migrateScore >= replicaScore &&
                    migrateScore >= forwardScore &&
                    migrateScore >= decreaseScore){
                log.info("Scheduler decides to migrate replica file {} from node {} to node {}",
                        file, nodeContainsFileWithMinAccessAmount, replicaCandidate);

                //readWriteService._removeFromNode(nodeiMin, file);
                //readWriteService._writeToNode(nodej, file);

            }
            else if (decreaseScore >= replicaScore &&
                    decreaseScore >= forwardScore &&
                    decreaseScore >= migrateScore){
                log.info("Scheduler decides to remove replica file {} from node {}",
                        file, nodeContainsFileWithMinAccessAmount);

                //readWriteService._removeFromNode(nodeiMin, file);
            }
            log.info("==============end==============\n");
        }
    }

    // pi 是 访问量最高的 持有原始数据的节点
    private double calculateReplicaScore(double pi, double pj, double rj, double sum, int replicaNum){
        double pjStar = pj / sum;
        double score = (decsAlgConfig.ALPHA * rj + decsAlgConfig.BETA * pjStar)
                / ((pi + pj + 1) * decsAlgConfig.TC + decsAlgConfig.TI)
                * decsAlgConfig.GAMMA * Math.pow(Math.E, replicaNum);
        return score;
    }

    // pi 是 访问量最低的 持有原始数据的节点
    private double calculateForwardScore(double pi, double pj, double sum){
        double pjStar = pj / sum;
        double score = decsAlgConfig.TC * (decsAlgConfig.BETA * pjStar)
                / ((pj + pi) * decsAlgConfig.TC + pj * decsAlgConfig.TI);
        return score;
    }

    // pi 是 访问量最低的 持有原始数据的节点
    private double calculateMigrateScore(double pi, double pj, double ri, double rj, double sum){
        double pjStar = pj / sum;
        double piStar = pi / sum;
        double score = decsAlgConfig.TC
                * (decsAlgConfig.ALPHA * (1 - ri + rj) + decsAlgConfig.BETA * (1 - piStar + pjStar))
                / (2 * ((pi + 1) * (decsAlgConfig.TC + decsAlgConfig.TI) + pj * decsAlgConfig.TC));
        return score;
    }

    private double calculateDecreaseScore(double pi, double pj, double ri, double sum){
        double piStar = pi / sum;
        double score = decsAlgConfig.TC
                * (decsAlgConfig.ALPHA * (1 - ri) + decsAlgConfig.BETA * (1 - piStar))
                / (pi * (decsAlgConfig.TC + decsAlgConfig.TI) + pj * decsAlgConfig.TC);
        return score;
    }
}
