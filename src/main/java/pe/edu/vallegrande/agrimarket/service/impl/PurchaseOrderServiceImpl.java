package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.dto.CreatePurchaseOrderRequest;
import pe.edu.vallegrande.agrimarket.dto.PurchaseOrderItemDTO;
import pe.edu.vallegrande.agrimarket.dto.PurchaseOrderWithDetailsDTO;
import pe.edu.vallegrande.agrimarket.dto.StatusUpdateRequest;
import pe.edu.vallegrande.agrimarket.dto.UpdatePurchaseOrderRequest;
import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import pe.edu.vallegrande.agrimarket.model.sql.PurchaseOrder;
import pe.edu.vallegrande.agrimarket.model.sql.PurchaseOrderDetail;
import pe.edu.vallegrande.agrimarket.model.sql.PurchaseOrderStatus;
import pe.edu.vallegrande.agrimarket.repository.mongo.SupplierRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.PurchaseOrderDetailRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final SupplierRepository supplierRepository;
    private final OrderReferenceValidator orderReferenceValidator;
    private final InventoryService inventoryService;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Flux<PurchaseOrderWithDetailsDTO> findAllWithDetails() {
        return purchaseOrderRepository.findAll()
                .flatMap(po -> Mono.just(po)
                        .zipWith(purchaseOrderDetailRepository.findByPurchaseOrderId(po.getPurchaseOrderId()).collectList())
                        .map(tuple -> buildDTO(tuple.getT1(), tuple.getT2()))
                );
    }

    @Override
    public Mono<PurchaseOrderWithDetailsDTO> findByIdWithDetails(Integer id) {
        return purchaseOrderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PurchaseOrder no encontrado con id: " + id)))
                .flatMap(po -> Mono.just(po)
                        .zipWith(purchaseOrderDetailRepository.findByPurchaseOrderId(po.getPurchaseOrderId()).collectList())
                        .map(tuple -> buildDTO(tuple.getT1(), tuple.getT2()))
                );
    }

    @Override
    public Mono<PurchaseOrderWithDetailsDTO> create(CreatePurchaseOrderRequest req) {
        return createPurchaseOrder(req).as(transactionalOperator::transactional);
    }

    @Override
    public Mono<List<PurchaseOrderWithDetailsDTO>> createBatch(List<CreatePurchaseOrderRequest> requests) {
        return Flux.fromIterable(requests)
                .concatMap(this::createPurchaseOrder)
                .collectList()
                .as(transactionalOperator::transactional);
    }

    private Mono<PurchaseOrderWithDetailsDTO> createPurchaseOrder(CreatePurchaseOrderRequest req) {
        return validateInitialStatus(req.status())
                .then(validateCreate(req))
                .then(Mono.defer(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    PurchaseOrder po = PurchaseOrder.builder()
                            .purchaseOrderDate(req.purchaseOrderDate())
                            .status("PENDING")
                            .totalAmount(calculateTotal(req.items()))
                            .supplierId(req.supplierId())
                            .storeId(req.storeId())
                            .notes(req.notes())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return purchaseOrderRepository.save(po)
                            .flatMap(savedOrder -> saveOrderDetails(savedOrder, req.items()));
                }));
    }

    @Override
    public Mono<PurchaseOrderWithDetailsDTO> update(Integer id, UpdatePurchaseOrderRequest req) {
        return purchaseOrderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PurchaseOrder no encontrado con id: " + id)))
                .flatMap(existingPo -> validateEditable(existingPo)
                        .then(validateUpdateStatus(req.status()))
                        .then(validateUpdate(req))
                        .then(Mono.defer(() -> updatePurchaseOrder(id, existingPo, req)))
                );
    }

    private Mono<PurchaseOrderWithDetailsDTO> updatePurchaseOrder(
            Integer id, PurchaseOrder existingPo, UpdatePurchaseOrderRequest req) {
        existingPo.setPurchaseOrderDate(req.purchaseOrderDate());
        existingPo.setTotalAmount(calculateTotal(req.items()));
        existingPo.setSupplierId(req.supplierId());
        existingPo.setStoreId(req.storeId());
        existingPo.setNotes(req.notes());
        existingPo.setUpdatedAt(LocalDateTime.now());

        return purchaseOrderRepository.save(existingPo)
                .flatMap(updatedPo -> purchaseOrderDetailRepository.deleteByPurchaseOrderId(id)
                        .then(saveOrderDetails(updatedPo, req.items())))
                .as(transactionalOperator::transactional);
    }

    private Mono<PurchaseOrderWithDetailsDTO> saveOrderDetails(
            PurchaseOrder purchaseOrder, List<PurchaseOrderItemDTO> items) {
        return Flux.fromIterable(items)
                .map(item -> toDetail(item, purchaseOrder.getPurchaseOrderId()))
                .flatMap(purchaseOrderDetailRepository::save)
                .collectList()
                .map(details -> buildDTO(purchaseOrder, details));
    }

    @Override
    public Mono<PurchaseOrderWithDetailsDTO> updateStatus(Integer id, StatusUpdateRequest req) {
        return parseNextStatus(req.status())
                .flatMap(nextStatus -> purchaseOrderRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                "PurchaseOrder no encontrado con id: " + id)))
                        .flatMap(order -> applyStatusTransition(id, order, nextStatus))
                        .as(transactionalOperator::transactional));
    }

    private Mono<PurchaseOrderStatus> parseNextStatus(String status) {
        PurchaseOrderStatus nextStatus;
        try {
            nextStatus = PurchaseOrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status debe ser uno de: " + List.of(PurchaseOrderStatus.values())));
        }
        return Mono.just(nextStatus);
    }

    private Mono<PurchaseOrderWithDetailsDTO> applyStatusTransition(
            Integer id, PurchaseOrder order, PurchaseOrderStatus nextStatus) {
        if (nextStatus.name().equals(order.getStatus())) {
            return findByIdWithDetails(id);
        }
        if (!PurchaseOrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo una orden PENDING puede cambiar a RECEIVED o CANCELLED"));
        }
        return receiveStockIfNeeded(id, order, nextStatus)
                .then(saveUpdatedStatus(id, order, nextStatus));
    }

    private Mono<Void> receiveStockIfNeeded(
            Integer id, PurchaseOrder order, PurchaseOrderStatus nextStatus) {
        if (nextStatus != PurchaseOrderStatus.RECEIVED) {
            return Mono.empty();
        }
        if (order.getStoreId() == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La orden necesita storeId para recibir el inventario"));
        }
        return purchaseOrderDetailRepository.findByPurchaseOrderId(id)
                .concatMap(item -> inventoryService.increaseStock(
                        item.getProductId(), order.getStoreId(), item.getQuantity()))
                .then();
    }

    private Mono<PurchaseOrderWithDetailsDTO> saveUpdatedStatus(
            Integer id, PurchaseOrder order, PurchaseOrderStatus nextStatus) {
        order.setStatus(nextStatus.name());
        order.setUpdatedAt(LocalDateTime.now());
        return purchaseOrderRepository.save(order)
                .flatMap(saved -> purchaseOrderDetailRepository.findByPurchaseOrderId(id)
                        .collectList()
                        .map(items -> buildDTO(saved, items)));
    }

    private Mono<Void> validateInitialStatus(String status) {
        if (!PurchaseOrderStatus.PENDING.name().equalsIgnoreCase(status.trim())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Una orden nueva debe iniciar con status PENDING"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateEditable(PurchaseOrder order) {
        if (!PurchaseOrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se puede editar una orden de compra PENDING"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateUpdateStatus(String status) {
        if (!PurchaseOrderStatus.PENDING.name().equalsIgnoreCase(status.trim())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usa PATCH /api/purchase-orders/{id}/status para cambiar el estado"));
        }
        return Mono.empty();
    }

    private BigDecimal calculateTotal(List<PurchaseOrderItemDTO> items) {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Mono<Void> validateCreate(CreatePurchaseOrderRequest req) {
        return orderReferenceValidator.validateStoreExists(req.storeId())
                .then(validateSupplierExists(req.supplierId()))
                .then(orderReferenceValidator.validateProductsExist(
                        req.items() == null ? null
                                : req.items().stream().map(PurchaseOrderItemDTO::productId).toList(),
                        "Debe registrar al menos un item en la purchase order"));
    }

    private Mono<Void> validateUpdate(UpdatePurchaseOrderRequest req) {
        return orderReferenceValidator.validateStoreExists(req.storeId())
                .then(validateSupplierExists(req.supplierId()))
                .then(orderReferenceValidator.validateProductsExist(
                        req.items() == null ? null
                                : req.items().stream().map(PurchaseOrderItemDTO::productId).toList(),
                        "Debe registrar al menos un item en la purchase order"));
    }

    private Mono<Void> validateSupplierExists(String supplierId) {
        Long id;
        try {
            id = Long.valueOf(supplierId);
        } catch (NumberFormatException ex) {
            return Mono.error(new ResourceNotFoundException("Supplier con id inválido: " + supplierId));
        }
        return supplierRepository.existsById(id)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Supplier no existe con id: " + supplierId)))
                .then();
    }

    private PurchaseOrderDetail toDetail(PurchaseOrderItemDTO item, Integer purchaseOrderId) {
        return PurchaseOrderDetail.builder()
                .quantity(item.quantity())
                .unitPrice(item.unitPrice())
                .productId(item.productId())
                .purchaseOrderId(purchaseOrderId)
                .build();
    }

    private PurchaseOrderWithDetailsDTO buildDTO(PurchaseOrder po, java.util.List<PurchaseOrderDetail> items) {
        return PurchaseOrderWithDetailsDTO.builder()
                .purchaseOrderId(po.getPurchaseOrderId())
                .purchaseOrderDate(po.getPurchaseOrderDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .supplierId(po.getSupplierId())
                .storeId(po.getStoreId())
                .notes(po.getNotes())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .items(items)
                .build();
    }
}
