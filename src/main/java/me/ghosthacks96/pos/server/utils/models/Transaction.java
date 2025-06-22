package me.ghosthacks96.pos.server.utils.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private String transactionId;
    private Customer customer;
    private List<TransactionItem> items;
    private double subtotal;
    private double tax;
    private double discount;
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private TransactionStatus status;
    private LocalDateTime transactionDate;
    private String cashierId;

    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, MOBILE_PAY, GIFT_CARD
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, CANCELLED, REFUNDED
    }

    public Transaction() {
        this.items = new ArrayList<>();
        this.transactionDate = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
    }

    public Transaction(String transactionId, String cashierId) {
        this();
        this.transactionId = transactionId;
        this.cashierId = cashierId;
    }

    public void addItem(TransactionItem item) {
        items.add(item);
        calculateTotals();
    }

    public void removeItem(TransactionItem item) {
        items.remove(item);
        calculateTotals();
    }

    public void calculateTotals() {
        subtotal = items.stream()
                .mapToDouble(TransactionItem::getTotalPrice)
                .sum();
        totalAmount = subtotal + tax - discount;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public List<TransactionItem> getItems() { return items; }
    public void setItems(List<TransactionItem> items) {
        this.items = items;
        calculateTotals();
    }

    public double getSubtotal() { return subtotal; }

    public double getTax() { return tax; }
    public void setTax(double tax) {
        this.tax = tax;
        calculateTotals();
    }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) {
        this.discount = discount;
        calculateTotals();
    }

    public double getTotalAmount() { return totalAmount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public String getCashierId() { return cashierId; }
    public void setCashierId(String cashierId) { this.cashierId = cashierId; }

    public int getItemCount() {
        return items.stream().mapToInt(TransactionItem::getQuantity).sum();
    }
}