package reins.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reins.config.GlobalVar;
import reins.domain.FakeFile;
import reins.domain.Node;
import reins.service.MetaDataService;
import reins.service.TradeOffService;

@Service
public class TradeOffServiceImpl implements TradeOffService {
    @Autowired
    MetaDataService metaDataService;

    @Autowired
    GlobalVar globalVar;

    @Override
    public Node pickNodeToRead(String file) {
        return metaDataService.getNodeByName(globalVar.NODE_ID).get();
    }

    @Override
    public Node pickNodeToWrite(FakeFile file) {
        return metaDataService.getNodeByName(globalVar.NODE_ID).get();
    }
}
