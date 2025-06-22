package me.ghosthacks96.pos.server.utils.models;

public class TransactionItem {
    private String itemId;
    private Product product;
    private int quantity;
    private double unitPrice;
    private double discount;
    private double totalPrice;

    public TransactionItem() {}

    public TransactionItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.discount = 0.0;
        calculateTotalPrice();
    }

    public void calculateTotalPrice() {
        this.totalPrice = (unitPrice * quantity) - discount;
    }

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        calculateTotalPrice();
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        calculateTotalPrice();
    }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) {
        this.discount = discount;
        calculateTotalPrice();
    }

    public double getTotalPrice() { return totalPrice; }
}