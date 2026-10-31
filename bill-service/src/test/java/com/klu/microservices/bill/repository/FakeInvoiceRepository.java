package com.klu.microservices.bill.repository;

import com.klu.microservices.bill.entity.Invoice;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class FakeInvoiceRepository implements InvoiceRepository {

    private final Map<Long, Invoice> store = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(100);

    @Override
    public <S extends Invoice> S save(S entity) {
        if (entity.getInvoiceId() == null) {
            entity.setInvoiceId(idSeq.incrementAndGet());
        }
        store.put(entity.getInvoiceId(), entity);
        return entity;
    }

    @Override
    public Optional<Invoice> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Invoice> findByServiceRecordId(Long serviceRecordId) {
        return store.values().stream()
                .filter(i -> Objects.equals(i.getServiceRecordId(), serviceRecordId))
                .findFirst();
    }

    @Override
    public boolean existsByServiceRecordId(Long serviceRecordId) {
        return findByServiceRecordId(serviceRecordId).isPresent();
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return store.values().stream()
                .filter(i -> Objects.equals(i.getInvoiceNumber(), invoiceNumber))
                .findFirst();
    }

    @Override
    public List<Invoice> findByCustomerEmailIgnoreCaseOrderByGeneratedAtDesc(String customerEmail) {
        return store.values().stream()
                .filter(i -> i.getCustomerEmail() != null && i.getCustomerEmail().equalsIgnoreCase(customerEmail))
                .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
                .toList();
    }

    @Override
    public List<Invoice> findByCustomerIdOrderByGeneratedAtDesc(Long customerId) {
        return store.values().stream()
                .filter(i -> Objects.equals(i.getCustomerId(), customerId))
                .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
                .toList();
    }

    @Override
    public List<Invoice> findAllByOrderByGeneratedAtDesc() {
        return store.values().stream()
                .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
                .toList();
    }

    @Override
    public BigDecimal sumTotalAmount() {
        return store.values().stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal avgTotalAmount() {
        if (store.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = sumTotalAmount();
        return sum.divide(BigDecimal.valueOf(store.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public List<Invoice> findTop5ByOrderByGeneratedAtDesc() {
        return findAllByOrderByGeneratedAtDesc().stream().limit(5).toList();
    }

    @Override
    public long count() {
        return store.size();
    }

    // Default implementations for unused JpaRepository methods
    @Override public boolean existsById(Long id) { return store.containsKey(id); }
    @Override public List<Invoice> findAll() { return new ArrayList<>(store.values()); }
    @Override public List<Invoice> findAllById(Iterable<Long> longs) { return List.of(); }
    @Override public void deleteById(Long aLong) { store.remove(aLong); }
    @Override public void delete(Invoice entity) { store.remove(entity.getInvoiceId()); }
    @Override public void deleteAllById(Iterable<? extends Long> longs) { }
    @Override public void deleteAll(Iterable<? extends Invoice> entities) { }
    @Override public void deleteAll() { store.clear(); }
    @Override public <S extends Invoice> List<S> saveAll(Iterable<S> entities) { return List.of(); }
    @Override public void flush() { }
    @Override public <S extends Invoice> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Invoice> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
    @Override public void deleteAllInBatch(Iterable<Invoice> entities) { }
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) { }
    @Override public void deleteAllInBatch() { }
    @Override public Invoice getOne(Long aLong) { return store.get(aLong); }
    @Override public Invoice getById(Long aLong) { return store.get(aLong); }
    @Override public Invoice getReferenceById(Long aLong) { return store.get(aLong); }
    @Override public <S extends Invoice> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Invoice> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends Invoice> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends Invoice> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Invoice> long count(Example<S> example) { return 0; }
    @Override public <S extends Invoice> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Invoice, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public List<Invoice> findAll(Sort sort) { return findAll(); }
    @Override public Page<Invoice> findAll(Pageable pageable) { return Page.empty(); }
}
