package reins.service;

import reins.domain.AccessRecord;

import java.util.List;

public interface PopularityService {
    double calculatePopularityByFileAndByNodeForNextHour(String fileName, String nodeId);

    double calculatePopularityForHour(List<AccessRecord> records, long hour);

    double calculatePopularityForNextHour(List<AccessRecord> records);
}
