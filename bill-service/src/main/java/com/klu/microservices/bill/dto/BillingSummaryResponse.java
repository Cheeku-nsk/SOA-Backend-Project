package com.klu.microservices.bill.dto;

import java.math.BigDecimal;
import java.util.List;

public class BillingSummaryResponse {

    private long totalInvoices;
    private BigDecimal totalInvoiceValue;
    private BigDecimal averageInvoiceValue;
    private List<InvoiceResponse> recentInvoices;

    public BillingSummaryResponse() {
    }

    public BillingSummaryResponse(long totalInvoices, BigDecimal totalInvoiceValue, BigDecimal averageInvoiceValue, List<InvoiceResponse> recentInvoices) {
        this.totalInvoices = totalInvoices;
        this.totalInvoiceValue = totalInvoiceValue;
        this.averageInvoiceValue = averageInvoiceValue;
        this.recentInvoices = recentInvoices;
    }

    public long getTotalInvoices() {
        return totalInvoices;
    }

    public void setTotalInvoices(long totalInvoices) {
        this.totalInvoices = totalInvoices;
    }

    public BigDecimal getTotalInvoiceValue() {
        return totalInvoiceValue;
    }

    public void setTotalInvoiceValue(BigDecimal totalInvoiceValue) {
        this.totalInvoiceValue = totalInvoiceValue;
    }

    public BigDecimal getAverageInvoiceValue() {
        return averageInvoiceValue;
    }

    public void setAverageInvoiceValue(BigDecimal averageInvoiceValue) {
        this.averageInvoiceValue = averageInvoiceValue;
    }

    public List<InvoiceResponse> getRecentInvoices() {
        return recentInvoices;
    }

    public void setRecentInvoices(List<InvoiceResponse> recentInvoices) {
        this.recentInvoices = recentInvoices;
    }
}
