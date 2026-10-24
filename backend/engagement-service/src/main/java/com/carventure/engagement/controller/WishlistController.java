package com.carventure.engagement.controller;

import com.carventure.engagement.dto.AddWishlistRequest;
import com.carventure.engagement.dto.WishlistItemResponse;
import com.carventure.engagement.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final WishlistService service;

    public WishlistController(WishlistService service) {
        this.service = service;
    }

    @PostMapping
    public WishlistItemResponse add(@Valid @RequestBody AddWishlistRequest request) {
        return service.add(request);
    }

    @GetMapping("/{customerId}")
    public List<WishlistItemResponse> list(@PathVariable String customerId) {
        return service.list(customerId);
    }

    @GetMapping("/{customerId}/contains/{carId}")
    public boolean contains(@PathVariable String customerId, @PathVariable String carId) {
        return service.contains(customerId, carId);
    }

    @DeleteMapping("/{customerId}/{carId}")
    public ResponseEntity<Void> remove(@PathVariable String customerId, @PathVariable String carId) {
        service.remove(customerId, carId);
        return ResponseEntity.noContent().build();
    }
}
