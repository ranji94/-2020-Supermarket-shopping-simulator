package repository;

import entity.Klient;
import entity.Kolejka;

public interface SklepRepository {
    Klient findClientById(String clientId);
    Kolejka findShortestQueue();
    void addClientToShop(Klient klient);
    void addClientToQueue(Klient klient, Kolejka kolejka);
    boolean isClientExistsInQueue(Klient klient, Kolejka kolejka);
}
