package me.ghosthacks96.pos.server.utils.models;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Receipt {
    private String receiptId;
    private Transaction transaction;
    private String storeName;
    private String storeAddress;
    private String storePhone;
    private LocalDateTime printDate;

    public Receipt() {
        this.printDate = LocalDateTime.now();
    }

    public Receipt(Transaction transaction, String storeName) {
        this();
        this.transaction = transaction;
        this.storeName = storeName;
        this.receiptId = "RCP-" + transaction.getTransactionId();
    }

    public String generateReceiptText() {
        StringBuilder receipt = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        receipt.append("===============================\n");
        receipt.append("         ").append(storeName).append("\n");
        if (storeAddress != null) {
            receipt.append("      ").append(storeAddress).append("\n");
        }
        if (storePhone != null) {
            receipt.append("      ").append(storePhone).append("\n");
        }
        receipt.append("===============================\n");
        receipt.append("Receipt ID: ").append(receiptId).append("\n");
        receipt.append("Transaction ID: ").append(transaction.getTransactionId()).append("\n");
        receipt.append("Date: ").append(transaction.getTransactionDate().format(formatter)).append("\n");
        receipt.append("Cashier: ").append(transaction.getCashierId()).append("\n");
        receipt.append("-------------------------------\n");

        for (TransactionItem item : transaction.getItems()) {
            receipt.append(String.format("%-20s %2d x %8.2f = %8.2f\n",
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice()));
        }

        receipt.append("-------------------------------\n");
        receipt.append(String.format("Subtotal:           %12.2f\n", transaction.getSubtotal()));
        receipt.append(String.format("Tax:                %12.2f\n", transaction.getTax()));
        receipt.append(String.format("Discount:           %12.2f\n", transaction.getDiscount()));
        receipt.append(String.format("TOTAL:              %12.2f\n", transaction.getTotalAmount()));
        receipt.append("-------------------------------\n");
        receipt.append("Payment Method: ").append(transaction.getPaymentMethod()).append("\n");
        receipt.append("===============================\n");
        receipt.append("    Thank you for shopping!\n");
        receipt.append("===============================\n");

        return receipt.toString();
    }

    // Getters and Setters
    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreAddress() { return storeAddress; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }

    public String getStorePhone() { return storePhone; }
    public void setStorePhone(String storePhone) { this.storePhone = storePhone; }

    public LocalDateTime getPrintDate() { return printDate; }
    public void setPrintDate(LocalDateTime printDate) { this.printDate = printDate; }
}