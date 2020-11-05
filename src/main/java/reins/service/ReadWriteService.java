package reins.service;

import reins.domain.FakeFile;

public interface ReadWriteService {
    String read(String fileName);
    String write(FakeFile file);
}
