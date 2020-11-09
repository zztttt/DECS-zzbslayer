package reins.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reins.domain.FileMeta;
import reins.service.ReadWriteService;

import java.util.List;

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

    @PostMapping("/write-batch")
    @ResponseBody
    public int writeBatch(@RequestBody List<FileMeta> fileMetas){
        for (FileMeta fileMeta: fileMetas){
            readWriteService.write(fileMeta);
        }
        return 0;
    }

    @GetMapping("/read-batch")
    @ResponseBody
    public int readBatch(@RequestBody List<String> fileNames, @RequestParam("timeWindow") long timeWindow){
        for (String fileName: fileNames){
            readWriteService._readWithTimeWindow(fileName, timeWindow);
        }
        return 0;
    }
}
