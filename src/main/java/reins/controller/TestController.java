package reins.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reins.config.GlobalVar;
import reins.domain.FileMeta;
import reins.domain.Node;
import reins.service.MetaDataService;
import reins.service.PopularityService;
import reins.service.impl.SchedulerServiceImpl;
import reins.utils.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public int replica(){
        schedulerService.replicaScheduler();
        return 0;
    }

    @GetMapping("/replica-with-timeWindow")
    @ResponseBody
    public int replicaWithTimeWindow(@RequestParam("timeWindow")long timeWindow){
        schedulerService._replicaSchedulerWithTimeWindow(timeWindow);
        return 0;
    }

    @GetMapping("/adaptive")
    @ResponseBody
    public int adaptive(@RequestParam("percentage") double percentage){
        schedulerService._adaptiveScheduler(percentage);
        return 0;
    }

    @GetMapping("/adaptive-with-timeWindow")
    @ResponseBody
    public int adaptiveWithTimeWindow(@RequestParam("percentage") double percentage, @RequestParam("timeWindow")long timeWindow){
        schedulerService._adaptiveSchedulerWithTimeWindow(percentage, timeWindow);
        return 0;
    }

    @GetMapping("/file-distribution")
    @ResponseBody
    public Map<String, List<FileMeta>> getFileDistribution(){
        List<String> nodes = metaDataService.getNodes().orElseGet(() -> new ArrayList<>());
        Map<String, List<FileMeta>> distribution = new HashMap<>();
        for (String node: nodes){
            distribution.put(node, metaDataService.getFilesByNode(node).orElse(null));
        }
        return distribution;
    }
}
