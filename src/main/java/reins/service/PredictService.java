package reins.service;

import reins.domain.AccessRecord;

import java.util.List;

public interface PredictService {
    int predict(List<AccessRecord> dataset);
}
