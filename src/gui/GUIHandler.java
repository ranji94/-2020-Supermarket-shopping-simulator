package gui;

public interface GUIHandler {
    void addTotalClientsInShop(int totalClientsInShop);
    void addTotalClientsInShopping(int totalClientsInShopping);
    void addTotalClientsInCashQueues(int totalClientsInCashQueues);
    void addTotalClientsServiced(int totalClientsServiced);
    void cashQueuesVisualisation(String cashQueuesVisualisation);
    void addCashQueuesCount(int cashQueuesCount);
    void addTotalCashRegistersPrivileged(int totalCashRegistersPrivileged);
    void addMaxQueuesCount(int maxQueues);
    void addTotalProductsBought(int totalProductsBought);
    void addTotalProductsReturned(int totalProductsReturned);
    void addClientsPrivileged(int clientsPrivileged);
}
