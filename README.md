# Java Notepad

A desktop text editor and drawing application built with Java SE (Swing/AWT)
as a Semester II mini-project.

---

## Features

### Text Editor
- Open, save, and create plain-text files (`.txt`)
- Undo / Redo (200-step history)
- Zoom In / Out with `Ctrl +=` and `Ctrl +-`
- Customisable font family and size
- Background colour with automatic text contrast adjustment
- Real-time Line / Column / Character count in the status bar
- Word wrap toggle

### Doodle Pad
- Drawing tools: Pen, Eraser, Line, Rectangle, Oval, Fill
- Adjustable brush size via slider
- Flood fill using BFS algorithm
- Export canvas as PNG or JPG

### General
- Tabbed interface (Text Editor | Doodle Pad)
- Safe exit — always prompts to save unsaved changes
- Native look and feel on Windows / macOS / Linux

---

## Project Structure

src/
├── main/ Main.java → Entry point, EDT setup
├── ui/ MainWindow.java → JFrame, tabs, status bar
│ AppMenuBar.java → All menus and shortcuts
├── editor/ TextEditorPanel.java → Text area, zoom, undo, bg colour
│ LineNumberPanel.java → Custom-painted line number gutter
├── fileHandling/ FileHandler.java → File I/O, safe exit logic
└── doodle/ DoodlePanel.java → Canvas, tools, flood fill, export

---

## How to Run

1. Open the project in **IntelliJ IDEA**, **NetBeans**, or **VS Code with Java Extension Pack**
2. Set `src/main/Main.java` as the run configuration entry point
3. Run — no external dependencies required

Or compile and run manually:
```bash
cd src
javac -d ../out main/Main.java ui/*.java editor/*.java fileHandling/*.java doodle/*.java
cd ../out
java main.Main
```

---

## Java Concepts Used

| Concept | Where |
|---|---|
| Inheritance (`extends JFrame`) | MainWindow |
| Composition | TextEditorPanel has-a JTextArea |
| Interfaces (DocumentListener, CaretListener) | TextEditorPanel |
| Adapter pattern (WindowAdapter, MouseAdapter) | MainWindow, DoodlePanel |
| Observer pattern (ActionListener, ChangeListener) | AppMenuBar, TextEditorPanel |
| Enum | DoodlePanel — Tool |
| UndoManager | TextEditorPanel |
| BFS algorithm | DoodlePanel — flood fill |
| Custom painting (paintComponent) | LineNumberPanel, DoodlePanel |
| NIO file I/O (Files.readAllBytes) | FileHandler |
| try-with-resources | FileHandler |
| InputMap / ActionMap key bindings | TextEditorPanel |
| ImageIO (PNG/JPG export) | DoodlePanel |
| SwingUtilities.invokeLater (EDT safety) | Main |

---

## Branch Structure

| Branch | Purpose |
|---|---|
| `main` | Stable, working code only |
| `feature/text-editor` | All text editor development |
| `feature/doodle-pad` | All doodle pad development |
| `feature/file-handler` | File I/O and safe exit logic |
| `bugfix/line-numbers` | Line number alignment fix + scroll sync |
| `bugfix/safe-exit` | Window close and Exit menu fix |

---

## Student Info

- **Name:** Amitabh Sohani / mrittika chakroborty 
- **Institute:** Vidyalankar Institute of Technology, Wadala
- **University:** Mumbai University
- **Semester:** II — Java Programming
- note that this was a collage mini project with one more contributor 
