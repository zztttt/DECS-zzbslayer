# Redis 中的数据结构
- 节点ID -> 节点自身的 metadata
    - 记录节点自身的 metadata，比如磁盘占用信息
    - String nodeId -> Node node
- 节点ID_storage -> 文件的列表
    - 记录该节点存储了哪些文件
    - String fileName_storage -> List\<FakeFile\>
- 文件名 -> 节点名的列表
    - 记录该文件位于哪些节点上
    - String fileName -> List\<String\> nodeNames
- 文件名_acIndex -> 节点名列表
    - 记录有哪些 IP 访问过该文件
    - String fileName_acIndex -> List\<String\>
    - 帮助找到所有 文件名_节点ID 的键值
    - **存储该文件的节点列表，跟访问过该文件的节点列表可能不同**
        - 存在读请求转发
- 文件名_节点ID -> 该文件在该节点上的访问记录的列表
    - 记录所有文件访问记录
    - 访问记录是 (时间窗口，访问量) 的二元组
    