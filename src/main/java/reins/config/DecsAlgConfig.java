package reins.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DecsAlgConfig {
    @Value("${decs.popularity-threshold}")
    public double POPULARITY_THRESHOLD;

    // 用户与当前节点（也就是最近节点）传输所需要的时间
    @Value("${decs.tc}")
    public double TC;

    //节点间传输所需要的时间
    @Value("${decs.ti}")
    public double TI;

    @Value("decs.time-window")
    public int TIME_WINDOW;

    @Value("${decs.alpha}")
    public double ALPHA;

    @Value("${decs.beta}")
    public double BETA;

    @Value("${decs.gamma}")
    public double GAMMA;
}
