package projectlx.co.zw.audittrail.service.batch;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.repository.AuditLogRepository;

@Component
@RequiredArgsConstructor
public class AuditLogChunkDeleteWriter implements ItemWriter<Long> {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void write(Chunk<? extends Long> chunk) {
        List<Long> ids = new ArrayList<>(chunk.getItems());
        if (ids.isEmpty()) {
            return;
        }
        auditLogRepository.deleteByIds(ids);
    }
}
