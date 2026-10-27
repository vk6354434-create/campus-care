import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

// The only entry point of the application. Run this file.
public class Main {

    public static void main(String[] args) {

        // Modern look for every button, field, drop-down and scrollbar
        // (also loads the saved Default / Light / Dark theme)
        Theme.install();

        // Create / upgrade the database tables before the window opens
        String databaseError = DatabaseSetup.run();

        // Train the AI category model (takes under a second)
        SmartClassifier.trainInBackground();

        SwingUtilities.invokeLater(() -> {

            MainFrame frame = new MainFrame();

            // Screens available before login (Home, Login, Register, Track)
            frame.buildPublicScreens();

            // Dashboard screens are added by MainFrame after login

            frame.start(MainFrame.WELCOME);

            if (databaseError != null) {
                JOptionPane.showMessageDialog(frame,
                        "Could not connect to the database.\n\n"
                        + databaseError + "\n\n"
                        + "Check that MySQL is running and that the username "
                        + "and password in db.properties are correct.",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
