package entity;

import java.util.HashMap;
import java.util.Map;

public class Shop {
    private int clientsInShoppingCount;
    private int clientsInCashQueueCount;
    private int maxQueueLenght;

    // GLOBAL VARIABLES
    private Map<String, Client> allClientsInShop = new HashMap<>();
    private Map<String, Client> allClientsInCashQueues = new HashMap<>();
    private Map<String, Client> allClientsInShopping = new HashMap<>();

    private static Shop INSTANCE;

    private Shop() {}

    public static Shop getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new Shop();
        }

        return INSTANCE;
    }

    public int getAllClientsInShopSum() {
        return allClientsInShop.size();
    }

    public int getClientsInCashQueueCount() {
        return allClientsInCashQueues.size();
    }

    public int getAllClientsInShoppingSum() { return allClientsInShopping.size(); }

    public Map<String, Client> getAllClientsInShop() {
        return allClientsInShop;
    }

    public void setAllClientsInShop(Map<String, Client> allClientsInShop) {
        this.allClientsInShop = allClientsInShop;
    }



    public int getMaxQueueLenght() {
        return maxQueueLenght;
    }

    public void setMaxQueueLenght(int maxQueueLenght) {
        this.maxQueueLenght = maxQueueLenght;
    }

    public Map<String, Client> getAllClientsInCashQueues() {
        return allClientsInCashQueues;
    }

    public void setAllClientsInCashQueues(Map<String, Client> allClientsInCashQueues) {
        this.allClientsInCashQueues = allClientsInCashQueues;
    }

    public Map<String, Client> getAllClientsInShopping() {
        return allClientsInShopping;
    }

    public void setAllClientsInShopping(Map<String, Client> allClientsInShopping) {
        this.allClientsInShopping = allClientsInShopping;
    }

    @Override
    public String toString() {
        return "Shop{" +
                "clientsInShoppingCount=" + clientsInShoppingCount +
                ", clientsInCashQueueCount=" + clientsInCashQueueCount +
                ", maxQueueLenght=" + maxQueueLenght +
                ", allClientsInShop=" + allClientsInShop +
                ", allClientsInCashQueues=" + allClientsInCashQueues +
                ", allClientsInShopping=" + allClientsInShopping +
                '}';
    }
}
