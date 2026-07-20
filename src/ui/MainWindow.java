package ui;

import editor.TextEditorPanel;
import doodle.DoodlePanel;
import fileHandling.FileHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Java Features Used:
 *  - JFrame            → Top-level window container
 *  - JTabbedPane       → Switchable panels (Editor / Doodle)
 *  - BorderLayout      → Layout manager for component placement
 *  - BorderFactory     → Creates border styles for status bar
 *  - Inheritance       → MainWindow extends JFrame
 *
 * BUG FIX — Safe window closing:
 *  OLD: setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
 *    → Closes the app immediately when the user clicks the X button,
 *      skipping the unsaved-changes check entirely.
 *
 *  NEW: setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE)
 *       + WindowAdapter.windowClosing() → delegates to fileHandler.tryExit()
 *    → tryExit() runs checkUnsavedChanges() first. If the user cancels the
 *      save dialog, the window stays open. Only a confirmed safe-to-exit
 *      path results in System.exit(0).
 *
 *  WHY WindowAdapter instead of WindowListener interface:
 *    WindowListener has 7 methods; we only need windowClosing(). Extending
 *    WindowAdapter (which provides empty default implementations of all 7)
 *    lets us override just the one we care about.
 *
 * CHANGE: AppMenuBar now receives parentFrame (this) so its dialogs are
 *         properly parented and centred on the application window.
 */
public class MainWindow extends JFrame {

    private final TextEditorPanel textEditorPanel;
    private final DoodlePanel     doodlePanel;
    private final FileHandler     fileHandler;
    private final JTabbedPane     tabbedPane;

    public MainWindow() {
        // ── Window Setup ────────────────────────────────────────
        setTitle("Java Notepad");
        setSize(960, 700);
        setMinimumSize(new Dimension(700, 500));

        // BUG FIX: DO_NOTHING_ON_CLOSE + WindowAdapter replaces EXIT_ON_CLOSE
        // so the close button goes through the unsaved-changes guard.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fileHandler.tryExit();   // prompts to save if modified; exits if safe
            }
        });

        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Core Panels ─────────────────────────────────────────
        textEditorPanel = new TextEditorPanel();
        doodlePanel     = new DoodlePanel();
        fileHandler     = new FileHandler(this, textEditorPanel);

        // ── Tabbed Pane ─────────────────────────────────────────
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("\uD83D\uDCDD  Text Editor", textEditorPanel);
        tabbedPane.addTab("\uD83C\uDFA8  Doodle Pad",  doodlePanel);
        add(tabbedPane, BorderLayout.CENTER);

        // ── Menu Bar ────────────────────────────────────────────
        // CHANGE: pass 'this' as parentFrame so AppMenuBar can centre its dialogs.
        AppMenuBar appMenuBar = new AppMenuBar(this, fileHandler, textEditorPanel,
                                               doodlePanel, tabbedPane);
        setJMenuBar(appMenuBar.createMenuBar());

        // ── Status Bar ──────────────────────────────────────────
        JLabel statusBar = new JLabel("  Ready  |  Line: 1  |  Col: 1  |  Chars: 0  |  Zoom: 14pt");
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        add(statusBar, BorderLayout.SOUTH);
        textEditorPanel.setStatusBar(statusBar);
    }
}
