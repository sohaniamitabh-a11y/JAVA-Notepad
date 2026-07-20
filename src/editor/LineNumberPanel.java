package editor;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Java Features Used:
 *  - Custom painting (paintComponent override) → draws line numbers manually
 *  - Element / Document API  → reads line count from JTextArea's document model
 *  - DocumentListener        → repaints whenever text changes
 *  - FontMetrics             → measures character height for correct y-positioning
 *  - Polymorphism            → overrides JPanel's paintComponent()
 *
 * BUG FIX — y-position formula corrected:
 *  OLD (buggy): insetTop + (i * lineHeight) - fm.getDescent()
 *    → This over-counted by one lineHeight for the first line and subtracted
 *      only the descent, leaving an offset proportional to (ascent + leading).
 *
 *  NEW (correct): insetTop + fm.getAscent() + (i - 1) * lineHeight
 *    → The JTextArea draws the first line's baseline at (insetTop + ascent).
 *      Each subsequent line is exactly one lineHeight lower. By using (i-1)
 *      we start the first label at line 0 offset from the top, which matches
 *      the text area's own baseline exactly.
 *
 *  The scroll sync fix (viewport ChangeListener) is applied in TextEditorPanel,
 *  which owns the JScrollPane. The panel itself only needs to paint correctly
 *  for whatever vertical position Swing gives it via the row header mechanism.
 */
public class LineNumberPanel extends JPanel {

    private static final int   PANEL_WIDTH  = 50;
    private static final Color BG_COLOR     = new Color(240, 242, 246);
    private static final Color FG_COLOR     = new Color(130, 140, 160);
    private static final Color BORDER_CLR   = new Color(210, 215, 225);

    private final JTextArea textArea;

    public LineNumberPanel(JTextArea textArea) {
        this.textArea = textArea;
        setPreferredSize(new Dimension(PANEL_WIDTH, 0));
        setBackground(BG_COLOR);

        // Repaint every time the document changes (insertions, deletions)
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { repaint(); }
            public void removeUpdate(DocumentEvent e)  { repaint(); }
            public void changedUpdate(DocumentEvent e) { repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Right border separator line
        g2.setColor(BORDER_CLR);
        g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

        Font        font       = textArea.getFont();
        g2.setFont(new Font(font.getFamily(), Font.PLAIN, font.getSize() - 1));
        FontMetrics fm         = g2.getFontMetrics();
        int         lineHeight = fm.getHeight();
        int         insetTop   = textArea.getInsets().top;

        Element root  = textArea.getDocument().getDefaultRootElement();
        int     lines = root.getElementCount();

        g2.setColor(FG_COLOR);
        for (int i = 1; i <= lines; i++) {
            String num = String.valueOf(i);
            int x = getWidth() - fm.stringWidth(num) - 8;

            // FIXED formula: first line baseline = insetTop + ascent
            // Each subsequent line is exactly one lineHeight lower.
            int y = insetTop + fm.getAscent() + (i - 1) * lineHeight;

            g2.drawString(num, x, y);
        }
    }
}
