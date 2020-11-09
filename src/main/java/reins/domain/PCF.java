package reins.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PCF {
    double value;

    @Builder.Default
    int positiveCount = 0;

    @Builder.Default
    int negativeCount = 0;

    public void increasePostiveCount(){
        this.positiveCount++;
    }

    public void increaseNegativeCount(){
        this.negativeCount++;
    }

    public void clearCount(){
        this.positiveCount = 0;
        this.negativeCount = 0;
    }

    public void increaseValueByStep(double step){
        value += step;
        if (value > 1)
            value = 1;
    }

    public void decreaseValueByStep(double step){
        value -= step;
        if (value < 0)
            value = 0;
    }
}
