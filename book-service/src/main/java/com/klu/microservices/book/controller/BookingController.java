package com.klu.microservices.book.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @GetMapping
    public String getBooking() {
        return "Welcome to Booking Service – Schedule and manage your vehicle service.";
    }
}
