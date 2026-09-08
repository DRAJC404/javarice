import Views.Loginframe;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            Loginframe loginFrame = new Loginframe();

            loginFrame.setVisible(true);
        });
    }
}
