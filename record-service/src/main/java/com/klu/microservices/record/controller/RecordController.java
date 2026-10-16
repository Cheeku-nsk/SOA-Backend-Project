package com.klu.microservices.record.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/records")
public class RecordController {

    @GetMapping
    public String getRecords() {
        return "Track Your Vehicle Records – View and manage your vehicle service history.";
    }
}
