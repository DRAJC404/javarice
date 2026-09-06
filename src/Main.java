import Views.Loginframe;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            Loginframe login = new Loginframe();

            login.setVisible(true);
        });
    }
}