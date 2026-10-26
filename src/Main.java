import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

// The only entry point of the application. Run this file.
public class Main {

    public static void main(String[] args) {

        // Modern look for every button, field, drop-down and scrollbar
        Theme.install();

        // Create / upgrade the database tables before the window opens
        String databaseError = DatabaseSetup.run();

        // Train the AI category model (takes under a second)
        SmartClassifier.trainInBackground();

        SwingUtilities.invokeLater(() -> {

            MainFrame frame = new MainFrame();

            // Screens available before login
            frame.addScreen(MainFrame.WELCOME, new Welcome(frame));
            frame.addScreen(MainFrame.STUDENT_LOGIN, new Login(frame));
            frame.addScreen(MainFrame.STUDENT_REGISTER, new Register(frame));
            frame.addScreen(MainFrame.ADMIN_LOGIN, new AdminLogin(frame));
            frame.addScreen(MainFrame.ADMIN_REGISTER, new AdminRegister(frame));
            frame.addScreen(MainFrame.TRACK_COMPLAINT, new TrackComplaint(frame));

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
