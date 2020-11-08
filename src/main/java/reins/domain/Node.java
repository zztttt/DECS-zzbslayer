package reins.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Node {
    public final static Node NULL = Node.builder().diskMeta(DiskMeta.NULL).build();

    String id;
    DiskMeta diskMeta;
}
