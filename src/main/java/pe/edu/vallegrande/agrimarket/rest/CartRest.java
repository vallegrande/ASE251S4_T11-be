package pe.edu.vallegrande.agrimarket.rest;

import pe.edu.vallegrande.agrimarket.dto.CartItemRequest;
import pe.edu.vallegrande.agrimarket.dto.CheckoutRequest;
import pe.edu.vallegrande.agrimarket.dto.CheckoutResponse;
import pe.edu.vallegrande.agrimarket.model.mongo.Cart;
import pe.edu.vallegrande.agrimarket.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cart")
public class CartRest {

    private final CartService cartService;

    public CartRest(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Mono<Cart> getCart(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String sessionToken) {
        return cartService.getActiveCart(email, sessionToken);
    }

    @PostMapping("/items")
    public Mono<Cart> addItem(
            @AuthenticationPrincipal String email,
            @RequestHeader(name = "X-Session-Token", required = false) String sessionToken,
            @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(email, sessionToken, request.getProductId(), request.getQuantity());
    }

    @PutMapping("/items/{productId}")
    public Mono<Cart> updateItem(
            @AuthenticationPrincipal String email,
            @RequestHeader(name = "X-Session-Token", required = false) String sessionToken,
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "quantity debe ser mayor que cero"));
        }
        return cartService.updateItem(email, sessionToken, productId, quantity);
    }

    @DeleteMapping("/items/{productId}")
    public Mono<Cart> removeItem(
            @AuthenticationPrincipal String email,
            @RequestHeader(name = "X-Session-Token", required = false) String sessionToken,
            @PathVariable String productId) {
        return cartService.removeItem(email, sessionToken, productId);
    }

    // Checkout requiere un CLIENTE autenticado; el pago de este proyecto es simulado.
    @PostMapping("/checkout")
    public Mono<CheckoutResponse> checkout(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CheckoutRequest request) {
        return cartService.checkout(email, request.getStoreId(), request.getPaymentMethod());
    }
}
