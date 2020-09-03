package entity;

import java.util.*;

public class CashQueue {
    private String queueId;
    private String cashRegisterId;
    private int averageServiceTime;
    private Queue<Client> clients = new LinkedList<>();
    private boolean privileged;

    public CashQueue() {}

    public CashQueue(String queueId) {
        this.queueId = queueId;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getCashRegisterId() {
        return cashRegisterId;
    }

    public void setCashRegisterId(String cashRegisterId) {
        this.cashRegisterId = cashRegisterId;
    }

    public int getCashQueueLength() {
        return clients.size();
    }

    public int getAverageServiceTime() {
        return averageServiceTime;
    }

    public void setAverageServiceTime(int averageServiceTime) {
        this.averageServiceTime = averageServiceTime;
    }

    public Queue<Client> getClients() {
        return clients;
    }

    public void setClients(Queue<Client> clients) {
        this.clients = clients;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }

    @Override
    public String toString() {
        return "CashQueue{" +
                "queueId='" + queueId + '\'' +
                ", cashRegisterId='" + cashRegisterId + '\'' +
                ", averageServiceTime=" + averageServiceTime +
                '}';
    }
}
