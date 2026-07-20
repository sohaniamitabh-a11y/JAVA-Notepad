 bugfix/fixes
package main;

import ui.MainWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Java Features Used:
 *  - SwingUtilities.invokeLater() → Thread safety for GUI initialization
 *  - UIManager.setLookAndFeel()   → Platform-native appearance
 *  - Lambda expressions (Java 8+)  → Concise Runnable for invokeLater
 */
public class Main {
    public static void main(String[] args) {
        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // All Swing UI must be created on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }

package main;

import ui.MainWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Java Features Used:
 *  - SwingUtilities.invokeLater() → Thread safety for GUI initialization
 *  - UIManager.setLookAndFeel()   → Platform-native appearance
 *  - Lambda expressions (Java 8+)  → Concise Runnable for invokeLater
 */
public class Main {
    public static void main(String[] args) {
        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // All Swing UI must be created on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
 main
}