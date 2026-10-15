package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.CartItemRequest;
import com.agrimarket.demo.dto.CheckoutRequest;
import com.agrimarket.demo.dto.CheckoutResponse;
import com.agrimarket.demo.model.mongo.Cart;
import com.agrimarket.demo.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // Permite recuperar el carrito del cliente autenticado o el del invitado por sessionToken.
    @GetMapping
    public Mono<Cart> getCart(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String sessionToken) {
        return cartService.getActiveCart(email, sessionToken);
    }

    // El invitado debe conservar y enviar X-Session-Token para mantener su carrito.
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
