package reins.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AccessRecord {
    private long timeWindow;
    private int accessAmount;

    public void increaseAccess(){
        this.accessAmount++;
    }
}
