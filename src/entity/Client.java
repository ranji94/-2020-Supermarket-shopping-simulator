package entity;

public class Client {
    private String clientId;
    private int productsCount;
    private boolean inCashRegister;

    public Client(String clientId, int productsCount, boolean inCashRegister) {
        this.clientId = clientId;
        this.productsCount = productsCount;
        this.inCashRegister = inCashRegister;
    }

    public Client(String clientId, int productsCount) {
        this.clientId = clientId;
        this.productsCount = productsCount;
        this.inCashRegister = false;
    }

    public Client(String clientId) {
        this.clientId = clientId;
        this.productsCount = 0;
        this.inCashRegister = false;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getProductsCount() {
        return productsCount;
    }

    public void setProductsCount(int productsCount) {
        this.productsCount = productsCount;
    }

    public boolean isInCashRegister() {
        return inCashRegister;
    }

    public void setInCashRegister(boolean inCashRegister) {
        this.inCashRegister = inCashRegister;
    }

    @Override
    public String toString() {
        return "Client{" +
                "clientId='" + clientId + '\'' +
                ", productsCount=" + productsCount +
                ", inCashRegister=" + inCashRegister +
                '}';
    }
}
