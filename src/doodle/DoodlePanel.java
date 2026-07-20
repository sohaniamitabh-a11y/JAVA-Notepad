package doodle;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Java Features Used:
 *  - BufferedImage / Graphics2D     → Off-screen pixel canvas for persistent drawing
 *  - RenderingHints                 → Anti-aliasing for smooth lines
 *  - BasicStroke                    → Controls line width and cap style
 *  - MouseAdapter / MouseMotionAdapter → Mouse event listeners (press, drag, release)
 *  - ComponentAdapter               → Detects panel resize to reinitialize canvas
 *  - JColorChooser                  → Color picker dialog
 *  - JSlider + ChangeListener       → Adjustable brush size
 *  - JToolBar                       → Horizontal toolbar with tool buttons
 *  - ButtonGroup + JToggleButton    → Radio-style tool selection (only one active)
 *  - Enum (Tool)                    → Type-safe representation of drawing tools
 *  - Inner class (CanvasPanel)      → Separates canvas painting from toolbar logic
 *  - BorderLayout                   → Places toolbar above canvas
 *
 * NEW Features Added:
 *  - exportCanvas()     → Saves the BufferedImage canvas to PNG or JPG via ImageIO.write()
 *                         WHY ImageIO: it's the standard Java API for reading/writing
 *                         raster images; no external libraries needed.
 *  - floodFill()        → BFS (breadth-first search) flood fill for the FILL tool
 *                         WHY BFS not recursion: recursive flood fill hits Java's
 *                         default stack limit (~500 frames) on large solid areas,
 *                         causing StackOverflowError. A queue-based BFS uses the
 *                         heap instead, which is far larger.
 *
 * BUG FIXES:
 *  - FILL tool was declared in the enum but never implemented → now wired to floodFill()
 *  - Color picker dialog passed null parent → now receives the parent Component
 *    so the dialog is properly centred and modal relative to the application window.
 */
public class DoodlePanel extends JPanel {

    // ── Tool Enum ────────────────────────────────────────────────
    private enum Tool { PEN, ERASER, LINE, RECTANGLE, OVAL, FILL }

    // ── State ────────────────────────────────────────────────────
    private Tool          currentTool  = Tool.PEN;
    private Color         currentColor = Color.BLACK;
    private int           brushSize    = 4;

    private BufferedImage canvas;
    private Graphics2D    g2d;
    private int           startX, startY;

    // Preview overlay (for shape drag previews — drawn on top, not committed)
    private BufferedImage preview;

    public DoodlePanel() {
        setLayout(new BorderLayout());
        add(buildToolBar(), BorderLayout.NORTH);
        CanvasPanel cp = new CanvasPanel();
        add(cp, BorderLayout.CENTER);
    }

    // ════════════════════════════════════════════════════════════
    //  TOOLBAR
    // ════════════════════════════════════════════════════════════
    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBackground(new Color(248, 249, 252));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 230)));

        ButtonGroup group = new ButtonGroup();

        bar.add(toolBtn("\u270F  Pen",    Tool.PEN,       group));
        bar.add(toolBtn("\u2B1C  Eraser", Tool.ERASER,    group));
        bar.addSeparator();
        bar.add(toolBtn("\u2571  Line",   Tool.LINE,      group));
        bar.add(toolBtn("\u25AD  Rect",   Tool.RECTANGLE, group));
        bar.add(toolBtn("\u25CB  Oval",   Tool.OVAL,      group));
        bar.add(toolBtn("\uD83E\uDEA3  Fill",  Tool.FILL,      group));   // BUG FIX: now wired up
        bar.addSeparator();

        // Brush size slider
        bar.add(new JLabel("  Size: "));
        JSlider sizeSlider = new JSlider(1, 40, brushSize);
        sizeSlider.setPreferredSize(new Dimension(120, 26));
        sizeSlider.setMaximumSize(new Dimension(120, 26));
        sizeSlider.setToolTipText("Brush size");
        sizeSlider.addChangeListener(e -> brushSize = sizeSlider.getValue());
        bar.add(sizeSlider);
        bar.addSeparator();

        // Color swatch button
        JButton colorBtn = new JButton("  \uD83C\uDFA8  Color  ");
        colorBtn.setToolTipText("Pick brush color");
        colorBtn.setBackground(currentColor);
        colorBtn.setForeground(Color.WHITE);
        colorBtn.setOpaque(true);
        colorBtn.addActionListener(e -> {
            // BUG FIX: pass 'this' as parent so dialog is modal to the app window,
            // not floating independently over whatever is on screen.
            Color chosen = JColorChooser.showDialog(this, "Choose Brush Color", currentColor);
            if (chosen != null) {
                currentColor = chosen;
                int brightness = (chosen.getRed() * 299 + chosen.getGreen() * 587 + chosen.getBlue() * 114) / 1000;
                colorBtn.setBackground(chosen);
                colorBtn.setForeground(brightness > 128 ? Color.BLACK : Color.WHITE);
            }
        });
        bar.add(colorBtn);
        bar.addSeparator();

        // Clear button
        JButton clearBtn = new JButton("  \uD83D\uDDD1  Clear  ");
        clearBtn.setForeground(new Color(180, 30, 30));
        clearBtn.setToolTipText("Clear entire canvas");
        clearBtn.addActionListener(e -> clearCanvas());
        bar.add(clearBtn);

        return bar;
    }

    private JToggleButton toolBtn(String label, Tool tool, ButtonGroup group) {
        JToggleButton btn = new JToggleButton(label);
        btn.setSelected(tool == Tool.PEN);
        btn.addActionListener(e -> currentTool = tool);
        group.add(btn);
        return btn;
    }

    // ════════════════════════════════════════════════════════════
    //  INNER CANVAS PANEL
    // ════════════════════════════════════════════════════════════
    private class CanvasPanel extends JPanel {

        CanvasPanel() {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            // Resize → reinitialize the BufferedImage to the new panel size
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    initCanvas(getWidth(), getHeight());
                    repaint();
                }
            });

            // ── Mouse Press ──────────────────────────────────────
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    startX = e.getX();
                    startY = e.getY();
                    if (currentTool == Tool.PEN || currentTool == Tool.ERASER) {
                        applyPen(e.getX(), e.getY(), e.getX(), e.getY());
                        repaint();
                    }
                }

                // ── Mouse Release (finalise shapes / trigger fill) ─
                @Override public void mouseReleased(MouseEvent e) {
                    if (g2d == null) return;
                    setupGraphics(g2d);
                    switch (currentTool) {
                        case LINE:
                            g2d.drawLine(startX, startY, e.getX(), e.getY());
                            break;
                        case RECTANGLE:
                            drawRectFromPoints(g2d, startX, startY, e.getX(), e.getY(), false);
                            break;
                        case OVAL:
                            drawOvalFromPoints(g2d, startX, startY, e.getX(), e.getY(), false);
                            break;
                        case FILL:
                            // BUG FIX: FILL was declared but never handled.
                            // On mouse release we know the exact click coordinates
                            // and can run the flood fill from that point.
                            floodFill(e.getX(), e.getY(), currentColor);
                            break;
                        default:
                            break;
                    }
                    preview = null; // discard the drag preview overlay
                    repaint();
                }
            });

            // ── Mouse Drag ───────────────────────────────────────
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (g2d == null) return;
                    switch (currentTool) {
                        case PEN:
                        case ERASER:
                            applyPen(startX, startY, e.getX(), e.getY());
                            startX = e.getX(); startY = e.getY();
                            break;
                        case LINE:
                        case RECTANGLE:
                        case OVAL:
                            buildPreview(e.getX(), e.getY());
                            break;
                        default:
                            break;
                    }
                    repaint();
                }
            });
        }

        // Paint: committed canvas layer + live preview overlay on top
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvas  != null) g.drawImage(canvas,  0, 0, null);
            if (preview != null) g.drawImage(preview, 0, 0, null);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DRAWING HELPERS
    // ════════════════════════════════════════════════════════════

    /** Initialises (or resizes) the off-screen BufferedImage, preserving old content. */
    private void initCanvas(int w, int h) {
        if (w <= 0 || h <= 0) return;
        BufferedImage fresh = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    ng    = fresh.createGraphics();
        ng.setColor(Color.WHITE);
        ng.fillRect(0, 0, w, h);
        if (canvas != null) ng.drawImage(canvas, 0, 0, null);
        ng.dispose();

        if (g2d != null) g2d.dispose();
        canvas = fresh;
        g2d    = canvas.createGraphics();
        setupGraphics(g2d);
    }

    /** Applies anti-aliasing and round stroke caps. */
    private void setupGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    /** Pen / eraser: draws a line segment between two drag points. */
    private void applyPen(int x1, int y1, int x2, int y2) {
        if (g2d == null) return;
        setupGraphics(g2d);
        g2d.setColor(currentTool == Tool.ERASER ? Color.WHITE : currentColor);
        g2d.drawLine(x1, y1, x2, y2);
    }

    /** Builds a temporary preview image for shape tools (shown during drag, not committed). */
    private void buildPreview(int ex, int ey) {
        if (canvas == null) return;
        preview = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = preview.createGraphics();
        setupGraphics(pg);
        pg.setColor(currentColor);
        switch (currentTool) {
            case LINE:      pg.drawLine(startX, startY, ex, ey);                       break;
            case RECTANGLE: drawRectFromPoints(pg, startX, startY, ex, ey, false);     break;
            case OVAL:      drawOvalFromPoints(pg, startX, startY, ex, ey, false);     break;
            default:        break;
        }
        pg.dispose();
    }

    private void drawRectFromPoints(Graphics2D g, int x1, int y1, int x2, int y2, boolean filled) {
        int x = Math.min(x1, x2), y = Math.min(y1, y2);
        int w = Math.abs(x2 - x1),  h = Math.abs(y2 - y1);
        if (filled) g.fillRect(x, y, w, h); else g.drawRect(x, y, w, h);
    }

    private void drawOvalFromPoints(Graphics2D g, int x1, int y1, int x2, int y2, boolean filled) {
        int x = Math.min(x1, x2), y = Math.min(y1, y2);
        int w = Math.abs(x2 - x1),  h = Math.abs(y2 - y1);
        if (filled) g.fillOval(x, y, w, h); else g.drawOval(x, y, w, h);
    }

    /**
     * BFS Flood Fill — fills contiguous pixels of the same colour as the seed
     * pixel with currentColor.
     *
     * WHY BFS (queue) instead of recursion:
     *   Recursive flood fill calls itself once per pixel. On a 1000×800 canvas
     *   a large solid region has 800,000 pixels — far beyond Java's default
     *   thread stack depth (~1,000 frames), causing StackOverflowError.
     *   A queue stores pending pixels on the heap, which is gigabytes deep.
     *
     * WHY check targetRGB == fillRGB early:
     *   If the user clicks on a pixel already the same colour as the brush,
     *   every pixel on the canvas would match, flooding the entire image.
     *   The early-exit guard prevents that.
     *
     * @param px         Seed x coordinate (where user clicked)
     * @param py         Seed y coordinate
     * @param fillColor  Colour to flood with
     */
    private void floodFill(int px, int py, Color fillColor) {
        if (canvas == null) return;
        if (px < 0 || px >= canvas.getWidth() || py < 0 || py >= canvas.getHeight()) return;

        int targetRGB = canvas.getRGB(px, py);
        int fillRGB   = fillColor.getRGB();
        if (targetRGB == fillRGB) return;   // already the desired colour → nothing to do

        int w = canvas.getWidth();
        int h = canvas.getHeight();

        // Queue stores pixel coordinates as encoded ints (y*width + x) to avoid
        // creating millions of Point objects and reduce GC pressure.
        Queue<Integer> queue = new LinkedList<>();
        queue.add(py * w + px);

        while (!queue.isEmpty()) {
            int idx = queue.poll();
            int x   = idx % w;
            int y   = idx / w;

            if (x < 0 || x >= w || y < 0 || y >= h)    continue;
            if (canvas.getRGB(x, y) != targetRGB)        continue;

            canvas.setRGB(x, y, fillRGB);

            // 4-connected neighbours (no diagonals — matches MS-Paint style fill)
            queue.add(y * w + (x + 1));
            queue.add(y * w + (x - 1));
            queue.add((y + 1) * w + x);
            queue.add((y - 1) * w + x);
        }
    }

    // ────────────────────────────────────────────────────────────
    //  EXPORT AS IMAGE
    // ────────────────────────────────────────────────────────────
    /**
     * Exports the current canvas to a PNG or JPG file chosen by the user.
     *
     * WHY ImageIO.write():
     *   javax.imageio.ImageIO is the standard Java SE API for encoding images.
     *   It supports PNG (lossless, preserves transparency) and JPG (lossy, smaller).
     *   The format string ("png" / "jpg") tells the codec which encoder to use.
     *
     * WHY convert to TYPE_INT_RGB for JPG:
     *   Our canvas is TYPE_INT_ARGB (has an alpha channel). The JPG codec does NOT
     *   support transparency and throws an exception if you try to write an ARGB
     *   image as JPG. We blit the canvas onto a plain white RGB image first.
     *
     * @param parent  The parent component for the JFileChooser dialog (centres it).
     */
    public void exportCanvas(Component parent) {
        if (canvas == null) {
            JOptionPane.showMessageDialog(parent,
                "Nothing to export — canvas is empty.",
                "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // ── File chooser ─────────────────────────────────────────
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Doodle as Image");
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("PNG Image (*.png)", "png"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JPG Image (*.jpg)", "jpg"));
        chooser.setFileFilter(chooser.getChoosableFileFilters()[1]); // default: PNG

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File   chosen    = chooser.getSelectedFile();
        String filterDesc = chooser.getFileFilter().getDescription();
        // Determine format from the selected filter description
        String format    = filterDesc.startsWith("JPG") ? "jpg" : "png";

        // Append the correct extension if the user omitted it
        String name = chosen.getName().toLowerCase();
        if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
            chosen = new File(chosen.getAbsolutePath() + "." + format);
        }

        // ── Image preparation ────────────────────────────────────
        BufferedImage toSave;
        if (format.equals("jpg")) {
            // JPG encoder cannot handle ARGB — convert to opaque RGB
            toSave = new BufferedImage(canvas.getWidth(), canvas.getHeight(),
                                       BufferedImage.TYPE_INT_RGB);
            Graphics2D g = toSave.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, toSave.getWidth(), toSave.getHeight());
            g.drawImage(canvas, 0, 0, null);
            g.dispose();
        } else {
            toSave = canvas;   // PNG supports ARGB natively
        }

        // ── Write to disk ────────────────────────────────────────
        try {
            boolean ok = ImageIO.write(toSave, format, chosen);
            if (!ok) {
                // ImageIO.write returns false if no suitable encoder was found
                JOptionPane.showMessageDialog(parent,
                    "Could not find an encoder for format: " + format,
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(parent,
                    "Doodle saved to:\n" + chosen.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                "Failed to write file:\n" + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Public API ───────────────────────────────────────────────
    public void clearCanvas() {
        if (g2d == null || canvas == null) return;
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        preview = null;
        repaint();
    }

    public Color getCurrentColor()        { return currentColor; }
    public void  setCurrentColor(Color c) { this.currentColor = c; }
}
