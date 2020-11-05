package reins.service;

import reins.domain.AccessRecord;
import reins.domain.FakeFile;
import reins.domain.Node;

import java.util.List;
import java.util.Optional;

public interface MetaDataService {
    Optional<List<FakeFile>> getFilesByNode(String nodeId);

    void setFilesByNode(String nodeName, List<FakeFile> files);

    void updateNode(Node node);

    Optional<List<String>> getNodes();

    Optional<List<String>> getNodesByFile(String fileName);

    void setNodesByFile(String fileName, List<String> nodeIds);

    Optional<Node> getNodeByName(String nodeId);

    Optional<List<AccessRecord>> getAccessRecordsByFileAndByNode(String fileName, String nodeId);

    void setAccessRecordByFileAndByNode(String fileName, String nodeId, List<AccessRecord> records);
}
