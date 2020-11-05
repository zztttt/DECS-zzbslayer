package reins.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Node {
    String id;
    DiskMeta diskMeta;
}
