package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.CreatePurchaseOrderRequest;
import com.agrimarket.demo.dto.PurchaseOrderItemDTO;
import com.agrimarket.demo.dto.PurchaseOrderWithDetailsDTO;
import com.agrimarket.demo.dto.StatusUpdateRequest;
import com.agrimarket.demo.dto.UpdatePurchaseOrderRequest;
import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.sql.PurchaseOrder;
import com.agrimarket.demo.model.sql.PurchaseOrderDetail;
import com.agrimarket.demo.model.sql.PurchaseOrderStatus;
import com.agrimarket.demo.repository.mongo.ProductRepository;
import com.agrimarket.demo.repository.mongo.SupplierRepository;
import com.agrimarket.demo.repository.sql.PurchaseOrderDetailRepository;
import com.agrimarket.demo.repository.sql.PurchaseOrderRepository;
import com.agrimarket.demo.repository.sql.StoreRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
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

                    Mono<PurchaseOrderWithDetailsDTO> flow = purchaseOrderRepository.save(po)
                            .flatMap(poSaved -> {
                                Flux<PurchaseOrderDetail> detailsFlux = Flux.fromIterable(req.items())
                                        .map(item -> toDetail(item, poSaved.getPurchaseOrderId()))
                                        .flatMap(purchaseOrderDetailRepository::save);

                                return Mono.just(poSaved)
                                        .zipWith(detailsFlux.collectList())
                                        .map(tuple -> buildDTO(tuple.getT1(), tuple.getT2()));
                            });

                    return flow.as(transactionalOperator::transactional);
                }));
    }

    @Override
    public Mono<PurchaseOrderWithDetailsDTO> update(Integer id, UpdatePurchaseOrderRequest req) {
        return purchaseOrderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PurchaseOrder no encontrado con id: " + id)))
                .flatMap(existingPo -> validateEditable(existingPo)
                        .then(validateUpdateStatus(req.status()))
                        .then(validateUpdate(req))
                        .then(Mono.defer(() -> {
                            LocalDateTime now = LocalDateTime.now();
                            existingPo.setPurchaseOrderDate(req.purchaseOrderDate());
                            existingPo.setTotalAmount(calculateTotal(req.items()));
                            existingPo.setSupplierId(req.supplierId());
                            existingPo.setStoreId(req.storeId());
                            existingPo.setNotes(req.notes());
                            existingPo.setUpdatedAt(now);

                            Mono<PurchaseOrderWithDetailsDTO> flow = purchaseOrderRepository.save(existingPo)
                                    .flatMap(updatedPo -> purchaseOrderDetailRepository.deleteByPurchaseOrderId(id)
                                            .then(Mono.just(updatedPo))
                                    )
                                    .flatMap(updatedPo -> {
                                        Flux<PurchaseOrderDetail> detailsFlux = Flux.fromIterable(req.items())
                                                .map(item -> toDetail(item, updatedPo.getPurchaseOrderId()))
                                                .flatMap(purchaseOrderDetailRepository::save);

                                        return Mono.just(updatedPo)
                                                .zipWith(detailsFlux.collectList())
                                                .map(tuple -> buildDTO(tuple.getT1(), tuple.getT2()));
                                    });

                            return flow.as(transactionalOperator::transactional);
                        }))
                );
    }

    @Override
    public Mono<PurchaseOrderWithDetailsDTO> updateStatus(Integer id, StatusUpdateRequest req) {
        PurchaseOrderStatus nextStatus;
        try {
            nextStatus = PurchaseOrderStatus.valueOf(req.status().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status debe ser uno de: " + List.of(PurchaseOrderStatus.values())));
        }

        Mono<PurchaseOrderWithDetailsDTO> flow = purchaseOrderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "PurchaseOrder no encontrado con id: " + id)))
                .flatMap(order -> {
                    if (nextStatus.name().equals(order.getStatus())) {
                        return findByIdWithDetails(id);
                    }
                    if (!PurchaseOrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Solo una orden PENDING puede cambiar a RECEIVED o CANCELLED"));
                    }
                    Mono<Void> receiveStock;
                    if (PurchaseOrderStatus.RECEIVED == nextStatus) {
                        if (order.getStoreId() == null) {
                            return Mono.error(new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "La orden necesita storeId para recibir el inventario"));
                        }
                        receiveStock = purchaseOrderDetailRepository
                                .findByPurchaseOrderId(id)
                                .concatMap(item -> inventoryService.increaseStock(
                                        item.getProductId(), order.getStoreId(), item.getQuantity()))
                                .then();
                    } else {
                        receiveStock = Mono.empty();
                    }
                    return receiveStock.then(Mono.defer(() -> {
                        order.setStatus(nextStatus.name());
                        order.setUpdatedAt(LocalDateTime.now());
                        return purchaseOrderRepository.save(order)
                                .flatMap(saved -> purchaseOrderDetailRepository.findByPurchaseOrderId(id)
                                        .collectList()
                                        .map(items -> buildDTO(saved, items)));
                    }));
                });
        return flow.as(transactionalOperator::transactional);
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
        return validateStoreExists(req.storeId())
                .then(validateSupplierExists(req.supplierId()))
                .then(validateProductsExist(req.items()));
    }

    private Mono<Void> validateUpdate(UpdatePurchaseOrderRequest req) {
        return validateStoreExists(req.storeId())
                .then(validateSupplierExists(req.supplierId()))
                .then(validateProductsExist(req.items()));
    }

    private Mono<Void> validateStoreExists(Integer storeId) {
        if (storeId == null) {
            return Mono.empty();
        }
        return storeRepository.existsById(storeId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Store no existe con id: " + storeId)))
                .then();
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

    private Mono<Void> validateProductsExist(List<PurchaseOrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return Mono.error(new ResourceNotFoundException("Debe registrar al menos un item en la purchase order"));
        }
        List<Long> productIds;
        try {
            productIds = items.stream()
                    .map(i -> Long.valueOf(i.productId()))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NumberFormatException ex) {
            return Mono.error(new ResourceNotFoundException("Existe un productId con formato inválido en los items"));
        }
        return productRepository.findAllById(productIds).collectList()
                .flatMap(found -> {
                    int expected = productIds.size();
                    int actual = found.size();
                    if (actual != expected) {
                        return Mono.error(new ResourceNotFoundException(
                                "Algunos productId no existen en el catálogo (encontrados: " + actual + "/" + expected + ")"));
                    }
                    return Mono.empty();
                });
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
