package reins.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reins.config.AdaptiveAlgConfig;
import reins.domain.AccessRecord;
import reins.service.KeyValueService;
import reins.service.MetaDataService;
import reins.service.PopularityService;
import reins.service.PredictService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PopularityServiceImpl implements PopularityService {
    @Autowired
    AdaptiveAlgConfig adaptiveAlgConfig;

    @Autowired
    PredictService predictService;

    @Autowired
    KeyValueService keyValueService;

    @Autowired
    MetaDataService metaDataService;

    @Override
    public double calculatePopularityForTimeWindow(List<AccessRecord> records, long timeWindow) {
        // 生成这个列表时就是按照顺序一个个append进去的
        //records.sort(Comparator.comparingLong(AccessRecord::getTimeWindow));
//        log.info("Raw records: ");
//        log.info(JSON.toJSONString(records));
        List<AccessRecord> dataset = rawRecordsToDataSet(records, timeWindow);
//        log.info("Dataset: ");
//        log.info(JSON.toJSONString(dataset));
        return calculatePopularity(dataset);
    }

    @Override
    public double calculatePopularityByFileAndByNodeForTimeWindow(String fileName, String nodeId, long timeWindow) {
//        String key = KeyUtil.generateInternalKey(fileName, nodeId, "popularity");
//        // buffer in redis
//        if (keyValueService.exists(key)){
//            return keyValueService.get(key, Double.class);
//        }
        log.info("=============Start=============");
        log.info("Calculating popularity for file {} and nodeId {} at timeWindow {}",
                fileName, nodeId, timeWindow);
        List<AccessRecord> records = metaDataService.getAccessRecordsByFileAndByNode(fileName, nodeId)
                .orElse(new ArrayList<>());
        double res = calculatePopularityForTimeWindow(records, timeWindow);
        //keyValueService.put(key, res);
        log.info("==============end==============\n");
        return res;
    }

    private List<AccessRecord> rawRecordsToDataSet(List<AccessRecord> rawRecords, long timeWindow){
        // 如果某个小时没有访问量那就会没有记录，因此需要先用访问量 0 填充
        int rawSize = rawRecords.size();
        if (rawSize <= 1)
            return rawRecords;

        List<AccessRecord> dataset = new ArrayList<>((int)(rawRecords.size() * 1.5));

        dataset.add(rawRecords.get(0));
        AccessRecord lastRecord = rawRecords.get(0);

        // 形如【第1小时，第3小时】这样的原始记录集，需要在其中把第2小时访问量0的记录填充上
        for (int i = 1; i < rawSize; i++){
            AccessRecord currentRecord = rawRecords.get(i);
            long currentMinute = currentRecord.getTimeWindow();
            long lastMinute = lastRecord.getTimeWindow();

            fillTimeGap(dataset, lastMinute, currentMinute);

            dataset.add(currentRecord);
            lastRecord = currentRecord;
        }

        long lastMinute = dataset.get(dataset.size() - 1).getTimeWindow();
        fillTimeGap(dataset, lastMinute, timeWindow);
        // 填充完毕后才是真正的数据集
        return dataset;
    }

    private void fillTimeGap(List<AccessRecord> tempDataset, long lastWindow, long targetWindow){
        long gap = lastWindow - targetWindow - 1;
        if (gap > 0){
            for (int j = 1; j <= gap; j++){
                tempDataset.add(
                        AccessRecord.builder()
                                .timeWindow(lastWindow + j)
                                .accessAmount(0)
                                .build()
                );
            }
        }
    }

    // Adaptive way for popularity calculation
    private double calculatePopularity(List<AccessRecord> dataset){
        int size = dataset.size();
        double res = 0;
        if (size == 0){
            return 0;
        }
        else if (size <= adaptiveAlgConfig.NCT){
            log.info("Popularity prediction uses average. [0, NCT]");
            for (AccessRecord record: dataset){
                res += record.getAccessAmount();
            }

            res = res / size;
        }
        else if (size <= adaptiveAlgConfig.OCT){
            log.info("Popularity prediction uses EMA. (NCT, OCT]");
            double lastEMA = dataset.get(adaptiveAlgConfig.NCT).getAccessAmount();
            for (int i = adaptiveAlgConfig.NCT + 1; i < size; i++){
                lastEMA = adaptiveAlgConfig.EMA_ALPHA * dataset.get(i).getAccessAmount()
                        + (1 - adaptiveAlgConfig.EMA_ALPHA) * lastEMA;
            }
            res = lastEMA;
        }
        else {
            log.info("Popularity prediction uses LSTM. (OCT, +∞)");
            res = predictService.predict(dataset);
        }
        log.info("Predicted popularity is {}", res);
        return res;
    }
}
