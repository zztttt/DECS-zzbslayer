package reins.service;

import reins.domain.AccessRecord;

import java.util.List;

public interface PopularityService {
    double calculatePopularityByFileAndByNodeForTimeWindow(String fileName, String nodeId, long timeWindow);

    double calculatePopularityForTimeWindow(List<AccessRecord> records, long timeWindow);

}
