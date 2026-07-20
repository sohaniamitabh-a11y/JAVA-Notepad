package editor;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Java Features Used:
 *  - JTextArea          → Multi-line text input component
 *  - JScrollPane        → Adds scroll bars around JTextArea
 *  - UndoManager        → Built-in undo/redo support
 *  - DocumentListener   → Fires on every text change (insert/remove/update)
 *  - CaretListener      → Fires when cursor position changes → updates status
 *  - Insets             → Adds inner padding to text area
 *  - Composition        → TextEditorPanel "has-a" JTextArea (composition over inheritance)
 *
 * NEW Features Added:
 *  - InputMap/ActionMap → Key bindings for Ctrl+= (zoom in) and Ctrl+- (zoom out)
 *                         WHY: InputMap lets us intercept keys while the text area is
 *                         focused without a raw KeyListener that would interfere with typing.
 *  - setTextBackground()→ Changes the editor background colour + auto-flips caret/text colour
 *                         for contrast on dark backgrounds.
 *  - Viewport ChangeListener → Repaints line numbers on every scroll event
 *                         BUG FIX: Without this, line numbers drifted out of alignment
 *                         after scrolling because DocumentListener only fires on text changes.
 */
public class TextEditorPanel extends JPanel {

    private final JTextArea   textArea;
    private final UndoManager undoManager;
    private       JLabel      statusBar;
    private       boolean     modified        = false;
    private       int         currentFontSize = 14;   // tracks zoom level separately from Font object

    public TextEditorPanel() {
        setLayout(new BorderLayout());

        // ── Text Area ────────────────────────────────────────────
        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, currentFontSize));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setTabSize(4);
        textArea.setMargin(new Insets(6, 8, 6, 8));
        textArea.setBackground(new Color(253, 253, 253));
        textArea.setCaretColor(new Color(30, 30, 30));

        // ── Undo / Redo ──────────────────────────────────────────
        undoManager = new UndoManager();
        undoManager.setLimit(200);
        textArea.getDocument().addUndoableEditListener(undoManager);

        // ── Document Change Listener → update status + modified flag ─
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { modified = true; updateStatus(); }
            public void removeUpdate(DocumentEvent e)  { modified = true; updateStatus(); }
            public void changedUpdate(DocumentEvent e) { updateStatus(); }
        });

        // ── Caret Listener → update line/col in real time ────────
        textArea.addCaretListener(e -> updateStatus());

        // ── Zoom Key Bindings ────────────────────────────────────
        // WHY InputMap instead of KeyListener:
        //   InputMap is declarative — bind a KeyStroke to a named Action.
        //   WHEN_FOCUSED means it only fires when textArea has keyboard focus,
        //   so it won't clash with other focused components.
        InputMap  im = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textArea.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK), "zoom-in");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,  KeyEvent.CTRL_DOWN_MASK), "zoom-out");

        am.put("zoom-in",  new AbstractAction() {
            public void actionPerformed(ActionEvent e) { zoomIn(); }
        });
        am.put("zoom-out", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { zoomOut(); }
        });

        // ── Scroll Pane with Line Numbers ────────────────────────
        LineNumberPanel lineNumberPanel = new LineNumberPanel(textArea);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumberPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // BUG FIX: viewport ChangeListener fires on every scroll event, keeping
        // line numbers painted at the correct position even while scrolling.
        scrollPane.getViewport().addChangeListener(e -> lineNumberPanel.repaint());

        add(scrollPane, BorderLayout.CENTER);
    }

    // ── Public API ───────────────────────────────────────────────
    public JTextArea getTextArea()  { return textArea; }
    public boolean   isModified()   { return modified; }

    public void setModified(boolean m) { this.modified = m; }

    public void setStatusBar(JLabel bar) {
        this.statusBar = bar;
        updateStatus();
    }

    public void toggleWordWrap(boolean wrap) {
        textArea.setLineWrap(wrap);
        textArea.setWrapStyleWord(wrap);
    }

    public void undo() { if (undoManager.canUndo()) undoManager.undo(); }
    public void redo() { if (undoManager.canRedo()) undoManager.redo(); }

    /**
     * Zoom In: increase font size by 2pt, capped at 48pt.
     * WHY deriveFont(): preserves the current font family and style (bold/italic),
     * only changing the point size. new Font() would reset those attributes.
     */
    public void zoomIn() {
        if (currentFontSize >= 48) return;
        currentFontSize += 2;
        textArea.setFont(textArea.getFont().deriveFont((float) currentFontSize));
        updateStatus();
    }

    /** Zoom Out: decrease font size by 2pt, minimum 8pt. */
    public void zoomOut() {
        if (currentFontSize <= 8) return;
        currentFontSize -= 2;
        textArea.setFont(textArea.getFont().deriveFont((float) currentFontSize));
        updateStatus();
    }

    /**
     * Sets the background colour of the text editing area.
     * WHY auto-flip: on a dark background, a black caret and black text become
     * invisible. We use the ITU-R BT.601 luma formula to compute perceived
     * brightness and switch caret + text to white when the background is dark.
     */
    public void setTextBackground(Color c) {
        textArea.setBackground(c);
        int brightness = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
        textArea.setCaretColor(brightness > 128 ? Color.BLACK : Color.WHITE);
        textArea.setForeground(brightness > 128 ? Color.BLACK : new Color(220, 220, 220));
    }

    // ── Status Bar Update ────────────────────────────────────────
    private void updateStatus() {
        if (statusBar == null) return;
        try {
            int pos   = textArea.getCaretPosition();
            int line  = textArea.getLineOfOffset(pos) + 1;
            int col   = pos - textArea.getLineStartOffset(line - 1) + 1;
            int chars = textArea.getText().length();
            statusBar.setText(String.format(
                "  Line: %d  |  Col: %d  |  Chars: %d  |  Zoom: %dpt  %s",
                line, col, chars, currentFontSize, modified ? "  \u25CF  Unsaved" : ""
            ));
        } catch (Exception ignored) {}
    }
}
