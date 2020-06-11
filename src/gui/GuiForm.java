package gui;

import com.sun.javaws.util.JfxHelper;

import javax.swing.*;

public class GuiForm{
    private JPanel mainPanel;
    private JPanel panelSklep;
    private JLabel statsKasa;
    private JLabel statsKasaUprzywilejowani;
    private JLabel statsKasaKupionych;
    private JLabel statsKasaZwroty;
    private JLabel statsKolejka;
    private JLabel statsKolejkaLudzieWKolejce;
    private JLabel statsKolejkaIloscKolejek;
    private JLabel statsSklep;
    private JLabel statsSklepWSklepie;
    private JLabel statsSklepWKolejkach;
    private JLabel statsSklepWTrakcieZakupow;

    public GuiForm() {

    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static void main(String[] args) {
        System.out.println("RUN GUI");
        JFrame frame = new JFrame("App");
        frame.setContentPane(new GuiForm().getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
