package reins.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdaptiveAlgConfig {

    @Value("${decs.adaptive.nct}")
    public int NCT;

    @Value("${decs.adaptive.oct}")
    public int OCT;

    @Value("${decs.adaptive.ema-alpha}")
    public double EMA_ALPHA;

    @Value("${decs.adaptive.pcf-init}")
    public double PCF_INIT;

    @Value("${decs.adaptive.step}")
    public double STEP;

    @Value("${decs.adaptive.pcf-value-threshold}")
    public double PCF_VALUE_THRESHOLD;

    @Value("${decs.adaptive.pcf-count-threshold}")
    public double PCF_COUNT_THRESHOLD;

}
