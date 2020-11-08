package reins.service;

import reins.domain.AccessRecord;
import reins.domain.FileMeta;
import reins.domain.Node;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MetaDataService {
    Optional<List<FileMeta>> getFilesByNode(String nodeId);

    void setFilesByNode(String nodeId, List<FileMeta> files);

    void updateNode(Node node);

    Optional<List<String>> getNodes();

    Optional<List<String>> getNodesByFile(String fileName);

    void setNodesByFile(String fileName, List<String> nodeIds);

    Optional<Node> getNodeByName(String nodeId);

    Optional<List<AccessRecord>> getAccessRecordsByFileAndByNode(String fileName, String nodeId);

    void setAccessRecordByFileAndByNode(String fileName, String nodeId, List<AccessRecord> records);

    Optional<List<String>> getAccessRecordIndexByFile(String fileName);

    void setAccessRecordIndexByFile(String fileName, List<String> nodeNames);

    Optional<List<FileMeta>> getAllFiles();

    void setAllFiles(List<FileMeta> fileNames);

    Optional<String> getForwardRule(String fileName, String nodeId);

    void setForwardRule(String fileName, String srcNode, String dstNode);

    Optional<Map<String, Map<String, Double>>> getPredictionResultByHour(long hour);

    void setPredictionResultByHour(long hour, Map<String, Map<String, Double>> predictionResult);
}
