package reins.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reins.service.MetaDataService;

import java.util.List;

@Controller
public class TestController {
    @Autowired
    MetaDataService metaDataService;

    @GetMapping("/nodes")
    @ResponseBody
    public List<String> getNodes(){
        return metaDataService.getNodes().get();
    }
}
