package com.book.review.service.service.impl;

import com.book.review.service.client.GoogleBooksFeignClient;
import com.book.review.service.config.ApiKeyProperties;
import com.book.review.service.dao.entity.ReviewEntity;
import com.book.review.service.dao.entity.UserEntity;
import com.book.review.service.dao.repository.ReviewRepository;
import com.book.review.service.dao.repository.UserRepository;
import com.book.review.service.exception.AuthorisationException;
import com.book.review.service.exception.BusinessException;
import com.book.review.service.mapper.ReviewMapper;
import com.book.review.service.mapper.UserMapper;
import com.book.review.service.model.ReviewRequestDto;
import com.book.review.service.model.ReviewResponseDto;
import com.book.review.service.service.ReviewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

import static com.book.review.service.util.helper.BookTransformer.convertToBookResponseDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final GoogleBooksFeignClient googleBooksFeignClient;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final ApiKeyProperties apiKeyProperties;

    @Override
    public Page<ReviewResponseDto> getReviewsByBookId(String bookId, Pageable pageable) {
        log.debug("ActionLog.getReviewsByBookId.start -> Get reviews by book id {}", bookId);

        final var reviewsEntity = reviewRepository.getReviewsByBookId(bookId, pageable);
        final var reviews = getReviewResponseDtos(reviewsEntity);

        log.debug("ActionLog.getReviewsByBookId.end -> Get reviews by book id {}", bookId);
        return reviews;
    }

    @Override
    public Page<ReviewResponseDto> getReviewsByReviewerId(Long reviewerId, Pageable pageable) {
        log.debug("ActionLog.getReviewsByReviewerId.start -> Get reviews by reviewer id {}", reviewerId);

        final var reviewsEntity = reviewRepository.getReviewsByReviewerId(reviewerId, pageable);
        final var reviews = getReviewResponseDtos(reviewsEntity);

        log.debug("ActionLog.getReviewsByReviewerId.end -> Get reviews by reviewer id {}", reviewerId);
        return reviews;
    }

    @Override
    @Transactional
    public void addReview(ReviewRequestDto reviewRequestDto) {
        log.debug("ActionLog.addReview.start -> Add review to book with id {}", reviewRequestDto.bookId());

        final var book = convertToBookResponseDto(googleBooksFeignClient.getBookById(
                reviewRequestDto.bookId(), apiKeyProperties.getKey()));
        final var userEntity = getReviewer();
        final var reviewEntity = ReviewEntity.builder()
                .bookId(book.id())
                .reviewer(userEntity)
                .review(reviewRequestDto.review())
                .reviewDate(ZonedDateTime.now())
                .build();
        reviewRepository.save(reviewEntity);

        log.debug("ActionLog.addReview.end -> Add review to book with id {}", reviewRequestDto.bookId());
    }

    @Override
    @Transactional
    public void updateReview(Long id, ReviewRequestDto reviewRequestDto) {
        log.debug("ActionLog.updateReview.start -> Update review for book with id {}", reviewRequestDto.bookId());

        final var userEntity = getReviewer();
        final var reviewEntity = getReviewEntity(id, reviewRequestDto.bookId(), userEntity.getId());

        reviewRepository.save(reviewMapper.toUpdateReviewEntity(reviewRequestDto, reviewEntity));

        log.debug("ActionLog.updateReview.end -> Update review for book with id {}", reviewRequestDto.bookId());
    }

    @Override
    @Transactional
    public void deleteReview(Long id, String bookId) {
        log.debug("ActionLog.deleteReview.start -> Delete review for book with id {}", bookId);

        final var userEntity = getReviewer();
        reviewRepository.delete(getReviewEntity(id, bookId, userEntity.getId()));

        log.debug("ActionLog.deleteReview.end -> Delete review for book with id {}", bookId);
    }

    private ReviewEntity getReviewEntity(Long id, String bookId, Long reviewerId) {
        return reviewRepository.getReviewByIdAndBookIdAndReviewerId(id, bookId, reviewerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        String.format(BusinessException.NOT_FOUND_REVIEW_BY_ID, id)));
    }

    private Page<ReviewResponseDto> getReviewResponseDtos(Page<ReviewEntity> reviewsEntity) {
        return reviewsEntity
                .map(reviewEntity -> {
                    final var reviewerDto = userMapper.toReviewDto(reviewEntity.getReviewer());
                    return reviewMapper.toReviewDto(reviewEntity, reviewerDto);
                });
    }

    private UserEntity getReviewer() {
        final var username = getUsername();

        return userRepository.findByEmail(username).orElseThrow(() ->
                new AuthorisationException(HttpStatus.NOT_FOUND,
                        String.format(AuthorisationException.NOT_FOUND_USER_BY_EMAIL, username)));
    }

    private String getUsername() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthorisationException(HttpStatus.UNAUTHORIZED, AuthorisationException.USER_UNAUTHORIZED);
        }
        return extractUsername(authentication.getPrincipal());
    }

    private String extractUsername(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String username) {
            return username;
        } else {
            throw new AuthorisationException(HttpStatus.UNAUTHORIZED, AuthorisationException.USER_UNAUTHORIZED);
        }
    }
}
