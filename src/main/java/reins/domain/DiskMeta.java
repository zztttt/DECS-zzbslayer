package reins.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DiskMeta {
    private int max;
    private int available;

    public void saveFile(FakeFile file){
        if (this.available < file.getSize()){
            throw new RuntimeException("Out of space");
        }
        this.available = this.available - file.getSize();
    }
}
