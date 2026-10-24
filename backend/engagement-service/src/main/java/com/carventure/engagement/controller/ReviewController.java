package com.carventure.engagement.controller;

import com.carventure.engagement.dto.ReviewSummary;
import com.carventure.engagement.dto.SubmitReviewRequest;
import com.carventure.engagement.dto.UpdateReviewRequest;
import com.carventure.engagement.entity.Review;
import com.carventure.engagement.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Review> submit(@Valid @RequestBody SubmitReviewRequest request) {
        return new ResponseEntity<>(service.submit(request), HttpStatus.CREATED);
    }

    @GetMapping("/seller/{sellerId}")
    public Page<Review> bySeller(@PathVariable String sellerId,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return service.bySeller(sellerId, PageRequest.of(page, size));
    }

    @GetMapping("/seller/{sellerId}/summary")
    public ReviewSummary summary(@PathVariable String sellerId) {
        return service.summary(sellerId);
    }

    @PutMapping("/{reviewId}")
    public Review update(@PathVariable String reviewId,
                         @Valid @RequestBody UpdateReviewRequest request) {
        return service.update(reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable String reviewId,
                                       @RequestParam String customerId) {
        service.delete(reviewId, customerId);
        return ResponseEntity.noContent().build();
    }
}
