package reins.service;

import reins.domain.FileMeta;
import reins.domain.Node;

public interface TradeOffService {
    Node pickNodeToWrite(FileMeta file);

    Node pickNodeToRead(String file);
}
