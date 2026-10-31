package com.klu.microservices.bill.repository;

import com.klu.microservices.bill.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByServiceRecordId(Long serviceRecordId);

    boolean existsByServiceRecordId(Long serviceRecordId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByCustomerEmailIgnoreCaseOrderByGeneratedAtDesc(String customerEmail);

    List<Invoice> findByCustomerIdOrderByGeneratedAtDesc(Long customerId);

    List<Invoice> findAllByOrderByGeneratedAtDesc();

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(AVG(i.totalAmount), 0) FROM Invoice i")
    BigDecimal avgTotalAmount();

    List<Invoice> findTop5ByOrderByGeneratedAtDesc();
}
