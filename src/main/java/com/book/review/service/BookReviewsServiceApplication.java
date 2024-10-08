package com.book.review.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@ConfigurationPropertiesScan
public class BookReviewsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookReviewsServiceApplication.class, args);
    }
}