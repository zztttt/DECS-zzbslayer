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

}
