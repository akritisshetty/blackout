import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

/**
 * MainUI.java
 *
 * Entry point and complete GUI for the Cryptographic Algorithm Implementation
 * demonstration application.
 *
 * Algorithms exposed:
 *   - RSA           (asymmetric encryption / decryption)
 *   - SHA-256       (one-way hashing)
 *   - Playfair      (classical digraphic substitution cipher)
 *
 * All cryptographic logic lives in RSA.java, SHA256.java, Playfair.java.
 * This file only handles layout, user interaction, and error reporting.
 */
public class MainUI extends JFrame {

    // -----------------------------------------------------------------------
    // Cryptographic back-ends
    // -----------------------------------------------------------------------
    private final RSA     rsa;      // RSA key pair generated on startup
    private final SHA256  sha256;
    private final Playfair playfair;

    // -----------------------------------------------------------------------
    // UI Components
    // -----------------------------------------------------------------------
    private JTextArea  inputArea;
    private JTextArea  outputArea;
    private JComboBox<String> algorithmBox;
    private JTextField keyField;
    private JLabel     statusLabel;
    private JLabel     keyLabel;
    private JLabel     inputTitleLabel;
    private JLabel     outputTitleLabel;
    private JButton    encryptBtn;
    private JButton    decryptBtn;
    private JButton    clearBtn;
    private JButton    copyBtn;
    private JButton    swapBtn;
    private JButton    showKeysBtn;
    private JButton    showMatrixBtn;

    // -----------------------------------------------------------------------
    // Theme colours (dark mode)
    // -----------------------------------------------------------------------
    private static final Color BG_DARK      = new Color(18,  18,  27);
    private static final Color BG_PANEL     = new Color(28,  28,  42);
    private static final Color BG_FIELD     = new Color(38,  38,  58);
    private static final Color ACCENT       = new Color(99, 102, 241);   // indigo
    private static final Color ACCENT_HOVER = new Color(129, 140, 248);
    private static final Color SUCCESS      = new Color(52,  211, 153);
    private static final Color WARNING      = new Color(251, 191,  36);
    private static final Color DANGER       = new Color(248,  113, 113);
    private static final Color TEXT_PRIMARY = new Color(226, 232, 240);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Color BORDER_COLOR = new Color(55,  65,  81);

    // -----------------------------------------------------------------------
    // Algorithm names
    // -----------------------------------------------------------------------
    private static final String ALG_RSA      = "RSA";
    private static final String ALG_SHA256   = "SHA-256";
    private static final String ALG_PLAYFAIR = "Playfair Cipher";

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public MainUI() {
        super("Cryptographic Algorithm Implementation");

        // Build the UI first so that statusLabel exists before we call setStatus()
        initComponents();
        applyTheme();
        layoutComponents();
        attachListeners();

        // Frame setup (make visible before key-gen so the user sees the window)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 720);
        setMinimumSize(new Dimension(780, 600));
        setLocationRelativeTo(null);
        setVisible(true);

        // Now update the status bar (statusLabel is guaranteed non-null here)
        setStatus("Generating RSA keys, please wait…", WARNING);

        // Initialise cryptographic engines (RSA key-gen can take a moment)
        rsa      = new RSA();
        sha256   = new SHA256();
        playfair = new Playfair();

        setStatus("RSA keys generated. Ready.", SUCCESS);
    }

    // -----------------------------------------------------------------------
    // Component creation
    // -----------------------------------------------------------------------
    private void initComponents() {
        inputArea  = createTextArea();
        outputArea = createTextArea();
        outputArea.setEditable(true);

        algorithmBox = new JComboBox<>(new String[]{ALG_RSA, ALG_SHA256, ALG_PLAYFAIR});
        algorithmBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        keyField    = new JTextField();
        keyField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        statusLabel = new JLabel("Initialising…");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        keyLabel    = new JLabel("Keyword (Playfair key):");
        keyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        encryptBtn   = createButton("🔒 Encrypt",   ACCENT);
        decryptBtn   = createButton("🔓 Decrypt",   new Color(55, 65, 81));
        clearBtn     = createButton("🗑 Clear All",  new Color(55, 65, 81));
        copyBtn      = createButton("📋 Copy Output", new Color(55, 65, 81));
        swapBtn      = createButton("⇄ Swap", new Color(55, 65, 81));
        showKeysBtn  = createButton("🔑 Show RSA Keys",  new Color(30, 58, 95));
        showMatrixBtn= createButton("⊞ Show Matrix", new Color(30, 58, 95));
    }

    private JTextArea createTextArea() {
        JTextArea ta = new JTextArea();
        ta.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setMargin(new Insets(10, 10, 10, 10));
        return ta;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 18, 10, 18));
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            final Color original = bg;
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(original.brighter());
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(original);
            }
        });
        return btn;
    }

    // -----------------------------------------------------------------------
    // Theme application
    // -----------------------------------------------------------------------
    private void applyTheme() {
        getContentPane().setBackground(BG_DARK);

        for (JTextArea ta : new JTextArea[]{inputArea, outputArea}) {
            ta.setBackground(BG_FIELD);
            ta.setForeground(TEXT_PRIMARY);
            ta.setCaretColor(TEXT_PRIMARY);
        }

        algorithmBox.setBackground(BG_FIELD);
        algorithmBox.setForeground(TEXT_PRIMARY);

        keyField.setBackground(BG_FIELD);
        keyField.setForeground(TEXT_PRIMARY);
        keyField.setCaretColor(TEXT_PRIMARY);
        keyField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        keyLabel.setForeground(TEXT_MUTED);
        statusLabel.setForeground(TEXT_MUTED);
    }

    // -----------------------------------------------------------------------
    // Layout
    // -----------------------------------------------------------------------
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        // --- Title bar ---
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(BG_PANEL);
        titlePanel.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(14, 20, 14, 20)));

        JLabel title = new JLabel("🔐 Cryptographic Algorithm Implementation");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("RSA  ·  SHA-256  ·  Playfair Cipher");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);

        JPanel titleText = new JPanel(new GridLayout(2, 1, 0, 2));
        titleText.setBackground(BG_PANEL);
        titleText.add(title);
        titleText.add(subtitle);
        titlePanel.add(titleText, BorderLayout.WEST);
        add(titlePanel, BorderLayout.NORTH);

        // --- Centre split pane ---
        JPanel centre = new JPanel(new GridLayout(1, 2, 12, 0));
        centre.setBackground(BG_DARK);
        centre.add(makeCard("📝 Input / Plaintext", inputArea));
        centre.add(makeCard("📤 Output / Result",   outputArea));
        add(centre, BorderLayout.CENTER);

        // --- Control panel (south) ---
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setBackground(BG_DARK);

        // Row 1: algorithm picker + key field
        JPanel controlRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controlRow1.setBackground(BG_DARK);

        JLabel algLabel = new JLabel("Algorithm:");
        algLabel.setForeground(TEXT_MUTED);
        algLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        algorithmBox.setPreferredSize(new Dimension(170, 36));
        controlRow1.add(algLabel);
        controlRow1.add(algorithmBox);

        keyField.setPreferredSize(new Dimension(200, 36));
        controlRow1.add(keyLabel);
        controlRow1.add(keyField);

        // Row 2: action buttons
        JPanel controlRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controlRow2.setBackground(BG_DARK);
        controlRow2.add(encryptBtn);
        controlRow2.add(decryptBtn);
        controlRow2.add(clearBtn);
        controlRow2.add(copyBtn);
        controlRow2.add(swapBtn);
        controlRow2.add(showKeysBtn);
        controlRow2.add(showMatrixBtn);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_PANEL);
        statusBar.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(6, 14, 6, 14)));
        statusLabel.setForeground(TEXT_MUTED);
        statusBar.add(statusLabel, BorderLayout.WEST);

        south.add(controlRow1, BorderLayout.NORTH);
        south.add(controlRow2, BorderLayout.CENTER);
        south.add(statusBar,   BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);

        // Initial key field visibility
        updateKeyFieldVisibility();
    }

    /** Wraps a JTextArea in a titled, styled card panel. */
    private JPanel makeCard(String title, JTextArea ta) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_PANEL);
        card.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(10, 10, 10, 10)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(ACCENT_HOVER);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        if (title.contains("Input"))  inputTitleLabel  = lbl;
        if (title.contains("Output")) outputTitleLabel = lbl;

        JScrollPane scroll = new JScrollPane(ta);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(BG_FIELD);

        card.add(lbl,    BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // -----------------------------------------------------------------------
    // Event listeners
    // -----------------------------------------------------------------------
    private void attachListeners() {
        algorithmBox.addActionListener(e -> updateKeyFieldVisibility());

        encryptBtn.addActionListener(e -> handleEncrypt());
        decryptBtn.addActionListener(e -> handleDecrypt());
        clearBtn.addActionListener(e -> handleClear());
        copyBtn.addActionListener(e -> handleCopy());
        swapBtn.addActionListener(e -> handleSwap());
        showKeysBtn.addActionListener(e -> handleShowKeys());
        showMatrixBtn.addActionListener(e -> handleShowMatrix());
    }

    /** Show/hide the key field depending on which algorithm is selected. */
    private void updateKeyFieldVisibility() {
        String alg = (String) algorithmBox.getSelectedItem();
        boolean isPlayfair = ALG_PLAYFAIR.equals(alg);
        boolean isSHA      = ALG_SHA256.equals(alg);

        keyLabel.setVisible(isPlayfair);
        keyField.setVisible(isPlayfair);
        decryptBtn.setEnabled(!isSHA);
        showKeysBtn.setVisible(ALG_RSA.equals(alg));
        showMatrixBtn.setVisible(isPlayfair);

        if (inputTitleLabel != null && outputTitleLabel != null) {
            if (isPlayfair) {
                inputTitleLabel.setText("📝 Input  (plaintext or ciphertext)");
                outputTitleLabel.setText("📤 Output  (ciphertext or plaintext)");
            } else if (isSHA) {
                inputTitleLabel.setText("📝 Input / Plaintext");
                outputTitleLabel.setText("📤 Output / Hash");
            } else {
                inputTitleLabel.setText("📝 Input / Plaintext");
                outputTitleLabel.setText("📤 Output / Result");
            }
        }

        if (isSHA) {
            setStatus("SHA-256 is a one-way hash function — decryption is not possible.", WARNING);
        } else if (isPlayfair) {
            setStatus("Playfair: type plaintext + Encrypt, or paste ciphertext + Decrypt (both use Input).", SUCCESS);
        } else {
            setStatus("Ready.", SUCCESS);
        }
    }

    // -----------------------------------------------------------------------
    // Action handlers
    // -----------------------------------------------------------------------
    private void handleEncrypt() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            showError("Input cannot be empty. Please enter some text to encrypt.");
            return;
        }

        String alg = (String) algorithmBox.getSelectedItem();
        try {
            String result;
            switch (alg) {
                case ALG_RSA:
                    result = rsa.encrypt(input);
                    setStatus("RSA encryption successful.", SUCCESS);
                    break;
                case ALG_SHA256:
                    result = sha256.hashWithDetails(input);
                    setStatus("SHA-256 hash computed successfully.", SUCCESS);
                    break;
                case ALG_PLAYFAIR:
                    String key = keyField.getText().trim();
                    if (key.isEmpty()) {
                        showError("Please enter a Playfair keyword before encrypting.");
                        return;
                    }
                    result = playfair.encrypt(input, key);
                    setStatus("Playfair encryption successful.", SUCCESS);
                    break;
                default:
                    return;
            }
            outputArea.setText(result);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Unexpected error: " + ex.getMessage());
        }
    }

    private void handleDecrypt() {
        String alg = (String) algorithmBox.getSelectedItem();
        if (ALG_SHA256.equals(alg)) {
            showInfo("SHA-256 is a one-way hash function and cannot be decrypted.\n\n"
                   + "It is mathematically infeasible to reverse the hash to the original text.\n"
                   + "This is by design — hashes are used for integrity verification, not secrecy.");
            return;
        }

        boolean isPlayfair = ALG_PLAYFAIR.equals(alg);
        String input;

        if (isPlayfair) {
            // Bidirectional: accept ciphertext from Input (primary) or Output (fallback)
            input = inputArea.getText().trim();
            if (input.isEmpty()) {
                input = outputArea.getText().trim();
                if (input.isEmpty()) {
                    showError("Please paste Playfair ciphertext into Input (or Output) before decrypting.");
                    return;
                }
            }
        } else {
            input = outputArea.getText().trim();
            if (input.isEmpty()) {
                input = inputArea.getText().trim();
                if (input.isEmpty()) {
                    showError("Please paste the ciphertext into the Output area (or Input area) before decrypting.");
                    return;
                }
            }
        }

        try {
            String result;
            switch (alg) {
                case ALG_RSA:
                    result = rsa.decrypt(input);
                    setStatus("RSA decryption successful.", SUCCESS);
                    break;
                case ALG_PLAYFAIR:
                    String key = keyField.getText().trim();
                    if (key.isEmpty()) {
                        showError("Please enter a Playfair keyword before decrypting.");
                        return;
                    }
                    result = playfair.decrypt(input, key);
                    setStatus("Playfair decryption successful.", SUCCESS);
                    break;
                default:
                    return;
            }
            // Playfair: keep ciphertext in Input, show plaintext in Output (bidirectional UX)
            if (isPlayfair) {
                outputArea.setText(result);
            } else {
                inputArea.setText(result);
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Decryption failed: " + ex.getMessage());
        }
    }

    private void handleClear() {
        inputArea.setText("");
        outputArea.setText("");
        keyField.setText("");
        setStatus("Cleared.", TEXT_MUTED);
    }

    private void handleCopy() {
        String text = outputArea.getText();
        if (text.isEmpty()) {
            showError("Output area is empty — nothing to copy.");
            return;
        }
        Toolkit.getDefaultToolkit()
               .getSystemClipboard()
               .setContents(new StringSelection(text), null);
        setStatus("Output copied to clipboard.", SUCCESS);
    }

    private void handleSwap() {
        String tmp = inputArea.getText();
        inputArea.setText(outputArea.getText());
        outputArea.setText(tmp);
        setStatus("Swapped input and output.", SUCCESS);
    }

    private void handleShowKeys() {
        JTextArea area = new JTextArea(rsa.getPublicKey() + "\n\n" + rsa.getPrivateKey());
        area.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        area.setEditable(false);
        area.setBackground(BG_FIELD);
        area.setForeground(TEXT_PRIMARY);
        area.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(640, 300));

        JOptionPane.showMessageDialog(this, scroll,
            "RSA Key Pair", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleShowMatrix() {
        String key = keyField.getText().trim();
        String matrix = playfair.getMatrixDisplay(key.isEmpty() ? "KEY" : key);
        JTextArea area = new JTextArea(matrix);
        area.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        area.setEditable(false);
        area.setBackground(BG_FIELD);
        area.setForeground(TEXT_PRIMARY);
        area.setMargin(new Insets(10, 10, 10, 10));
        JOptionPane.showMessageDialog(this, area,
            "Playfair Key Matrix" + (key.isEmpty() ? " (using default key 'KEY')" : ""),
            JOptionPane.INFORMATION_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // Utility helpers
    // -----------------------------------------------------------------------
    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    private void showError(String message) {
        setStatus("Error: " + message, DANGER);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        // Use system look-and-feel as a base, then override colours
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Dark-theme overrides for common Swing components
        UIManager.put("OptionPane.background",     new Color(28, 28, 42));
        UIManager.put("Panel.background",          new Color(28, 28, 42));
        UIManager.put("OptionPane.messageForeground", new Color(226, 232, 240));
        UIManager.put("Button.background",         new Color(55, 65, 81));
        UIManager.put("Button.foreground",         new Color(226, 232, 240));
        UIManager.put("ComboBox.background",       new Color(38, 38, 58));
        UIManager.put("ComboBox.foreground",       new Color(226, 232, 240));
        UIManager.put("ComboBox.selectionBackground", new Color(99, 102, 241));
        UIManager.put("ScrollPane.background",     new Color(38, 38, 58));

        SwingUtilities.invokeLater(MainUI::new);
    }
}
