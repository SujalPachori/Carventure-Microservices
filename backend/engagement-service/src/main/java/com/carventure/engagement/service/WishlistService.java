package com.carventure.engagement.service;

import com.carventure.engagement.client.CarLookup;
import com.carventure.engagement.dto.AddWishlistRequest;
import com.carventure.engagement.dto.CarResponse;
import com.carventure.engagement.dto.WishlistItemResponse;
import com.carventure.engagement.entity.WishlistItem;
import com.carventure.engagement.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {

    private final WishlistRepository repository;
    private final CarLookup carLookup;

    public WishlistService(WishlistRepository repository, CarLookup carLookup) {
        this.repository = repository;
        this.carLookup = carLookup;
    }

    @Transactional
    public WishlistItemResponse add(AddWishlistRequest request) {

        WishlistItem existing = repository
                .findByCustomerIdAndCarId(request.getCustomerId(), request.getCarId())
                .orElse(null);

        if (existing != null) {
            return enrich(WishlistItemResponse.from(existing));
        }

        WishlistItem item = new WishlistItem();
        item.setWishlistItemId(UUID.randomUUID().toString());
        item.setCustomerId(request.getCustomerId());
        item.setCarId(request.getCarId());
        item.setAddedAt(LocalDateTime.now());

        WishlistItem saved = repository.save(item);

        return enrich(WishlistItemResponse.from(saved));
    }

    public List<WishlistItemResponse> list(String customerId) {
        return repository.findByCustomerIdOrderByAddedAtDesc(customerId)
                .stream()
                .map(item -> enrich(WishlistItemResponse.from(item)))
                .toList();
    }

    public boolean contains(String customerId, String carId) {
        return repository.existsByCustomerIdAndCarId(customerId, carId);
    }

    @Transactional
    public void remove(String customerId, String carId) {
        repository.deleteByCustomerIdAndCarId(customerId, carId);
    }

    private WishlistItemResponse enrich(WishlistItemResponse response) {

        CarResponse car = carLookup.getCar(response.getCarId());

        if (car == null) {
            return response;
        }

        response.setBrand(car.getBrand());
        response.setModel(car.getModel());
        response.setManufactureYear(car.getManufactureYear());
        response.setPrice(car.getPrice());
        response.setCarStatus(car.getStatus());
        response.setAvailable(!"SOLD".equals(car.getStatus()));

        if (car.getImageUrls() != null && !car.getImageUrls().isEmpty()) {
            response.setImageUrl(car.getImageUrls().get(0));
        }

        return response;
    }
}
