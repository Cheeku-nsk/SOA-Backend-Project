package com.klu.microservices.bill.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing")
public class BillingController {

    @GetMapping
    public String getBilling() {
        return "Welcome to Billing Service – View and manage your service invoices.";
    }
}
