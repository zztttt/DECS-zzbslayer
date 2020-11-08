package reins.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reins.config.GlobalVar;
import reins.domain.AccessRecord;
import reins.service.PredictService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
@Service
public class PredictServiceImpl implements PredictService {
    @Autowired
    GlobalVar globalVar;

    @Override
    public int predict(List<AccessRecord> dataset) {

        try {
            return callScript(globalVar.SCRIPT_PATH);
        }
        catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private void datasetToFile(List<AccessRecord> dataset){

    }

    private int callScript(String scriptPath) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder()
                .command("python", "-u", scriptPath);
        Process p = pb.start();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        StringBuilder buffer = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null){
            buffer.append(line);
        }
        int result = p.waitFor();
        log.info("Value is: "+buffer.toString());
        log.info("Process exit value:"+result);
        in.close();

        return result;
    }
}
