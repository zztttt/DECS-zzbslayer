package reins.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reins.config.GlobalVar;
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

    @Autowired
    TimeUtil timeUtil;

    @GetMapping("/nodes")
    @ResponseBody
    public List<String> getNodes(){
        return metaDataService.getNodes().get();
    }

    @GetMapping("/popularities")
    @ResponseBody
    public double popularities(@RequestParam("fileName")String fileName){
        long timeWindow = timeUtil.getCurrentTimeWindow();
        return popularityService.calculatePopularityByFileAndByNodeForTimeWindow(fileName, globalVar.NODE_ID, timeWindow);
    }

    @GetMapping("/replica")
    @ResponseBody
    public int replica(@RequestParam("timeWindow")long timeWindow){
        schedulerService._replicaSchedulerWithTimeWindow(timeWindow);
        return 0;
    }

    @GetMapping("/adaptive")
    @ResponseBody
    public int adaptive(@RequestParam("percentage") double percentage, @RequestParam("timeWindow")long timeWindow){
        schedulerService._adaptiveSchedulerWithTimeWindow(percentage, timeWindow);
        return 0;
    }
}
