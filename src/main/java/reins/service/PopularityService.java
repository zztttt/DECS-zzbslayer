package reins.service;

import reins.domain.AccessRecord;

import java.util.List;

public interface PopularityService {
    double calculatePopularityByFileAndByNodeForHour(String fileName, String nodeId, long hour);

    double calculatePopularityForHour(List<AccessRecord> records, long hour);

}
