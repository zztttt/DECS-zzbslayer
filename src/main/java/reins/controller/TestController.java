package reins.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reins.config.GlobalVar;
import reins.domain.AccessRecord;
import reins.service.MetaDataService;
import reins.service.PopularityService;
import reins.service.impl.SchedulerServiceImpl;
import reins.utils.TimeUtil;

import java.util.List;

@Controller
public class TestController {
    @Autowired
    MetaDataService metaDataService;

    @Autowired
    PopularityService popularityService;

    @Autowired
    SchedulerServiceImpl schedulerService;

    @Autowired
    GlobalVar globalVar;

    @GetMapping("/nodes")
    @ResponseBody
    public List<String> getNodes(){
        return metaDataService.getNodes().get();
    }

    @GetMapping("/popularities")
    @ResponseBody
    public double popularities(@RequestParam("fileName")String fileName){
        return popularityService.calculatePopularityByFileAndByNodeForNextHour(fileName, globalVar.NODE_ID);
    }

    @GetMapping("/replica")
    @ResponseBody
    public int replica(){
        schedulerService.replicaScheduler();
        return 0;
    }
}
