package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.CheckoutResponse;
import pe.edu.vallegrande.agrimarket.model.mongo.Cart;
import reactor.core.publisher.Mono;

public interface CartService {
    Mono<Cart> getActiveCart(String email, String sessionToken);
    Mono<Cart> addItem(String email, String sessionToken, String productId, Integer quantity);
    Mono<Cart> updateItem(String email, String sessionToken, String productId, Integer quantity);
    Mono<Cart> removeItem(String email, String sessionToken, String productId);
    Mono<Void> mergeGuestCart(String sessionToken, String customerId);
    Mono<CheckoutResponse> checkout(String email, Integer storeId, String paymentMethod);
}
