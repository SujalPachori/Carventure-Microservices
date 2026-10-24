package com.carventure.engagement.service;

import com.carventure.engagement.dto.ReviewSummary;
import com.carventure.engagement.dto.SubmitReviewRequest;
import com.carventure.engagement.dto.UpdateReviewRequest;
import com.carventure.engagement.entity.Review;
import com.carventure.engagement.exception.NotAllowedException;
import com.carventure.engagement.exception.NotFoundException;
import com.carventure.engagement.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository repository;

    public ReviewService(ReviewRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Review submit(SubmitReviewRequest request) {

        LocalDateTime now = LocalDateTime.now();

        Review existing = repository
                .findBySellerIdAndCustomerId(request.getSellerId(), request.getCustomerId())
                .orElse(null);

        if (existing != null) {
            existing.setRating(request.getRating());
            existing.setComment(request.getComment());
            existing.setUpdatedAt(now);
            return repository.save(existing);
        }

        Review review = new Review();
        review.setReviewId(UUID.randomUUID().toString());
        review.setSellerId(request.getSellerId());
        review.setCustomerId(request.getCustomerId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(now);

        return repository.save(review);
    }

    public Page<Review> bySeller(String sellerId, Pageable pageable) {
        return repository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
    }

    public ReviewSummary summary(String sellerId) {

        long count = repository.countBySellerId(sellerId);

        if (count == 0) {
            return new ReviewSummary(sellerId, 0.0, 0);
        }

        Double average = repository.averageRating(sellerId);
        double rounded = average == null ? 0.0 : Math.round(average * 10.0) / 10.0;

        return new ReviewSummary(sellerId, rounded, count);
    }

    @Transactional
    public Review update(String reviewId, UpdateReviewRequest request) {

        Review review = load(reviewId);

        if (!review.getCustomerId().equals(request.getCustomerId())) {
            throw new NotAllowedException("You can only edit your own review");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        return repository.save(review);
    }

    @Transactional
    public void delete(String reviewId, String customerId) {

        Review review = load(reviewId);

        if (!review.getCustomerId().equals(customerId)) {
            throw new NotAllowedException("You can only delete your own review");
        }

        repository.delete(review);
    }

    private Review load(String reviewId) {
        return repository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));
    }
}
