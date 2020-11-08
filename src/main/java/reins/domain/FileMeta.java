package reins.domain;

import lombok.Data;

@Data
public class FileMeta {
    private String fileName;
    private int size; // MB

    @Override
    public int hashCode(){
        return fileName.hashCode() + size;
    }

    @Override
    public boolean equals(Object file){
        if (getClass() != file.getClass())
            return false;
        FileMeta target = (FileMeta) file;
        if (this.fileName == target.getFileName()
            && this.size == target.getSize())
            return true;
        return false;
    }
}
