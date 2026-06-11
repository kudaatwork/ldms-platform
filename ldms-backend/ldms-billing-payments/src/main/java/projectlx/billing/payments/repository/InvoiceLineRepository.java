package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.InvoiceLine;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    List<InvoiceLine> findByInvoiceIdAndEntityStatusNotOrderByLineNumberAsc(Long invoiceId, EntityStatus entityStatus);
}
