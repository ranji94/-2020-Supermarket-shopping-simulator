package repository;

import entity.Klient;
import entity.Kolejka;
import entity.Sklep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SklepRepositoryImpl implements SklepRepository {
    private static Sklep sklep = Sklep.getInstance();

    @Override
    public Klient findClientById(String clientId) {
        return sklep.getWszyscyKlienciWSklepie().get(clientId);
    }

    @Override
    public Kolejka findShortestQueue() {
        Map<String, Kolejka> kolejki = sklep.getWszystkieKolejkiWSklepie();
        int queueLong = Integer.MAX_VALUE;
        Kolejka shortestQueue = null;

        for(Map.Entry<String, Kolejka> q : kolejki.entrySet()) {
            if(q.getValue().getDlugoscKolejki() < queueLong) {
                queueLong = q.getValue().getDlugoscKolejki();
                shortestQueue = q.getValue();
            }
        }
        return shortestQueue;
    }

    @Override
    public void addClientToShop(Klient klient) {
        if(sklep.getWszyscyKlienciWSklepie() == null) {
            sklep.setWszyscyKlienciWSklepie(new HashMap<>());
        }

        sklep.getWszyscyKlienciWSklepie().put(klient.getIdKlient(), klient);
    }

    @Override
    public boolean isClientExistsInQueue(Klient klient, Kolejka kolejka) {
        List<Klient> klienciWKolejce = kolejka.getListaKlientow();
        return klienciWKolejce.contains(klient);
    }

    @Override
    public void addClientToQueue(Klient klient, Kolejka kolejka) {
        Map<String, Kolejka> wszystkieKolejkiWSklepie = sklep.getWszystkieKolejkiWSklepie();

        if(wszystkieKolejkiWSklepie == null) {
            sklep.setWszystkieKolejkiWSklepie(new HashMap<>());
        }

        if(!sklep.getWszystkieKolejkiWSklepie().containsValue(kolejka)) {
            sklep.getWszystkieKolejkiWSklepie().put(kolejka.getIdKolejki(), kolejka);
        }

        List<Klient> listaKlientowWKolejce = sklep.getWszystkieKolejkiWSklepie().get(kolejka.getIdKolejki()).getListaKlientow();

        if(!listaKlientowWKolejce.contains(klient)) {
            listaKlientowWKolejce.add(klient);
            sklep.getWszystkieKolejkiWSklepie().get(kolejka.getIdKolejki()).setListaKlientow(listaKlientowWKolejce);
        }
    }
}
