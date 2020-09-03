package entity;

public class CashRegister {
    private String cashRegisterId;
    private String cashQueueId;
    private String currentClientId;
    private int serviceTime;
    private boolean privileged;

    public CashRegister(String cashRegisterId, String cashQueueId, String currentClientId, int serviceTime, boolean privileged) {
        this.cashRegisterId = cashRegisterId;
        this.cashQueueId = cashQueueId;
        this.currentClientId = currentClientId;
        this.serviceTime = serviceTime;
        this.privileged = privileged;
    }

    public int getServiceTime() {
        return serviceTime;
    }

    public void setServiceTime(int serviceTime) {
        this.serviceTime = serviceTime;
    }

    public String getCashRegisterId() {
        return cashRegisterId;
    }

    public void setCashRegisterId(String cashRegisterId) {
        this.cashRegisterId = cashRegisterId;
    }

    public String getCashQueueId() {
        return cashQueueId;
    }

    public void setCashQueueId(String cashQueueId) {
        this.cashQueueId = cashQueueId;
    }

    public String getCurrentClientId() {
        return currentClientId;
    }

    public void setCurrentClientId(String currentClientId) {
        this.currentClientId = currentClientId;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }
}
