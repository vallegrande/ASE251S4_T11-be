package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.CreatePurchaseOrderRequest;
import com.agrimarket.demo.dto.PurchaseOrderItemDTO;
import com.agrimarket.demo.dto.PurchaseOrderWithDetailsDTO;
import com.agrimarket.demo.dto.UpdatePurchaseOrderRequest;
import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.sql.PurchaseOrder;
import com.agrimarket.demo.model.sql.PurchaseOrderDetail;
import com.agrimarket.demo.repository.mongo.SupplierRepository;
import com.agrimarket.demo.repository.sql.PurchaseOrderDetailRepository;
import com.agrimarket.demo.repository.sql.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final SupplierRepository supplierRepository;
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
        return supplierRepository.findById(Long.valueOf(req.supplierId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Supplier no existe con id: " + req.supplierId())))
                .flatMap(supplier -> {
                    LocalDateTime now = LocalDateTime.now();
                    PurchaseOrder po = PurchaseOrder.builder()
                            .purchaseOrderDate(req.purchaseOrderDate())
                            .status(req.status())
                            .totalAmount(req.totalAmount())
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
                });
    }

    @Override
    public Mono<PurchaseOrderWithDetailsDTO> update(Integer id, UpdatePurchaseOrderRequest req) {
        return purchaseOrderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("PurchaseOrder no encontrado con id: " + id)))
                .flatMap(existingPo -> {
                    Mono<PurchaseOrderWithDetailsDTO> flow = supplierRepository.findById(Long.valueOf(req.supplierId()))
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Supplier no existe con id: " + req.supplierId())))
                            .flatMap(supplier -> {
                                LocalDateTime now = LocalDateTime.now();
                                existingPo.setPurchaseOrderDate(req.purchaseOrderDate());
                                existingPo.setStatus(req.status());
                                existingPo.setTotalAmount(req.totalAmount());
                                existingPo.setSupplierId(req.supplierId());
                                existingPo.setStoreId(req.storeId());
                                existingPo.setNotes(req.notes());
                                existingPo.setUpdatedAt(now);

                                return purchaseOrderRepository.save(existingPo)
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
                            });

                    return flow.as(transactionalOperator::transactional);
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
