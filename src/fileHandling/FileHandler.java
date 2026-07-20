package fileHandling;

import editor.TextEditorPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Java Features Used:
 *  - java.io.File              → Represents file paths on disk
 *  - java.io.BufferedWriter    → Efficient character-by-character file writing
 *  - java.io.FileWriter        → Writes characters to a file
 *  - java.nio.file.Files       → readAllBytes() for simple whole-file reading (NIO)
 *  - StandardCharsets.UTF_8    → Explicit charset to avoid platform encoding issues
 *  - JFileChooser              → Native-looking open/save dialogs
 *  - FileNameExtensionFilter   → Restricts chooser to .txt files
 *  - JOptionPane               → Unsaved-changes warning dialogs
 *  - try-with-resources        → Auto-closes BufferedWriter even on exception
 *  - Exception handling        → try/catch for IOException
 *  - Encapsulation             → currentFile is private; exposed only via getter
 *
 * NEW / FIXED:
 *  - tryExit()          → public method called by the window-closing listener and
 *                         Exit menu item. Runs the unsaved-changes check BEFORE
 *                         exiting, so no work is lost.
 *                         WHY NEW: previously System.exit(0) was called directly,
 *                         bypassing checkUnsavedChanges() entirely.
 *  - checkUnsavedChanges() made package-private so MainWindow's WindowAdapter can
 *    delegate through tryExit() without needing a separate flag.
 */
public class FileHandler {

    private final JFrame          parentFrame;
    private final TextEditorPanel editorPanel;
    private       File            currentFile = null;

    public FileHandler(JFrame parent, TextEditorPanel panel) {
        this.parentFrame = parent;
        this.editorPanel = panel;
    }

    // ── NEW FILE ─────────────────────────────────────────────────
    public void newFile() {
        if (!checkUnsavedChanges()) return;
        editorPanel.getTextArea().setText("");
        editorPanel.setModified(false);
        currentFile = null;
        parentFrame.setTitle("Java Notepad  \u2014  New File");
    }

    // ── OPEN ─────────────────────────────────────────────────────
    public void openFile() {
        if (!checkUnsavedChanges()) return;

        JFileChooser chooser = buildChooser();
        if (chooser.showOpenDialog(parentFrame) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            byte[]  bytes   = Files.readAllBytes(file.toPath());
            String  content = new String(bytes, StandardCharsets.UTF_8);

            editorPanel.getTextArea().setText(content);
            editorPanel.getTextArea().setCaretPosition(0);
            editorPanel.setModified(false);
            currentFile = file;
            parentFrame.setTitle("Java Notepad  \u2014  " + file.getName());

        } catch (IOException ex) {
            showError("Could not open file:\n" + ex.getMessage());
        }
    }

    // ── SAVE ─────────────────────────────────────────────────────
    public void saveFile() {
        if (currentFile == null) saveFileAs();
        else                     writeToDisk(currentFile);
    }

    // ── SAVE AS ──────────────────────────────────────────────────
    public void saveFileAs() {
        JFileChooser chooser = buildChooser();
        if (chooser.showSaveDialog(parentFrame) != JFileChooser.APPROVE_OPTION) return;

        File chosen = chooser.getSelectedFile();
        if (!chosen.getName().toLowerCase().endsWith(".txt")) {
            chosen = new File(chosen.getAbsolutePath() + ".txt");
        }
        writeToDisk(chosen);
        currentFile = chosen;
        parentFrame.setTitle("Java Notepad  \u2014  " + currentFile.getName());
    }

    // ── EXIT (NEW) ────────────────────────────────────────────────
    /**
     * Called by both the Exit menu item and the window-closing listener.
     * Runs the unsaved-changes check first, then exits only if it's safe to do so.
     *
     * WHY needed: previously the Exit item called System.exit(0) directly, and
     * the window used JFrame.EXIT_ON_CLOSE — both bypassed checkUnsavedChanges().
     * Now both paths funnel through here, so the user is always warned.
     */
    public void tryExit() {
        if (checkUnsavedChanges()) {
            System.exit(0);
        }
        // If checkUnsavedChanges() returns false the user clicked Cancel → do nothing
    }

    // ── INTERNAL HELPERS ─────────────────────────────────────────

    /**
     * Returns true if it's safe to proceed (no unsaved changes, or user chose to discard/save).
     * Returns false if the user clicked Cancel.
     */
    boolean checkUnsavedChanges() {
        if (!editorPanel.isModified()) return true;

        int choice = JOptionPane.showConfirmDialog(
            parentFrame,
            "You have unsaved changes.\nSave before continuing?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION)  { saveFile(); return true; }
        if (choice == JOptionPane.NO_OPTION)   { return true;  }
        return false; // CANCEL
    }

    /** Writes the current editor content to a File using try-with-resources. */
    private void writeToDisk(File file) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(editorPanel.getTextArea().getText());
            editorPanel.setModified(false);
            parentFrame.setTitle("Java Notepad  \u2014  " + file.getName());
        } catch (IOException ex) {
            showError("Could not save file:\n" + ex.getMessage());
        }
    }

    private JFileChooser buildChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        if (currentFile != null) chooser.setCurrentDirectory(currentFile.getParentFile());
        return chooser;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(parentFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public File getCurrentFile() { return currentFile; }
}
