package me.ghosthacks96.pos.server.utils.models;

public class Customer {
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private double loyaltyPoints;
    private CustomerType customerType;

    public enum CustomerType {
        REGULAR, VIP, MEMBER
    }

    public Customer() {}

    public Customer(String customerId, String firstName, String lastName) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.customerType = CustomerType.REGULAR;
        this.loyaltyPoints = 0.0;
    }

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(double loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
