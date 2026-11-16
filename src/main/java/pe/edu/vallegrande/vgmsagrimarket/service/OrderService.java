package pe.edu.vallegrande.vgmsagrimarket.service;

import pe.edu.vallegrande.vgmsagrimarket.dto.CreateOrderRequest;
import pe.edu.vallegrande.vgmsagrimarket.dto.OrderWithDetailsDTO;
import pe.edu.vallegrande.vgmsagrimarket.dto.StatusUpdateRequest;
import pe.edu.vallegrande.vgmsagrimarket.dto.UpdateOrderRequest;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Flux<OrderWithDetailsDTO> findAllWithDetails();

    Mono<OrderWithDetailsDTO> findByIdWithDetails(Integer id);

    Flux<OrderWithDetailsDTO> findByCustomerEmail(String email);

    Mono<OrderWithDetailsDTO> findByIdWithDetailsForCustomer(Integer id, String email);

    Mono<OrderWithDetailsDTO> create(CreateOrderRequest req);

    Mono<List<OrderWithDetailsDTO>> createBatch(List<CreateOrderRequest> requests);

    Mono<OrderWithDetailsDTO> update(Integer id, UpdateOrderRequest req);

    Mono<OrderWithDetailsDTO> updateStatus(Integer id, StatusUpdateRequest req);
}
