package entity;

import java.util.HashMap;
import java.util.Map;

public class AppInterface {
    private int maxQueueLength;
    private int openCashRegistersCount;
    private Map<String, CashQueue> allQueues = new HashMap<>();
    private Map<String, CashRegister> allCashRegisters = new HashMap<>();
    private int averageQueueServiceTime;

    private static AppInterface INSTANCE;

    private AppInterface() {}

    public static AppInterface getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new AppInterface();
        }

        return INSTANCE;
    }

    public int getMaxQueueLength() {
        return maxQueueLength;
    }

    public void setMaxQueueLength(int maxQueueLength) {
        this.maxQueueLength = maxQueueLength;
    }

    public int getOpenCashRegistersCount() {
        return openCashRegistersCount;
    }

    public void setOpenCashRegistersCount(int openCashRegistersCount) {
        this.openCashRegistersCount = openCashRegistersCount;
    }

    public Map<String, CashQueue> getAllQueues() {
        return allQueues;
    }

    public CashQueue getShortestQueue() {
        int shortestQueueLong = Integer.MAX_VALUE;
        CashQueue shortestQueue = null;

        for(Map.Entry<String, CashQueue> q : allQueues.entrySet()) {
            if(q.getValue().getCashQueueLength() < shortestQueueLong) {
                shortestQueueLong = q.getValue().getCashQueueLength();
                shortestQueue = q.getValue();
            }
        }

        return shortestQueue;
    }

    public CashQueue getShortestPrivilegedQueue() {
        int shortestQueueLong = Integer.MAX_VALUE;
        CashQueue shortestQueue = null;

        for(Map.Entry<String, CashQueue> q : allQueues.entrySet()) {
            if(q.getValue().getCashQueueLength() < shortestQueueLong) {
                shortestQueueLong = q.getValue().getCashQueueLength();

                if(q.getValue().isPrivileged()) {
                    shortestQueue = q.getValue();
                }
            }
        }

        return shortestQueue != null ? shortestQueue : getShortestQueue();
    }

    public int getPrivilegedQueuesCount() {
        int uprzywilejowanych = 0;

        for(Map.Entry<String, CashQueue> q : allQueues.entrySet()) {
            if(q.getValue().isPrivileged()) {
                uprzywilejowanych++;
            }
        }

        return uprzywilejowanych;
    }

    public void setAllQueues(Map<String, CashQueue> allQueues) {
        this.allQueues = allQueues;
    }

    public int getAverageQueueServiceTime() {
        return averageQueueServiceTime;
    }

    public void setAverageQueueServiceTime(int averageQueueServiceTime) {
        this.averageQueueServiceTime = averageQueueServiceTime;
    }

    public Map<String, CashRegister> getAllCashRegisters() {
        return allCashRegisters;
    }

    public void setAllCashRegisters(Map<String, CashRegister> allCashRegisters) {
        this.allCashRegisters = allCashRegisters;
    }
}
