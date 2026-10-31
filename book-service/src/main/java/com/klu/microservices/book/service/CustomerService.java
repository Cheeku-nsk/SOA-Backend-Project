package com.klu.microservices.book.service;

import com.klu.microservices.book.entity.Customer;
import com.klu.microservices.book.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer getOrCreateCustomer(String email, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Customer email cannot be empty");
        }
        return customerRepository.findByEmail(email).orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setEmail(email);
            newCustomer.setName(name != null && !name.isBlank() ? name : email.split("@")[0]);
            newCustomer.setPhone("N/A");
            newCustomer.setCreatedAt(LocalDateTime.now());
            return customerRepository.save(newCustomer);
        });
    }
}
