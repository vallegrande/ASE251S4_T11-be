package pe.edu.vallegrande.vgmsagrimarket.service;

import pe.edu.vallegrande.vgmsagrimarket.dto.CreatePurchaseOrderRequest;
import pe.edu.vallegrande.vgmsagrimarket.dto.PurchaseOrderWithDetailsDTO;
import pe.edu.vallegrande.vgmsagrimarket.dto.StatusUpdateRequest;
import pe.edu.vallegrande.vgmsagrimarket.dto.UpdatePurchaseOrderRequest;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PurchaseOrderService {

    Flux<PurchaseOrderWithDetailsDTO> findAllWithDetails();

    Mono<PurchaseOrderWithDetailsDTO> findByIdWithDetails(Integer id);

    Mono<PurchaseOrderWithDetailsDTO> create(CreatePurchaseOrderRequest req);

    Mono<List<PurchaseOrderWithDetailsDTO>> createBatch(List<CreatePurchaseOrderRequest> requests);

    Mono<PurchaseOrderWithDetailsDTO> update(Integer id, UpdatePurchaseOrderRequest req);

    Mono<PurchaseOrderWithDetailsDTO> updateStatus(Integer id, StatusUpdateRequest req);
}
