package gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class GUIController implements GUIHandler {

    @FXML
    private Label wszyscywsklepie;

    @FXML
    private Label wszyscyNaZakupach;

    @FXML
    private Label wszyscyWKolejkach;

    @FXML
    private Label wszyscyObsluzeniKlienci;

    @FXML
    private Label obrazowanieKolejek;

    @FXML
    private Label liczbaKolejekNorm;

    @FXML
    private Label liczbaKasUprzywilejowanych;

    @FXML
    private Label maxDlugoscKolejek;

    @FXML
    private Label zakupionychTowarow;

    @FXML
    private Label zwroconeTowaryKasa;

    @FXML
    private Label klienciUprzywilejowani;

    public void addWszyscyWSklepie(int wszyscyklienci){
        Platform.runLater(()->{wszyscywsklepie.setText("Klienci w sklepie: "+wszyscyklienci);});
    }

    @Override
    public void addWszyscyNaZakupach(int wszyscyzakupy) {
        Platform.runLater(()->{wszyscyNaZakupach.setText("Ludzie w trakcie zakupów: "+wszyscyzakupy);});
    }

    @Override
    public void addWszyscyWKolejce(int wszyscykolejkach) {
        Platform.runLater(()->{wszyscyWKolejkach.setText("Ludzie w kolejkach: "+wszyscykolejkach);});
    }

    @Override
    public void addWszyscyObsluzeni(int wszyscyobsluzeni) {
        Platform.runLater(()->{wszyscyObsluzeniKlienci.setText("Ludzie obsłużeni: "+wszyscyobsluzeni);});
    }

    @Override
    public void obrazowanieKolejek(String kolejki) {
        Platform.runLater(()->obrazowanieKolejek.setText(kolejki));
    }

    @Override
    public void addLiczbeKolejek(int liczbaKolejek) {
        Platform.runLater(()->{liczbaKolejekNorm.setText("Liczba kolejek: "+liczbaKolejek);});
    }

    @Override
    public void addLiczbaKasUprzywilejowanych(int liczbaKasUprz) {
        Platform.runLater(()->{liczbaKasUprzywilejowanych.setText("Liczba kolejek uprzywilejowanych: "+liczbaKasUprz);});
    }

    @Override
    public void addMaxDlugoscKolejek(int maxKolejek) {
        Platform.runLater(()->{maxDlugoscKolejek.setText("Maksymalna liczba kolejek: "+maxKolejek);});
    }

    @Override
    public void addZakupionychTowarow(int kupionychTowarow) {
        Platform.runLater(()->{zakupionychTowarow.setText("Liczba zakupionych towarów: "+kupionychTowarow);});
    }

    @Override
    public void addZwroconeTowary(int zwroconeTowary) {
        Platform.runLater(()->{zwroconeTowaryKasa.setText("Liczba zwróconych towarów: "+zwroconeTowary);});
    }

    @Override
    public void addKlienciUprzywilejowani(int klienciUprz) {
        Platform.runLater(()->{klienciUprzywilejowani.setText("Klienci obsłużeni w kasie uprzywilejowanej: "+klienciUprz);});
    }

}
