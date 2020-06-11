package gui;

public interface GUIHandler {
    void addWszyscyWSklepie(int wszyscyklienci);
    void addWszyscyNaZakupach(int wszyscyzakupy);
    void addWszyscyWKolejce(int wszyscykolejkach);
    void addWszyscyObsluzeni(int wszyscyobsluzeni);
    void obrazowanieKolejek(String kolejki);
    void addLiczbeKolejek(int liczbaKolejek);
    void addLiczbaKasUprzywilejowanych(int liczbaKasUprz);
    void addMaxDlugoscKolejek(int maxKolejek);
    void addZakupionychTowarow(int kupionychTowarow);
    void addZwroconeTowary(int zwroconeTowary);
    void addKlienciUprzywilejowani(int klienciUprz);
}
