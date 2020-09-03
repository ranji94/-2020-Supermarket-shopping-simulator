package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class GUIController implements GUIHandler {

    @FXML
    private Label totalClientsInShop;

    @FXML
    private Label totalClientsInShopping;

    @FXML
    private Label totalClientsInCashQueues;

    @FXML
    private Label totalClientsServiced;

    @FXML
    private Label cashQueuesVisualisation;

    @FXML
    private Label totalCashQueues;

    @FXML
    private Label totalCashRegistersPrivileged;

    @FXML
    private Label maxQueuesCount;

    @FXML
    private Label totalProductsBought;

    @FXML
    private Label totalProductsReturned;

    @FXML
    private Label clientsPrivileged;

    public void addTotalClientsInShop(int totalClientsInShop){
        Platform.runLater(()->{
            this.totalClientsInShop.setText("Clients in shop: "+totalClientsInShop);});
    }

    @Override
    public void addTotalClientsInShopping(int totalClientsInShopping) {
        Platform.runLater(()->{
            this.totalClientsInShopping.setText("Clients during shopping: "+totalClientsInShopping);});
    }

    @Override
    public void addTotalClientsInCashQueues(int totalClientsInCashQueues) {
        Platform.runLater(()->{
            this.totalClientsInCashQueues.setText("Clients in cash queues: "+totalClientsInCashQueues);});
    }

    @Override
    public void addTotalClientsServiced(int totalClientsServiced) {
        Platform.runLater(()->{
            this.totalClientsServiced.setText("Clients serviced: "+totalClientsServiced);});
    }

    @Override
    public void cashQueuesVisualisation(String cashQueuesVisualisation) {
        Platform.runLater(()-> this.cashQueuesVisualisation.setText(cashQueuesVisualisation));
    }

    @Override
    public void addCashQueuesCount(int cashQueuesCount) {
        Platform.runLater(()->{
            totalCashQueues.setText("CashQueues count: "+cashQueuesCount);});
    }

    @Override
    public void addTotalCashRegistersPrivileged(int totalCashRegistersPrivileged) {
        Platform.runLater(()->{
            this.totalCashRegistersPrivileged.setText("Total privileged cash registers: "+totalCashRegistersPrivileged);});
    }

    @Override
    public void addMaxQueuesCount(int maxQueues) {
        Platform.runLater(()->{maxQueuesCount.setText("Max Queues count: " + maxQueues);});
    }

    @Override
    public void addTotalProductsBought(int totalProductsBought) {
        Platform.runLater(()->{
            this.totalProductsBought.setText("Total products bought: "+totalProductsBought);});
    }

    @Override
    public void addTotalProductsReturned(int totalProductsReturned) {
        Platform.runLater(()->{
            this.totalProductsReturned.setText("Total products returned: "+totalProductsReturned);});
    }

    @Override
    public void addClientsPrivileged(int clientsPrivileged) {
        Platform.runLater(()->{
            this.clientsPrivileged.setText("Clients serviced in privileged cash registers: "+clientsPrivileged);});
    }

}
