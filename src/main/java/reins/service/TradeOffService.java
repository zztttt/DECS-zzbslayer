package reins.service;

import reins.domain.FakeFile;
import reins.domain.Node;

public interface TradeOffService {
    Node pickNodeToWrite(FakeFile file);

    Node pickNodeToRead(String file);
}
