package reins.service;

import reins.domain.FileMeta;
import reins.domain.Node;

public interface ReadWriteService {
    String read(String fileName);
    String write(FileMeta file);

    void _writeToNode(Node node, FileMeta file);
    int _readFromNode(Node node, String fileName);
    void _removeFromNode(Node node, FileMeta file);

    /*
    * TODO: 暂时不支持更新文件
    * 一旦有 replica 就要同时去更新 replica
    */
}
