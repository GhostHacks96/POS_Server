package me.ghosthacks96.pos.server.utils.models;

public class Product {
    private String productId;
    private String name;
    private String description;
    private double price;
    private String category;
    private String barcode;
    private int stockQuantity;
    private boolean isActive;

    public Product() {}

    public Product(String productId, String name, double price, String barcode) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.barcode = barcode;
        this.isActive = true;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
