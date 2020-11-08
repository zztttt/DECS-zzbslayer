package reins.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DiskMeta {
    public final static DiskMeta NULL = DiskMeta.builder().build();

    private int max;
    private int available;

    public double getFreeSpaceRatio(){
        return available * 1.0 / max;
    }

    public void saveFile(FileMeta file){
        if (this.available < file.getSize()){
            throw new RuntimeException("Out of space");
        }
        this.available = this.available - file.getSize();
    }

    public void removeFile(FileMeta file){
        this.available = this.available + file.getSize();
    }
}
