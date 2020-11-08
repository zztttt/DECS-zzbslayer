package reins.config;


import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalVar {

    @Autowired
    EurekaClient eurekaClient;

    public String NODE_ID;

    @Value("${decs.disk.max}")
    public int DISK_MAX;

    @Value("${decs.predict.script.path}")
    public String SCRIPT_PATH;

    @Bean("GlobalVar")
    public void setNODE_ID(){
        NODE_ID = eurekaClient.getApplicationInfoManager().getInfo().getInstanceId();
    }

}
