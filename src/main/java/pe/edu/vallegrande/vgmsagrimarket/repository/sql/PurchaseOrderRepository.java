package pe.edu.vallegrande.vgmsagrimarket.repository.sql;

import pe.edu.vallegrande.vgmsagrimarket.model.sql.PurchaseOrder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends ReactiveCrudRepository<PurchaseOrder, Integer> {
}
