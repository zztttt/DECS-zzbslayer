package reins.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reins.domain.FileMeta;
import reins.service.ReadWriteService;

@Controller
public class ReadWriteController {

    @Autowired
    ReadWriteService readWriteService;

    @GetMapping("/read")
    @ResponseBody
    public String read(@RequestParam("fileName") String fileName){
        return readWriteService.read(fileName);
    }

    @GetMapping("/write")
    @ResponseBody
    public String write(FileMeta file){
        return readWriteService.write(file);
    }
}
