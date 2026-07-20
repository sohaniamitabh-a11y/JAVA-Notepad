package ui;

import editor.TextEditorPanel;
import doodle.DoodlePanel;
import fileHandling.FileHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Java Features Used:
 *  - JMenuBar / JMenu / JMenuItem   → Menu structure
 *  - JCheckBoxMenuItem              → Toggleable menu items (Word Wrap)
 *  - KeyStroke / accelerators       → Keyboard shortcuts (Ctrl+S, etc.)
 *  - JColorChooser                  → Built-in color picker dialog
 *  - JOptionPane                    → Simple dialogs (font picker)
 *  - GraphicsEnvironment            → Lists all system fonts
 *  - Lambda expressions             → ActionListeners as one-liners
 *  - Mnemonic keys                  → Alt+F opens File menu, etc.
 *
 * NEW features added:
 *  - Background Color picker  → Format menu → calls textEditorPanel.setTextBackground()
 *  - Zoom In  (Ctrl +=)       → Format menu → calls textEditorPanel.zoomIn()
 *  - Zoom Out (Ctrl +-)       → Format menu → calls textEditorPanel.zoomOut()
 *  - Export as Image          → Doodle menu → calls doodlePanel.exportCanvas()
 *
 * BUG FIX:
 *  - JColorChooser dialogs previously passed null as parent → now pass parentFrame
 *    so the dialog is properly centred on screen and behaves as a modal child
 *    of the main application window.
 */
public class AppMenuBar {

    private final JFrame            parentFrame;      // NEW: needed for correct dialog parenting
    private final FileHandler       fileHandler;
    private final TextEditorPanel   textEditorPanel;
    private final DoodlePanel       doodlePanel;
    private final JTabbedPane       tabbedPane;

    public AppMenuBar(JFrame parentFrame, FileHandler fh, TextEditorPanel tep,
                      DoodlePanel dp, JTabbedPane tp) {
        this.parentFrame     = parentFrame;
        this.fileHandler     = fh;
        this.textEditorPanel = tep;
        this.doodlePanel     = dp;
        this.tabbedPane      = tp;
    }

    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(buildFileMenu());
        menuBar.add(buildEditMenu());
        menuBar.add(buildFormatMenu());
        menuBar.add(buildDoodleMenu());
        return menuBar;
    }

    // ── FILE MENU ───────────────────────────────────────────────────────────
    private JMenu buildFileMenu() {
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);

        file.add(item("New",      KeyEvent.VK_N, 0,                    e -> fileHandler.newFile()));
        file.add(item("Open\u2026",  KeyEvent.VK_O, 0,                 e -> fileHandler.openFile()));
        file.addSeparator();
        file.add(item("Save",     KeyEvent.VK_S, 0,                    e -> fileHandler.saveFile()));
        file.add(item("Save As\u2026", KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK, e -> fileHandler.saveFileAs()));
        file.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        // BUG FIX: previously System.exit(0) bypassed the unsaved-changes check.
        // Now we delegate to FileHandler.tryExit() which prompts the user first.
        exitItem.addActionListener(e -> fileHandler.tryExit());
        file.add(exitItem);
        return file;
    }

    // ── EDIT MENU ───────────────────────────────────────────────────────────
    private JMenu buildEditMenu() {
        JMenu edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);

        edit.add(item("Cut",        KeyEvent.VK_X, 0, e -> textEditorPanel.getTextArea().cut()));
        edit.add(item("Copy",       KeyEvent.VK_C, 0, e -> textEditorPanel.getTextArea().copy()));
        edit.add(item("Paste",      KeyEvent.VK_V, 0, e -> textEditorPanel.getTextArea().paste()));
        edit.addSeparator();
        edit.add(item("Select All", KeyEvent.VK_A, 0, e -> textEditorPanel.getTextArea().selectAll()));
        edit.addSeparator();
        edit.add(item("Undo",       KeyEvent.VK_Z, 0, e -> textEditorPanel.undo()));
        edit.add(item("Redo",       KeyEvent.VK_Y, 0, e -> textEditorPanel.redo()));
        return edit;
    }

    // ── FORMAT MENU ─────────────────────────────────────────────────────────
    private JMenu buildFormatMenu() {
        JMenu format = new JMenu("Format");
        format.setMnemonic(KeyEvent.VK_O);

        // Font family picker
        JMenuItem fontItem = new JMenuItem("Font\u2026");
        fontItem.addActionListener(e -> showFontDialog());
        format.add(fontItem);

        // Word wrap toggle
        JCheckBoxMenuItem wordWrap = new JCheckBoxMenuItem("Word Wrap", true);
        wordWrap.addActionListener(e -> textEditorPanel.toggleWordWrap(wordWrap.isSelected()));
        format.add(wordWrap);

        // Font size submenu
        JMenu fontSizeMenu = new JMenu("Font Size");
        int[] sizes = {10, 12, 14, 16, 18, 20, 24, 28, 32};
        for (int size : sizes) {
            JMenuItem sizeItem = new JMenuItem(size + "pt");
            sizeItem.addActionListener(e -> {
                Font f = textEditorPanel.getTextArea().getFont();
                textEditorPanel.getTextArea().setFont(f.deriveFont((float) size));
            });
            fontSizeMenu.add(sizeItem);
        }
        format.add(fontSizeMenu);
        format.addSeparator();

        // ── NEW: Zoom In / Zoom Out ──────────────────────────────
        // WHY show in menu: accelerators on menu items are registered app-wide
        // (at the JMenuBar level), so they work without needing focus on the text area.
        // The matching InputMap bindings in TextEditorPanel cover the case where
        // the user's key lands in the text area directly (both routes call the same method).
        JMenuItem zoomInItem  = item("Zoom In",  KeyEvent.VK_EQUALS, 0, e -> textEditorPanel.zoomIn());
        JMenuItem zoomOutItem = item("Zoom Out", KeyEvent.VK_MINUS,  0, e -> textEditorPanel.zoomOut());
        zoomInItem.setText("Zoom In  (Ctrl +=)");
        zoomOutItem.setText("Zoom Out  (Ctrl +-)");
        format.add(zoomInItem);
        format.add(zoomOutItem);
        format.addSeparator();

        // ── NEW: Background Color picker ────────────────────────
        // WHY JColorChooser.showDialog: it's the built-in Swing dialog that
        // provides RGB sliders, a palette, and an eye-dropper in one call.
        // We pass parentFrame so the dialog is centred on the app window.
        JMenuItem bgColorItem = new JMenuItem("Background Color\u2026");
        bgColorItem.addActionListener(e -> {
            Color current = textEditorPanel.getTextArea().getBackground();
            Color chosen  = JColorChooser.showDialog(parentFrame, "Choose Background Color", current);
            if (chosen != null) textEditorPanel.setTextBackground(chosen);
        });
        format.add(bgColorItem);

        return format;
    }

    // ── DOODLE MENU ─────────────────────────────────────────────────────────
    private JMenu buildDoodleMenu() {
        JMenu doodle = new JMenu("Doodle");
        doodle.setMnemonic(KeyEvent.VK_D);

        JMenuItem colorItem = new JMenuItem("Pick Color\u2026");
        colorItem.addActionListener(e -> {
            // BUG FIX: was passing null → now passes parentFrame for proper modality
            Color chosen = JColorChooser.showDialog(parentFrame, "Choose Brush Color",
                                                    doodlePanel.getCurrentColor());
            if (chosen != null) doodlePanel.setCurrentColor(chosen);
            tabbedPane.setSelectedIndex(1);
        });

        JMenuItem clearItem = new JMenuItem("Clear Canvas");
        clearItem.addActionListener(e -> {
            doodlePanel.clearCanvas();
            tabbedPane.setSelectedIndex(1);
        });

        // ── NEW: Export canvas as image ──────────────────────────
        // WHY in Doodle menu: the export is scoped to the doodle canvas, so it
        // logically belongs here rather than in File (which handles text files).
        JMenuItem exportItem = new JMenuItem("Export as Image\u2026");
        exportItem.addActionListener(e -> {
            tabbedPane.setSelectedIndex(1);   // switch to doodle tab so user sees what they're saving
            doodlePanel.exportCanvas(parentFrame);
        });

        doodle.add(colorItem);
        doodle.addSeparator();
        doodle.add(clearItem);
        doodle.addSeparator();
        doodle.add(exportItem);
        return doodle;
    }

    // ── FONT DIALOG ─────────────────────────────────────────────────────────
    private void showFontDialog() {
        String[] fonts = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames();

        String current  = textEditorPanel.getTextArea().getFont().getFamily();
        String selected = (String) JOptionPane.showInputDialog(
            parentFrame,          // BUG FIX: was null
            "Choose font family:",
            "Font Selector",
            JOptionPane.PLAIN_MESSAGE,
            null,
            fonts,
            current
        );
        if (selected != null) {
            Font oldFont = textEditorPanel.getTextArea().getFont();
            textEditorPanel.getTextArea().setFont(
                new Font(selected, oldFont.getStyle(), oldFont.getSize())
            );
        }
    }

    // ── HELPER: create menu item with Ctrl+key shortcut ──────────────────────
    private JMenuItem item(String label, int key, int extraMask,
                           java.awt.event.ActionListener listener) {
        JMenuItem mi = new JMenuItem(label);
        mi.setAccelerator(KeyStroke.getKeyStroke(key, KeyEvent.CTRL_DOWN_MASK | extraMask));
        mi.addActionListener(listener);
        return mi;
    }
}
