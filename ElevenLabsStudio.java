import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ElevenLabsStudio extends JFrame {

    private final List<JComboBox<String>> voiceCombos = new ArrayList<>();
    // second-voice combos carry a leading "(no 2nd voice)" item; tracked separately so
    // rebuildVoiceCombos() can restore that entry after a library change.
    private final java.util.Set<JComboBox<String>> voice2Combos =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private final JTextField apiKeyField  = new JTextField("sk_085b309952bcc3227379faa49e8f49d40478fda3985840e7");
    private final JTextField workDirField = new JTextField(System.getProperty("user.dir"));
    private final JPanel     linesContainer = new JPanel();
    private final List<LineRow> lineRows    = new ArrayList<>();
    private final JTextArea  logArea       = new JTextArea();

    private final JComboBox<String> ttsVoice = voiceCombo("TX3LPaxmHKxFdv7VOQHJ");
    private final JComboBox<String> ttsModel = modelCombo("eleven_v3");
    private final JTextField ttsPrefix = new JTextField("m");
    private final JTextField ttsSpeed  = new JTextField("1.0");
    private final JTextField ttsStab   = new JTextField("0.5");
    private final JTextField ttsSim    = new JTextField("0.5");
    private final JCheckBox  ttsBoost  = new JCheckBox("speaker_boost", true);
    private final JTextField ttsVoice2Gap = new JTextField("0.5");   // gap (s) between voice 1 and voice 2 on a two-voice line

    private final JTextField mergeGap  = new JTextField("0.0");

    private final JComboBox<String> tsModel = modelCombo("eleven_v3");
    private final JTextField tsPrefix= new JTextField("b");
    private final JTextField tsStab  = new JTextField("0.9");
    private final JTextField tsSim   = new JTextField("0.5");
    private final JTextField tsStyle = new JTextField("0.0");
    private final JCheckBox  tsBoost = new JCheckBox("speaker_boost", true);

    private final JComboBox<String> sttModel  = sttModelCombo("scribe_v2");
    private final JCheckBox vidExtract = new JCheckBox("FFmpeg audio extract", true);
    private final JCheckBox vidDiarize = new JCheckBox("Speaker diarization", false);
    private final JCheckBox vidEvents  = new JCheckBox("Tag audio events", false);

    private volatile List<WordStamp> lastVideoWords = null;
    private volatile String lastVideoBaseName = null;

    private final JTextArea  phraseArea   = new JTextArea(4, 6); // col1 -> text33..text40 (matched)
    private final JTextArea  arabicArea   = new JTextArea(4, 6); // col2 -> text41..text48 (typed)
    private final JTextArea  newGroupArea = new JTextArea(4, 6); // col3 -> text49..text56 (typed)
    private volatile List<PhraseHit> pendingPhrases = new ArrayList<>();
    private final java.util.List<JButton> actionButtons = new ArrayList<>();

    private final AudioPlayer player = new AudioPlayer();

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public ElevenLabsStudio() {
        super("ElevenLabs Audio Studio  —  TTS / Scribe / Timestamps");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        buildUI();
        setupMediaDropTarget();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { player.stop(); }
        });
    }

    /** Let the user drag & drop a video/audio file anywhere on the window to transcribe it. */
    private void setupMediaDropTarget() {
        new java.awt.dnd.DropTarget(this, java.awt.dnd.DnDConstants.ACTION_COPY,
                new java.awt.dnd.DropTargetAdapter() {
                    @Override public void drop(java.awt.dnd.DropTargetDropEvent ev) {
                        try {
                            ev.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                            java.awt.datatransfer.Transferable t = ev.getTransferable();
                            if (t.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                                @SuppressWarnings("unchecked")
                                java.util.List<File> files = (java.util.List<File>)
                                        t.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                                handleDroppedFiles(files);
                                ev.dropComplete(true);
                            } else {
                                ev.dropComplete(false);
                            }
                        } catch (Exception ex) {
                            log("Drag & drop failed: " + ex.getMessage());
                            ev.dropComplete(false);
                        }
                    }
                }, true);
    }

    private void handleDroppedFiles(java.util.List<File> files) {
        if (files == null || files.isEmpty()) return;
        File media = null;
        for (File f : files) {
            if (f != null && f.isFile() && isMediaFile(f)) { media = f; break; }
        }
        if (media == null) {
            log("Drag & drop: no supported video/audio file found "
                    + "(accepted: mp4, mov, mkv, webm, avi, m4v, mp3, wav, m4a, aac, flac, ogg).");
            return;
        }
        if (files.size() > 1)
            log("Drag & drop: multiple files dropped — using \"" + media.getName() + "\".");
        else
            log("Drag & drop: \"" + media.getName() + "\" — transcribing…");
        final File chosen = media;
        runInBackground(() -> doVideoWordTimestamps(chosen));
    }

    private static boolean isMediaFile(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return isVideoFile(f)
                || n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a")
                || n.endsWith(".aac") || n.endsWith(".flac") || n.endsWith(".ogg");
    }

    private void buildUI() {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);

        JPanel cfg = new JPanel(new GridBagLayout());
        cfg.setBorder(new TitledBorder("Configuration"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        cfg.add(new JLabel("API Key:"), g);
        g.gridx = 1; g.weightx = 1;
        cfg.add(apiKeyField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        cfg.add(new JLabel("Work folder:"), g);
        g.gridx = 1; g.weightx = 1;
        cfg.add(workDirField, g);
        g.gridx = 2; g.weightx = 0;
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> chooseDir());
        cfg.add(browse, g);

        linesContainer.setLayout(new BoxLayout(linesContainer, BoxLayout.Y_AXIS));
        addLineRow("", comboVal(ttsVoice));

        JPanel scriptPanel = new JPanel(new BorderLayout());
        scriptPanel.setBorder(new TitledBorder(
                "Quotes  (one per row · voice picker per row · Enter adds a new row)"));

        JPanel linesWrap = new JPanel(new BorderLayout());
        linesWrap.add(linesContainer, BorderLayout.NORTH);
        scriptPanel.add(new JScrollPane(linesWrap), BorderLayout.CENTER);

        JPanel scriptBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addLine = new JButton("Add line");
        addLine.addActionListener(e -> {
            addLineRow("", comboVal(ttsVoice));
            refreshLines();
        });
        JButton loadScript = new JButton("Load file…");
        loadScript.addActionListener(e -> loadScriptFile());
        JButton saveScript = new JButton("Save as myscript.txt");
        saveScript.addActionListener(e -> saveScriptFile());
        JButton clearLog = new JButton("Clear log");
        clearLog.addActionListener(e -> logArea.setText(""));
        scriptBtns.add(addLine);
        scriptBtns.add(loadScript);
        scriptBtns.add(saveScript);
        scriptBtns.add(clearLog);
        scriptPanel.add(scriptBtns, BorderLayout.SOUTH);

        logArea.setFont(mono);
        logArea.setEditable(false);
        logArea.setBackground(new Color(0x12, 0x16, 0x1c));
        logArea.setForeground(new Color(0xd6, 0xe2, 0xea));
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Log"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scriptPanel, logPanel);
        center.setResizeWeight(0.42);

        JPanel west = new JPanel();
        west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));

        JPanel voiceBox = settingsBox("0 · Voice Library");
        JButton btnAddVoice = new JButton("＋ Add voice…");
        btnAddVoice.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAddVoice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnAddVoice.setToolTipText("Add a new voice (name + ElevenLabs voice ID). "
                + "It is saved to " + VOICES_FILE.getAbsolutePath() + " and appears in every voice dropdown.");
        btnAddVoice.addActionListener(e -> addVoiceDialog());
        voiceBox.add(btnAddVoice);
        JButton btnDelVoice = new JButton("− Remove voice…");
        btnDelVoice.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDelVoice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnDelVoice.setToolTipText("Remove a voice from the library (the file is rewritten).");
        btnDelVoice.addActionListener(e -> removeVoiceDialog());
        voiceBox.add(Box.createVerticalStrut(4));
        voiceBox.add(btnDelVoice);
        west.add(voiceBox);

        JPanel ttsBox = settingsBox("1 · Generate Audio (TTS)");
        ttsVoice.setToolTipText("Default voice for new lines. Each line in the script has its own voice picker that overrides this.");
        addRow(ttsBox, "Default voice", ttsVoice);
        addRow(ttsBox, "Model ID", ttsModel);
        ttsSpeed.setToolTipText("0.7 – 1.2 (1.0 = normal). Ignored by eleven_v3, which has no speed setting.");
        ttsStab .setToolTipText("0.0 – 1.0. eleven_v3 accepts only 0.0 (creative), 0.5 (natural), 1.0 (robust).");
        ttsSim  .setToolTipText("0.0 – 1.0.");
        tsStyle .setToolTipText("0.0 – 1.0.");
        addRow(ttsBox, "File prefix", ttsPrefix);
        addRow(ttsBox, "speed", ttsSpeed);
        addRow(ttsBox, "stability", ttsStab);
        addRow(ttsBox, "similarity_boost", ttsSim);
        ttsVoice2Gap.setToolTipText("Silence (seconds) inserted between voice 1 and voice 2 when a "
                + "2nd voice is chosen on a line. Needs FFmpeg for an exact gap; the raw fallback ignores it.");
        addRow(ttsBox, "voice-2 gap", ttsVoice2Gap);
        ttsBox.add(ttsBoost);
        JButton btnTts = action("Generate Audio (TTS)", this::doGenerateTts);
        ttsBox.add(Box.createVerticalStrut(4));
        ttsBox.add(btnTts);

        JButton btnStop = new JButton("■ Stop playback");
        btnStop.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnStop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnStop.setToolTipText("Stop the audio currently playing inside the app.");
        btnStop.addActionListener(e -> player.stop());
        ttsBox.add(Box.createVerticalStrut(4));
        ttsBox.add(btnStop);
        west.add(ttsBox);

        JPanel mergeBox = settingsBox("1b · Merge Audio → one MP3");
        mergeGap.setToolTipText("Seconds of silence inserted between clips (needs FFmpeg; ignored by the raw fallback).");
        addRow(mergeBox, "gap (s)", mergeGap);
        JButton btnMerge = action("Merge all lines → MP3", this::mergeGeneratedAudio);
        btnMerge.setToolTipText("Concatenates <prefix>1.mp3 … <prefix>N.mp3 (one per non-empty line, in order) "
                + "into <prefix>_combined.mp3. Uses FFmpeg when available, otherwise a raw MP3 frame append.");
        mergeBox.add(Box.createVerticalStrut(4));
        mergeBox.add(btnMerge);
        JButton btnPlayMerged = new JButton("▶ Play merged");
        btnPlayMerged.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPlayMerged.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnPlayMerged.addActionListener(e -> {
            File f = mergedFile();
            if (!f.exists()) log("Play merged: " + f.getName() + " does not exist yet — click \"Merge all lines\" first.");
            else player.playAsync(f);
        });
        mergeBox.add(Box.createVerticalStrut(4));
        mergeBox.add(btnPlayMerged);
        west.add(mergeBox);

        JPanel tsBox = settingsBox("5 · TTS + Emphasized Timestamps");
        addRow(tsBox, "Model ID", tsModel);
        addRow(tsBox, "File prefix", tsPrefix);
        addRow(tsBox, "stability", tsStab);
        addRow(tsBox, "similarity_boost", tsSim);
        addRow(tsBox, "style", tsStyle);
        tsBox.add(tsBoost);
        JButton btnTs = action("TTS + Timestamps", this::doTimestamps);
        tsBox.add(Box.createVerticalStrut(4));
        tsBox.add(btnTs);
        west.add(tsBox);

        JPanel ops = settingsBox("2 · 3 · 4 · Transcribe / Compare / Workflow");
        ops.add(action("Transcribe (Scribe)", this::doTranscribe));
        ops.add(Box.createVerticalStrut(4));
        ops.add(action("Compare Accuracy", this::doCompare));
        ops.add(Box.createVerticalStrut(4));
        ops.add(action("Full Workflow (1→2→3)", this::doFullWorkflow));
        west.add(ops);

        JPanel vidBox = settingsBox("6 · Video → Word Timestamps");
        sttModel.setToolTipText("Scribe STT model. Also used by option 2 (Transcribe). scribe_v1 is deprecated — prefer scribe_v2.");
        addRow(vidBox, "STT model", sttModel);
        vidExtract.setToolTipText("Extract the audio track to MP3 with FFmpeg before uploading (much smaller upload, same result). Falls back to uploading the video directly if FFmpeg is not installed.");
        vidDiarize.setToolTipText("Label each word with a speaker_id (who said what).");
        vidEvents.setToolTipText("Tag non-speech events such as (laughter), (music)…");
        vidBox.add(vidExtract);
        vidBox.add(vidDiarize);
        vidBox.add(vidEvents);
        JButton btnVid = new JButton("Choose video / audio…");
        btnVid.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnVid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnVid.setToolTipText("Upload a video or audio file and get word-by-word start/end timestamps (JSON + CSV + SRT).");
        btnVid.addActionListener(e -> chooseMediaAndTranscribe());
        actionButtons.add(btnVid);
        vidBox.add(Box.createVerticalStrut(4));
        vidBox.add(btnVid);
        JLabel dropHint = new JLabel("…or drag & drop a video/audio file onto the window");
        dropHint.setFont(dropHint.getFont().deriveFont(Font.ITALIC, 11f));
        dropHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        vidBox.add(Box.createVerticalStrut(2));
        vidBox.add(dropHint);

        JButton btnExport = new JButton("Export to Excel (.xlsx)");
        btnExport.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnExport.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnExport.setToolTipText("<html>Write the last video/audio word timestamps to an .xlsx "
                + "using the fixed, colour-coded template:<br>"
                + "• cols 1-32 paragraph words (text1-32), cols 33-64 their timing (start,end) — blue<br>"
                + "• cols 65-72 selected words (text33-40), cols 73-80 their start time — green<br>"
                + "• cols 81-88 Arabic meaning (text41-48, blank), cols 89-96 same start time — yellow<br>"
                + "• cols 97-104 new group (text49-56, blank), cols 105-112 same start time — purple<br>"
                + "• col 113 logo cell (text57) — peach<br>"
                + "Selected words come from \"Find &amp; Set Phrases\" (up to 8).</html>");
        btnExport.addActionListener(e -> runInBackground(this::exportWordsToExcel));
        actionButtons.add(btnExport);
        vidBox.add(Box.createVerticalStrut(4));
        vidBox.add(btnExport);



        west.add(vidBox);

        JPanel phraseBox = settingsBox("Selected Words (3 cols → text33 · text41 · text49)");
        phraseArea.setToolTipText("<html><b>Column 1 — selected words.</b> One per line, EXACTLY as in the "
                + "transcript (case + punctuation).<br>\"Find &amp; Set Phrases\" matches THESE only and writes "
                + "them to text33-40 with their start times.<br>Up to 8; extras ignored. "
                + "Leave empty and click the button to clear.</html>");
        arabicArea.setToolTipText("<html><b>Column 2 — free text → text41-48.</b> One per line, aligned by row "
                + "with column 1 (line 1 = meaning of selected word 1…).<br>Written verbatim on export; not matched. "
                + "Up to 8.</html>");
        newGroupArea.setToolTipText("<html><b>Column 3 — free text → text49-56.</b> One per line, aligned by row "
                + "with column 1.<br>Written verbatim on export; not matched. Up to 8.</html>");

        JPanel phraseCols = new JPanel(new GridLayout(1, 3, 4, 0));
        phraseCols.setAlignmentX(Component.LEFT_ALIGNMENT);
        phraseCols.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        phraseCols.setPreferredSize(new Dimension(240, 110));
        phraseCols.add(labeledArea("1 → text33", phraseArea, mono));
        phraseCols.add(labeledArea("2 → text41", arabicArea, mono));
        phraseCols.add(labeledArea("3 → text49", newGroupArea, mono));
        phraseBox.add(phraseCols);
        JButton btnPhrase = new JButton("Find & Set Phrases");
        btnPhrase.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPhrase.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnPhrase.setToolTipText("Looks up every selected word/phrase as consecutive words in the last "
                + "transcription and stores its start time for the next export.");
        btnPhrase.addActionListener(e -> runInBackground(this::setPhraseOverrides));
        actionButtons.add(btnPhrase);
        phraseBox.add(Box.createVerticalStrut(4));
        phraseBox.add(btnPhrase);
        west.add(phraseBox);

        west.add(Box.createVerticalGlue());
        JScrollPane westScroll = new JScrollPane(west,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        westScroll.setBorder(null);
        westScroll.setPreferredSize(new Dimension(230, 100));
        westScroll.getVerticalScrollBar().setUnitIncrement(16);

        add(cfg, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(westScroll, BorderLayout.WEST);
    }

    private JPanel settingsBox(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new TitledBorder(title));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** A text area with a small caption above it (for the 3-column selected-words layout). */
    private JPanel labeledArea(String caption, JTextArea area, Font mono) {
        area.setFont(mono);
        area.setLineWrap(false);
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel(caption, SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 10f));
        p.add(l, BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private static final int FIELD_W = 100;

    private void addRow(JPanel box, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(80, 24));
        l.setMaximumSize(new Dimension(80, 24));

        field.setPreferredSize(new Dimension(FIELD_W, 24));
        field.setMaximumSize(new Dimension(FIELD_W, 24));

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(l);
        row.add(Box.createHorizontalStrut(1));
        row.add(field);
        row.add(Box.createHorizontalGlue());
        box.add(row);
    }

    private JButton action(String text, Runnable task) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        b.addActionListener(e -> runInBackground(task));
        actionButtons.add(b);
        return b;
    }

    private void runInBackground(Runnable task) {
        setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                try { task.run(); }
                catch (Exception ex) { log("FATAL: " + ex); }
                return null;
            }
            @Override protected void done() { setBusy(false); }
        }.execute();
    }

    private void setBusy(boolean busy) {
        SwingUtilities.invokeLater(() -> {
            for (JButton b : actionButtons) b.setEnabled(!busy);
            setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                    : Cursor.getDefaultCursor());
        });
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private String apiKey()  { return apiKeyField.getText().trim(); }
    private File   workDir() { return new File(workDirField.getText().trim()); }

    private void chooseDir() {
        JFileChooser fc = new JFileChooser(workDir());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            workDirField.setText(fc.getSelectedFile().getAbsolutePath());
    }

    private void loadScriptFile() {
        JFileChooser fc = new JFileChooser(workDir());
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(Files.readAllBytes(fc.getSelectedFile().toPath()), StandardCharsets.UTF_8);
                clearLineRows();
                String defaultVoice = comboVal(ttsVoice);
                for (String line : content.split("\n", -1)) {
                    String t = line.trim();
                    if (!t.isEmpty()) addLineRow(t, defaultVoice);
                }
                if (lineRows.isEmpty()) addLineRow("", defaultVoice);
                refreshLines();
                log("Loaded script: " + fc.getSelectedFile().getName());
            } catch (Exception e) { log("Error reading script: " + e.getMessage()); }
        }
    }

    private void saveScriptFile() {
        try {
            StringBuilder sb = new StringBuilder();
            for (LineRow r : lineRows) sb.append(r.text.getText()).append("\n");
            File out = new File(workDir(), "myscript.txt");
            Files.write(out.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            log("Saved: " + out.getAbsolutePath());
        } catch (Exception e) { log("Error saving script: " + e.getMessage()); }
    }

    private List<String> readQuotes() {
        List<String> quotes = new ArrayList<>();
        for (LineRow r : lineRows) {
            String t = r.text.getText().trim();
            if (!t.isEmpty()) quotes.add(t);
        }
        return quotes;
    }

    private List<QuoteItem> readQuoteItems() {
        List<QuoteItem> items = new ArrayList<>();
        for (LineRow r : lineRows) {
            String t = r.text.getText().trim();
            if (!t.isEmpty()) items.add(new QuoteItem(comboVal(r.voice), comboVal(r.voice2), t));
        }
        return items;
    }

    private void addLineRow(String text, String voiceId) {
        insertLineRow(lineRows.size(), text, voiceId);
    }

    private LineRow insertLineRow(int idx, String text, String voiceId) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String seed = (voiceId == null || voiceId.isEmpty()) ? comboVal(ttsVoice) : voiceId;
        JComboBox<String> voiceCb = voiceCombo(seed);
        voiceCb.setPreferredSize(new Dimension(180, 24));
        voiceCb.setMaximumSize(new Dimension(180, 24));

        JComboBox<String> voiceCb2 = voiceCombo2(NO_VOICE2);
        voiceCb2.setPreferredSize(new Dimension(180, 24));
        voiceCb2.setMaximumSize(new Dimension(180, 24));
        voiceCb2.setToolTipText("Optional 2nd voice. When set, this line is generated by BOTH voices "
                + "and concatenated into one clip (gap set by \"voice-2 gap\" in the TTS panel). "
                + "Leave on \"(no 2nd voice)\" for the normal single-voice behavior.");

        JTextField textField = new JTextField(text);
        textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JButton listen = new JButton("▶ Listen");
        listen.setMargin(new Insets(2, 6, 2, 6));
        listen.setToolTipText("Play the generated audio for this line inside the app (generates it first if missing)");

        JButton regen = new JButton("↻ Regen");
        regen.setMargin(new Insets(2, 6, 2, 6));
        regen.setToolTipText("Regenerate audio for this line using its current voice");

        JButton applyAll = new JButton("→ all");
        applyAll.setMargin(new Insets(2, 6, 2, 6));
        applyAll.setToolTipText("Apply this row's voice(s) to all lines");

        JButton remove = new JButton("×");
        remove.setMargin(new Insets(2, 6, 2, 6));
        remove.setToolTipText("Remove this line");

        panel.add(voiceCb);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(voiceCb2);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(textField);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(listen);
        panel.add(Box.createHorizontalStrut(4));
        panel.add(regen);
        panel.add(Box.createHorizontalStrut(4));
        panel.add(applyAll);
        panel.add(Box.createHorizontalStrut(4));
        panel.add(remove);

        LineRow lr = new LineRow(panel, voiceCb, voiceCb2, textField);

        listen.addActionListener(e -> listenLine(lr));
        regen.addActionListener(e -> runInBackground(() -> regenerateLine(lr)));
        applyAll.addActionListener(e -> {
            String v1 = comboVal(lr.voice);
            String v2 = comboVal(lr.voice2);
            for (LineRow r : lineRows) {
                r.voice.setSelectedItem(v1);
                r.voice2.setSelectedItem(v2);
                if (r.voice2.isEditable()) r.voice2.getEditor().setItem(v2);
            }
        });
        remove.addActionListener(e -> {
            lineRows.remove(lr);
            linesContainer.remove(lr.panel);
            voiceCombos.remove(lr.voice);
            voiceCombos.remove(lr.voice2);
            voice2Combos.remove(lr.voice2);
            refreshLines();
        });
        textField.addActionListener(e -> {
            int i = lineRows.indexOf(lr);
            if (i < 0) return;
            LineRow added = insertLineRow(i + 1, "", comboVal(lr.voice));
            refreshLines();
            added.text.requestFocusInWindow();
        });

        lineRows.add(idx, lr);
        linesContainer.add(panel, idx);
        return lr;
    }

    private void refreshLines() {
        linesContainer.revalidate();
        linesContainer.repaint();
    }

    private void clearLineRows() {
        for (LineRow r : lineRows) {
            voiceCombos.remove(r.voice);
            voiceCombos.remove(r.voice2);
            voice2Combos.remove(r.voice2);
        }
        lineRows.clear();
        linesContainer.removeAll();
    }

    private static final List<String[]> VOICES = new ArrayList<>(Arrays.asList(
            new String[]{"Liam",                 "TX3LPaxmHKxFdv7VOQHJ"},
            new String[]{"Madison Ray (anchor)", "FyrYFW3P9GUxA348YGWu"},
            new String[]{"Brian",                "XrExE9yKIg1WjnnlVkGX"},
            new String[]{"Good woman",           "pFZP5JQG7iQjIQuC4Bku"},
            new String[]{"English voice",        "nPczCjzI2devNBz1zQrb"},
            new String[]{"Bint 2",               "lzvBSKYbNWDD0a6BaJSK"},
            new String[]{"Voice TU0s",           "TU0sO9BxJtJ4GRbC43XW"},
            new String[]{"Voice cwo4",           "cwo4ramDmreHdb4b1Jxz"},
            new String[]{"steady broadcaser",    "onwK4e9ZLuTAKqWW03F9"},
            new String[]{"thomas",               "IHw7aBJxrIo1SxkG9px5"},
            new String[]{"teacher",              "nCe84PxV5ZGfX9uplQkn"},
            new String[]{"johny",                "JZ3e95uoTACVf6tXaaEi"}
    ));

    private static final File VOICES_FILE =
            new File(System.getProperty("user.home"), ".elevenlabs_studio_voices.txt");

    static { loadVoicesFile(); }

    private static void loadVoicesFile() {
        if (!VOICES_FILE.isFile()) return;
        try {
            for (String line : Files.readAllLines(VOICES_FILE.toPath(), StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                int sep = t.indexOf('\t');
                if (sep < 0) sep = t.indexOf('|');
                if (sep <= 0 || sep == t.length() - 1) continue;
                upsertVoice(t.substring(0, sep).trim(), t.substring(sep + 1).trim());
            }
        } catch (Exception ignored) { }
    }

    private static void saveVoicesFile() throws IOException {
        StringBuilder sb = new StringBuilder("# ElevenLabs Studio voice library — name<TAB>id\n");
        for (String[] v : VOICES) sb.append(v[0]).append('\t').append(v[1]).append('\n');
        Files.write(VOICES_FILE.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static boolean upsertVoice(String name, String id) {
        if (name.isEmpty() || id.isEmpty()) return false;
        for (String[] v : VOICES) {
            if (v[1].equalsIgnoreCase(id)) { v[0] = name; return false; }
        }
        VOICES.add(new String[]{name, id});
        return true;
    }

    private static String voiceName(String id) {
        for (String[] v : VOICES) if (v[1].equals(id)) return v[0];
        return null;
    }

    private void addVoiceDialog() {
        JTextField name = new JTextField();
        JTextField id   = new JTextField();
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel("Voice name:"));  p.add(name);
        p.add(new JLabel("Voice ID:"));    p.add(id);
        p.setPreferredSize(new Dimension(360, 120));

        int r = JOptionPane.showConfirmDialog(this, p, "Add voice",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String n = name.getText().trim();
        String i = id.getText().trim();
        if (n.isEmpty() || i.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both the name and the voice ID are required.",
                    "Add voice", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean isNew = upsertVoice(n, i);
        try {
            saveVoicesFile();
            log((isNew ? "Voice added: " : "Voice updated: ") + n + "  —  " + i
                    + "   (library: " + VOICES_FILE.getAbsolutePath() + ")");
        } catch (Exception e) {
            log("Voice added in memory, but saving the library failed: " + e.getMessage());
        }
        rebuildVoiceCombos();
        ttsVoice.setSelectedItem(i);
    }

    private void removeVoiceDialog() {
        if (VOICES.isEmpty()) { log("Voice library is empty."); return; }
        String[] labels = new String[VOICES.size()];
        for (int i = 0; i < VOICES.size(); i++)
            labels[i] = VOICES.get(i)[0] + "  —  " + VOICES.get(i)[1];

        Object sel = JOptionPane.showInputDialog(this, "Remove which voice?", "Remove voice",
                JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
        if (sel == null) return;

        int idx = Arrays.asList(labels).indexOf(String.valueOf(sel));
        if (idx < 0) return;
        String[] gone = VOICES.remove(idx);
        try { saveVoicesFile(); } catch (Exception e) { log("Library save failed: " + e.getMessage()); }
        log("Voice removed: " + gone[0] + "  —  " + gone[1]
                + " (rows still using that ID keep it as free text and continue to work).");
        rebuildVoiceCombos();
    }

    private void rebuildVoiceCombos() {
        SwingUtilities.invokeLater(() -> {
            for (JComboBox<String> cb : voiceCombos) {
                boolean hasNone = voice2Combos.contains(cb);
                String current = comboVal(cb);
                cb.removeAllItems();
                if (hasNone) cb.addItem(NO_VOICE2);
                for (String[] v : VOICES) cb.addItem(v[1]);
                cb.setSelectedItem(current);
                if (cb.isEditable()) cb.getEditor().setItem(current);
            }
            refreshLines();
        });
    }

    private final class AudioPlayer {
        private volatile Thread worker;
        private volatile SourceDataLine line;
        private volatile boolean stopRequested;
        private final Map<String, File> decoded = Collections.synchronizedMap(new HashMap<>());

        void playAsync(File f) {
            stop();
            Thread t = new Thread(() -> playBlocking(f), "audio-player");
            t.setDaemon(true);
            worker = t;
            t.start();
        }

        void stop() {
            stopRequested = true;
            SourceDataLine l = line;
            if (l != null) { try { l.stop(); l.flush(); } catch (Exception ignored) {} }
            Thread t = worker;
            if (t != null && t.isAlive()) {
                try { t.join(1500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            worker = null;
        }

        private void playBlocking(File f) {
            stopRequested = false;
            try (AudioInputStream pcm = openPcm(f)) {
                AudioFormat fmt = pcm.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                if (!AudioSystem.isLineSupported(info)) {
                    log("Playback: no audio line supports " + fmt + " — falling back to the system player.");
                    openExternally(f);
                    return;
                }
                SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
                sdl.open(fmt);
                sdl.start();
                line = sdl;

                log("▶ Playing: " + f.getName());
                byte[] buf = new byte[8192];
                int n;
                while (!stopRequested && (n = pcm.read(buf, 0, buf.length)) > 0) {
                    sdl.write(buf, 0, n);
                }
                if (!stopRequested) sdl.drain();
                sdl.stop();
                sdl.close();
                line = null;
                log(stopRequested ? "■ Playback stopped." : "✓ Playback finished: " + f.getName());
            } catch (Exception e) {
                line = null;
                log("Playback error (" + f.getName() + "): " + e.getMessage()
                        + " — falling back to the system player.");
                openExternally(f);
            }
        }

        private AudioInputStream openPcm(File f) throws Exception {
            try {
                AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(
                        Files.newInputStream(f.toPath())));
                return toPcm(in);
            } catch (Exception spiFailed) {
                File wav = decodeToWav(f);
                if (wav == null)
                    throw new IOException("this JRE cannot decode " + f.getName()
                            + " and FFmpeg is not available");
                return toPcm(AudioSystem.getAudioInputStream(wav));
            }
        }

        private AudioInputStream toPcm(AudioInputStream in) {
            AudioFormat base = in.getFormat();
            if (base.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && base.getSampleSizeInBits() == 16)
                return in;
            AudioFormat pcm = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate() > 0 ? base.getSampleRate() : 44100f,
                    16,
                    base.getChannels() > 0 ? base.getChannels() : 2,
                    (base.getChannels() > 0 ? base.getChannels() : 2) * 2,
                    base.getSampleRate() > 0 ? base.getSampleRate() : 44100f,
                    false);
            return AudioSystem.getAudioInputStream(pcm, in);
        }

        private File decodeToWav(File src) {
            String key;
            try { key = src.getCanonicalPath() + "|" + src.lastModified() + "|" + src.length(); }
            catch (IOException e) { key = src.getAbsolutePath() + "|" + src.lastModified(); }

            File cached = decoded.get(key);
            if (cached != null && cached.isFile() && cached.length() > 0) return cached;
            if (!ffmpegAvailable()) return null;
            try {
                Path tmp = Files.createTempFile("els_play_", ".wav");
                tmp.toFile().deleteOnExit();
                log("Decoding " + src.getName() + " with FFmpeg for playback …");
                boolean ok = runFfmpeg(Arrays.asList("-y", "-i", src.getAbsolutePath(),
                        "-vn", "-ac", "2", "-ar", "44100", "-acodec", "pcm_s16le",
                        "-f", "wav", tmp.toAbsolutePath().toString()), 5);
                if (!ok || Files.size(tmp) == 0) return null;
                decoded.put(key, tmp.toFile());
                return tmp.toFile();
            } catch (Exception e) {
                log("FFmpeg decode failed: " + e.getMessage());
                return null;
            }
        }

        private void openExternally(File f) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                    Desktop.getDesktop().open(f);
                else
                    log("File is at " + f.getAbsolutePath());
            } catch (Exception e) {
                log("Could not open " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private volatile Boolean ffmpegPresent = null;

    private boolean ffmpegAvailable() {
        Boolean cached = ffmpegPresent;
        if (cached != null) return cached;
        boolean ok;
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (InputStream in = p.getInputStream()) { in.readAllBytes(); }
            ok = p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            ok = false;
        }
        ffmpegPresent = ok;
        return ok;
    }

    private boolean runFfmpeg(List<String> args, int timeoutMinutes) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.addAll(args);
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (InputStream in = p.getInputStream()) { in.readAllBytes(); }
            boolean done = p.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!done) { p.destroyForcibly(); return false; }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private File mergedFile() {
        String prefix = ttsPrefix.getText().trim();
        if (prefix.isEmpty()) prefix = "audio";
        return new File(workDir(), prefix + "_combined.mp3");
    }

    /** FFmpeg concat (exact gap) when available, otherwise a raw MP3 frame append (gap ignored). */
    private boolean concatClips(List<File> parts, double gap, File out) {
        boolean ok = ffmpegAvailable() && mergeWithFfmpeg(parts, gap, out);
        if (!ok) {
            if (gap > 0 && !ffmpegAvailable())
                log("FFmpeg not available -- the silence gap is ignored by the raw fallback.");
            ok = mergeRaw(parts, out);
        }
        return ok && out.isFile() && out.length() > 0;
    }

    private void mergeGeneratedAudio() {
        log("MERGE AUDIO → SINGLE MP3");
        log("========================");

        int count = readQuoteItems().size();
        if (count == 0) { log("No quotes found — nothing to merge."); return; }

        List<File> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            File f = ttsAudioFile(i);
            if (f.isFile() && f.length() > 0) parts.add(f);
            else log("Missing: " + f.getName() + " — skipped (click \"Generate Audio\" or \"↻ Regen\" for that line).");
        }
        if (parts.isEmpty()) { log("No generated audio files found — run \"Generate Audio (TTS)\" first."); return; }
        if (parts.size() == 1) log("Only one clip found — the output will simply be a copy of it.");

        double gap = 0;
        try { gap = Math.max(0, Double.parseDouble(mergeGap.getText().trim())); } catch (Exception ignored) {}

        File out = mergedFile();
        log("Merging " + parts.size() + " clip(s)" + (gap > 0 ? String.format(Locale.US, " with %.2fs gaps", gap) : "")
                + " → " + out.getName());

        boolean ok = concatClips(parts, gap, out);
        if (ok) log("Saved: " + out.getAbsolutePath() + "  (" + mb(out.length()) + " MB)");
        else     log("Merge failed.");
    }

    private boolean mergeWithFfmpeg(List<File> parts, double gapSeconds, File out) {
        File listFile = new File(workDir(), ".merge_list.txt");
        File silence  = new File(workDir(), ".merge_silence.mp3");
        try {
            if (gapSeconds > 0) {
                boolean sok = runFfmpeg(Arrays.asList("-y",
                        "-f", "lavfi",
                        "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
                        "-t", String.format(Locale.US, "%.3f", gapSeconds),
                        "-c:a", "libmp3lame", "-q:a", "2",
                        silence.getAbsolutePath()), 2);
                if (!sok) { log("FFmpeg: could not build the silence clip — merging without gaps."); gapSeconds = 0; }
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0 && gapSeconds > 0) sb.append(concatLine(silence));
                sb.append(concatLine(parts.get(i)));
            }
            Files.write(listFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));

            log("FFmpeg: concatenating …");
            boolean ok = runFfmpeg(Arrays.asList("-y",
                    "-f", "concat", "-safe", "0",
                    "-i", listFile.getAbsolutePath(),
                    "-c:a", "libmp3lame", "-q:a", "2",
                    out.getAbsolutePath()), 20);
            if (!ok) log("FFmpeg concat failed — falling back to a raw MP3 frame append.");
            return ok && out.isFile() && out.length() > 0;
        } catch (Exception e) {
            log("FFmpeg concat error: " + e.getMessage());
            return false;
        } finally {
            listFile.delete();
            if (silence.exists()) silence.delete();
        }
    }

    private static String concatLine(File f) {
        return "file '" + f.getAbsolutePath().replace("'", "'\\''") + "'\n";
    }

    private boolean mergeRaw(List<File> parts, File out) {
        log("Raw MP3 append (no FFmpeg) …");
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
            for (File f : parts) {
                byte[] data = Files.readAllBytes(f.toPath());
                int from = 0, to = data.length;

                if (data.length > 10 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
                    int size = ((data[6] & 0x7F) << 21) | ((data[7] & 0x7F) << 14)
                            | ((data[8] & 0x7F) << 7)  |  (data[9] & 0x7F);
                    from = Math.min(10 + size, data.length);
                }
                if (to - from > 128 && data[to - 128] == 'T' && data[to - 127] == 'A' && data[to - 126] == 'G')
                    to -= 128;

                if (to > from) os.write(data, from, to - from);
                log("   + " + f.getName() + "  (" + mb(to - from) + " MB of frames)");
            }
            return true;
        } catch (Exception e) {
            log("Raw merge error: " + e.getMessage());
            return false;
        }
    }

    private void doGenerateTts() {
        log("Starting Quote Audio Generation...");
        log("====================================");
        List<QuoteItem> items = readQuoteItems();
        if (items.isEmpty()) { log("No quotes found. Add quotes or load a file."); return; }

        log("Found " + items.size() + " quotes to convert.");
        String ttsSettings = ttsSettingsJson();
        String model = comboVal(ttsModel);

        int ok = 0;
        for (int i = 0; i < items.size(); i++) {
            QuoteItem it = items.get(i);
            File out = ttsAudioFile(i);
            boolean two = it.voice2 != null && !it.voice2.isEmpty()
                    && !it.voice2.equalsIgnoreCase(it.voice);
            log("Generating audio " + (i + 1) + " [voice " + voiceLabel(it.voice)
                    + (two ? " + " + voiceLabel(it.voice2) : "") + "]: \"" + preview(it.text) + "\"");
            if (generateLineTts(it.voice, it.voice2, it.text, model, ttsSettings, out)) ok++;
        }
        log("");
        log("Generated " + ok + " out of " + items.size() + " audio files.");
    }

    private static String voiceLabel(String id) {
        String n = voiceName(id);
        return n == null ? id : n + " / " + id;
    }

    private static boolean isV3(String modelId) {
        return modelId != null && modelId.toLowerCase(Locale.ROOT).startsWith("eleven_v3");
    }

    private static double dblField(JTextField f, double fallback) {
        try { return Double.parseDouble(f.getText().trim()); }
        catch (Exception e) { return fallback; }
    }

    private double clamp(String name, double v, double lo, double hi) {
        if (v < lo) { log("  ! " + name + "=" + trim(v) + " is below the allowed minimum — using " + trim(lo)); return lo; }
        if (v > hi) { log("  ! " + name + "=" + trim(v) + " is above the allowed maximum — using " + trim(hi)); return hi; }
        return v;
    }

    private double snapV3Stability(double v) {
        double[] allowed = {0.0, 0.5, 1.0};
        double best = allowed[0];
        for (double a : allowed)
            if (Math.abs(a - v) < Math.abs(best - v)) best = a;
        if (best != v)
            log("  ! eleven_v3 only accepts stability 0.0 / 0.5 / 1.0 — snapping "
                    + trim(v) + " → " + trim(best));
        return best;
    }

    private static String n3(double v) { return String.format(Locale.US, "%.3f", v); }

    private String voiceSettingsJson(String modelId,
                                     double stability,
                                     double similarity,
                                     Double style,
                                     Double speed,
                                     boolean speakerBoost) {
        StringBuilder sb = new StringBuilder("{");

        double stab = isV3(modelId) ? snapV3Stability(stability)
                : clamp("stability", stability, 0.0, 1.0);
        sb.append("\"stability\":").append(n3(stab));

        sb.append(",\"similarity_boost\":").append(n3(clamp("similarity_boost", similarity, 0.0, 1.0)));

        if (style != null)
            sb.append(",\"style\":").append(n3(clamp("style", style, 0.0, 1.0)));

        if (speed != null) {
            if (isV3(modelId)) {
                log("  ! eleven_v3 does not support the \"speed\" setting — it is omitted "
                        + "(use pacing tags in the text instead).");
            } else {
                sb.append(",\"speed\":").append(n3(clamp("speed", speed, 0.7, 1.2)));
            }
        }

        sb.append(",\"use_speaker_boost\":").append(speakerBoost);
        return sb.append("}").toString();
    }

    private String ttsSettingsJson() {
        return ttsSettingsJson(comboVal(ttsModel));
    }

    private String ttsSettingsJson(String modelId) {
        return voiceSettingsJson(
                modelId,
                dblField(ttsStab,  0.5),
                dblField(ttsSim,   0.75),
                null,
                dblField(ttsSpeed, 1.0),
                ttsBoost.isSelected());
    }

    private File ttsAudioFile(int zeroBasedIdx) {
        String prefix = ttsPrefix.getText().trim();
        if (prefix.isEmpty()) prefix = "audio";
        return new File(workDir(), prefix + (zeroBasedIdx + 1) + ".mp3");
    }

    private boolean generateOneTts(String voice, String text, String model, String settings, File out) {
        if (voice == null || voice.isEmpty()) { log("Error: no voice ID selected for " + out.getName()); return false; }
        String body = "{"
                + "\"text\":" + jsonStr(text) + ","
                + "\"model_id\":" + jsonStr(model) + ","
                + "\"voice_settings\":" + settings
                + "}";
        try {
            byte[] audio = postForBytes(
                    "https://api.elevenlabs.io/v1/text-to-speech/" + voice, body);
            Files.write(out.toPath(), audio);
            log("Saved: " + out.getName());
            return true;
        } catch (Exception e) {
            log("Error generating audio for " + out.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates one line into {@code out}. With a 2nd voice set, both voices speak the
     * same text and the two clips are concatenated (voice1 + gap + voice2) into the SAME
     * output file, so every other feature (Listen, Merge, Transcribe, ...) keeps working
     * unchanged. With no 2nd voice it is the original single-voice path, byte-for-byte.
     */
    private boolean generateLineTts(String voice1, String voice2, String text,
                                    String model, String settings, File out) {
        boolean hasSecond = voice2 != null && !voice2.trim().isEmpty();
        if (hasSecond && voice2.trim().equalsIgnoreCase(voice1 == null ? "" : voice1.trim())) {
            log("  (2nd voice equals the 1st -- generating a single voice.)");
            hasSecond = false;
        }
        if (!hasSecond) return generateOneTts(voice1, text, model, settings, out);

        double gap = dblField(ttsVoice2Gap, 0.5);
        if (gap < 0) { log("  ! voice-2 gap is negative -- using 0."); gap = 0; }

        File p1 = null, p2 = null;
        try {
            p1 = File.createTempFile("els_v1_", ".mp3");
            p2 = File.createTempFile("els_v2_", ".mp3");
            log("  Voice 1 [" + voiceLabel(voice1) + "] ...");
            if (!generateOneTts(voice1, text, model, settings, p1)) return false;
            log("  Voice 2 [" + voiceLabel(voice2.trim()) + "] ...");
            if (!generateOneTts(voice2.trim(), text, model, settings, p2)) return false;

            boolean ok = concatClips(Arrays.asList(p1, p2), gap, out);
            if (ok) log("  \u2713 2 voices concatenated -> " + out.getName()
                    + (gap > 0 ? String.format(Locale.US, "  (%.2fs gap)", gap) : ""));
            else    log("  \u2717 Could not concatenate the two voice clips for " + out.getName());
            return ok;
        } catch (Exception e) {
            log("  Two-voice generation error for " + out.getName() + ": " + e.getMessage());
            return false;
        } finally {
            if (p1 != null) p1.delete();
            if (p2 != null) p2.delete();
        }
    }

    private int nonEmptyIndex(LineRow target) {
        int idx = 0;
        for (LineRow r : lineRows) {
            if (r.text.getText().trim().isEmpty()) continue;
            if (r == target) return idx;
            idx++;
        }
        return -1;
    }

    private void listenLine(LineRow row) {
        final int idx = nonEmptyIndex(row);
        if (idx < 0) { log("Listen: line is empty — type some text first."); return; }

        File f = ttsAudioFile(idx);
        if (f.isFile() && f.length() > 0) { player.playAsync(f); return; }

        log("Listen: no audio yet at " + f.getName() + " — generating it now …");
        runInBackground(() -> {
            regenerateLine(row);
            File g = ttsAudioFile(idx);
            if (g.isFile() && g.length() > 0) player.playAsync(g);
        });
    }

    private void regenerateLine(LineRow row) {
        String text = row.text.getText().trim();
        if (text.isEmpty()) { log("Regen: line is empty — type some text first."); return; }
        int idx = nonEmptyIndex(row);
        if (idx < 0) { log("Regen: could not locate this line."); return; }
        String v1 = comboVal(row.voice);
        String v2 = comboVal(row.voice2);
        boolean two = !v2.isEmpty() && !v2.equalsIgnoreCase(v1);
        File out = ttsAudioFile(idx);
        log("Regenerating audio " + (idx + 1) + " [voice " + voiceLabel(v1)
                + (two ? " + " + voiceLabel(v2) : "") + "]: \"" + preview(text) + "\"");
        generateLineTts(v1, v2, text, comboVal(ttsModel), ttsSettingsJson(), out);
    }

    private void doTimestamps() {
        log("TTS + EMPHASIZED WORD TIMESTAMPS");
        log("================================");
        List<QuoteItem> items = readQuoteItems();
        if (items.isEmpty()) { log("No quotes found."); return; }

        String model = comboVal(tsModel);
        String settings = voiceSettingsJson(
                model,
                dblField(tsStab,  0.9),
                dblField(tsSim,   0.5),
                dblField(tsStyle, 0.0),
                null,
                tsBoost.isSelected());

        List<String> allRows = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            QuoteItem it = items.get(i);
            List<String> starts = generateAudioWithTimestamps(it.text, i, it.voice, model, settings);
            if (starts != null) allRows.add(String.join(",", starts));
        }
        try {
            Files.write(new File(workDir(), "all_starttimes.txt").toPath(),
                    String.join("\n", allRows).getBytes(StandardCharsets.UTF_8));
            log("");
            log("Saved combined file: all_starttimes.txt");
        } catch (Exception e) { log("Error writing all_starttimes.txt: " + e.getMessage()); }
    }

    private List<String> generateAudioWithTimestamps(String quote, int index,
                                                     String voice, String model, String settings) {
        try {
            log("[Timestamps] Generating audio " + (index + 1) + "...");
            String body = "{"
                    + "\"text\":" + jsonStr(quote) + ","
                    + "\"model_id\":" + jsonStr(model) + ","
                    + "\"voice_settings\":" + settings
                    + "}";
            String json = postForString(
                    "https://api.elevenlabs.io/v1/text-to-speech/" + voice + "/with-timestamps", body);

            Map<String, Object> root = asObj(MiniJson.parse(json));

            String b64 = (String) root.get("audio_base64");
            File f = new File(workDir(), tsPrefix.getText().trim() + (index + 1) + ".mp3");
            Files.write(f.toPath(), Base64.getDecoder().decode(b64));
            log("Saved: " + f.getName());

            Map<String, Object> a = asObj(root.get("alignment"));
            List<Object> chars  = asArr(a.get("characters"));
            List<Object> starts = asArr(a.get("character_start_times_seconds"));
            List<Object> ends   = asArr(a.get("character_end_times_seconds"));

            List<Word> words = new ArrayList<>();
            Word current = null;
            boolean inBracket = false;
            for (int i = 0; i < chars.size(); i++) {
                String c = String.valueOf(chars.get(i));
                if (c.equals("["))  { inBracket = true;  continue; }
                if (c.equals("]"))  { inBracket = false; continue; }
                if (inBracket) continue;

                if (c.matches("[A-Za-z']")) {
                    if (current == null)
                        current = new Word("", dbl(starts.get(i)), dbl(ends.get(i)));
                    current.word += c;
                    current.end = dbl(ends.get(i));
                } else if (current != null) {
                    words.add(current);
                    current = null;
                }
            }
            if (current != null) words.add(current);

            List<String> emphasized = new ArrayList<>();
            Matcher m = Pattern.compile("\\[emphasized\\]\\s*([A-Za-z']+)",
                    Pattern.CASE_INSENSITIVE).matcher(quote);
            while (m.find()) emphasized.add(m.group(1).toLowerCase());

            log("");
            log("Emphasized word timestamps:");
            boolean[] used = new boolean[words.size()];
            List<Word> picked = new ArrayList<>();
            for (String target : emphasized) {
                int idx = -1;
                for (int k = 0; k < words.size(); k++) {
                    if (!used[k] && words.get(k).word.equalsIgnoreCase(target)) { idx = k; break; }
                }
                if (idx != -1) {
                    used[idx] = true;
                    Word w = words.get(idx);
                    picked.add(w);
                    log(String.format(Locale.US, "   \"%s\"  ->  %.3fs  (ends %.3fs)",
                            w.word, w.start, w.end));
                } else {
                    log("   \"" + target + "\"  ->  not found in alignment");
                }
            }

            StringBuilder sb = new StringBuilder("[\n");
            for (int k = 0; k < picked.size(); k++) {
                Word w = picked.get(k);
                sb.append("  {\n")
                        .append("    \"word\": ").append(jsonStr(w.word)).append(",\n")
                        .append(String.format(Locale.US, "    \"start\": %s,%n", trim(w.start)))
                        .append(String.format(Locale.US, "    \"end\": %s%n", trim(w.end)))
                        .append("  }").append(k < picked.size() - 1 ? "," : "").append("\n");
            }
            sb.append("]");
            Files.write(new File(workDir(), (index + 1) + "_timings.json").toPath(),
                    sb.toString().getBytes(StandardCharsets.UTF_8));

            List<String> result = new ArrayList<>();
            for (Word w : picked) result.add(String.format(Locale.US, "%.3f", w.start));
            return result;

        } catch (Exception e) {
            log("Error (timestamps) for quote " + (index + 1) + ": " + e.getMessage());
            return null;
        }
    }

    private void chooseMediaAndTranscribe() {
        JFileChooser fc = new JFileChooser(workDir());
        fc.setDialogTitle("Choose a video or audio file");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Video / Audio (mp4, mov, mkv, webm, avi, m4v, mp3, wav, m4a, aac, flac, ogg)",
                "mp4", "mov", "mkv", "webm", "avi", "m4v",
                "mp3", "wav", "m4a", "aac", "flac", "ogg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File media = fc.getSelectedFile();
        runInBackground(() -> doVideoWordTimestamps(media));
    }

    private void doVideoWordTimestamps(File media) {
        log("VIDEO → WORD-BY-WORD TIMESTAMPS (Scribe)");
        log("========================================");
        log("Input: " + media.getName() + "  (" + mb(media.length()) + " MB)");

        File upload = media;
        if (vidExtract.isSelected() && isVideoFile(media)) {
            File audio = extractAudioWithFfmpeg(media);
            if (audio != null) upload = audio;
        }

        if (upload.length() > 1_000_000_000L)
            log("Warning: file is over 1 GB — the API may reject it. Enable FFmpeg extraction or trim the video.");

        String json;
        try {
            log("Uploading " + upload.getName() + " (" + mb(upload.length())
                    + " MB) to Scribe [" + comboVal(sttModel) + "] … this can take a while.");
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("model_id", comboVal(sttModel));
            fields.put("timestamps_granularity", "word");
            fields.put("diarize", String.valueOf(vidDiarize.isSelected()));
            fields.put("tag_audio_events", String.valueOf(vidEvents.isSelected()));
            json = scribeMultipart(upload, fields);
        } catch (Exception e) {
            log("Error calling Scribe: " + e.getMessage());
            return;
        }

        try {
            Map<String, Object> root = asObj(MiniJson.parse(json));
            String transcript = root.get("text") == null ? "" : String.valueOf(root.get("text"));
            String lang = root.get("language_code") == null ? "?" : String.valueOf(root.get("language_code"));
            Object prob = root.get("language_probability");

            List<WordStamp> stamps = new ArrayList<>();
            List<Object> words = asArr(root.get("words"));
            if (words != null) {
                for (Object o : words) {
                    Map<String, Object> w = asObj(o);
                    String type = w.get("type") == null ? "word" : String.valueOf(w.get("type"));
                    if (!"word".equals(type)) continue;
                    String text = String.valueOf(w.get("text")).trim();
                    if (text.isEmpty()) continue;
                    double start = w.get("start") == null ? 0 : dbl(w.get("start"));
                    double end   = w.get("end")   == null ? start : dbl(w.get("end"));
                    String spk   = w.get("speaker_id") == null ? "" : String.valueOf(w.get("speaker_id"));
                    stamps.add(new WordStamp(text, start, end, spk));
                }
            }

            log("Detected language: " + lang + (prob != null
                    ? String.format(Locale.US, "  (confidence %.0f%%)", dbl(prob) * 100) : ""));
            log("Words with timestamps: " + stamps.size());

            lastVideoWords = stamps;
            lastVideoBaseName = stripExt(media.getName());
            if (!pendingPhrases.isEmpty()) {
                pendingPhrases = new ArrayList<>();
                log("Note: previously resolved phrases were cleared — click \"Find & Set Phrases\" again "
                        + "so they are matched against this new transcript.");
            }

            if (stamps.isEmpty()) {
                log("No word timestamps returned. Raw transcript:");
                log(transcript.isEmpty() ? "(empty)" : transcript);
                return;
            }

            log("");
            log("Preview (first " + Math.min(15, stamps.size()) + " words):");
            for (int i = 0; i < Math.min(15, stamps.size()); i++) {
                WordStamp w = stamps.get(i);
                log(String.format(Locale.US, "   %6.3fs – %6.3fs   %s%s",
                        w.start, w.end, w.text,
                        w.speaker.isEmpty() ? "" : "   [" + w.speaker + "]"));
            }
            if (stamps.size() > 15) log("   … " + (stamps.size() - 15) + " more");

            String base = stripExt(media.getName());
            saveText(base + "_transcript.txt", transcript);
            saveText(base + "_words.json", wordsToJson(stamps));
            saveText(base + "_words.csv",  wordsToCsv(stamps));
            saveText(base + "_words.srt",  wordsToSrt(stamps));

            log("");
            log("Done. Saved: " + base + "_transcript.txt, _words.json, _words.csv, _words.srt");

        } catch (Exception e) {
            log("Error parsing Scribe response: " + e.getMessage());
        }
    }

    private String scribeMultipart(File mediaFile, Map<String, String> fields) throws Exception {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            writeAscii(baos, "--" + boundary + "\r\n");
            writeAscii(baos, "Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n");
            writeAscii(baos, e.getValue() + "\r\n");
        }
        writeAscii(baos, "--" + boundary + "\r\n");
        writeAscii(baos, "Content-Disposition: form-data; name=\"file\"; filename=\""
                + mediaFile.getName() + "\"\r\n");
        writeAscii(baos, "Content-Type: " + guessMime(mediaFile.getName()) + "\r\n\r\n");
        baos.write(Files.readAllBytes(mediaFile.toPath()));
        writeAscii(baos, "\r\n--" + boundary + "--\r\n");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.elevenlabs.io/v1/speech-to-text"))
                .timeout(Duration.ofMinutes(30))
                .header("xi-api-key", apiKey())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400)
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + resp.body());
        return resp.body();
    }

    private File extractAudioWithFfmpeg(File video) {
        if (!ffmpegAvailable()) {
            log("FFmpeg not available — uploading the video file directly instead.");
            return null;
        }
        File out = new File(workDir(), stripExt(video.getName()) + "_extracted.mp3");
        log("FFmpeg: extracting audio track …");
        boolean ok = runFfmpeg(Arrays.asList("-y",
                "-i", video.getAbsolutePath(),
                "-vn", "-acodec", "libmp3lame", "-q:a", "2",
                out.getAbsolutePath()), 15);
        if (ok && out.exists() && out.length() > 0) {
            log("FFmpeg: audio saved → " + out.getName() + "  (" + mb(out.length()) + " MB)");
            return out;
        }
        log("FFmpeg: extraction failed — uploading the video file directly instead.");
        return null;
    }


    // ---- Fixed Excel template layout (0-based column indices) ----
    // [ 32 paragraph words ][ 32 paragraph timings ]
    // [ 8 selected words   ][ 8 selected timings (start only) ]
    // [ 8 Arabic meanings  ][ 8 Arabic timings (start only, same as selected) ]
    // [ 1 logo cell ]
    private static final int PARA_WORDS       = 32;                            // paragraph word slots
    private static final int SEL_WORDS        = 8;                             // slots per 8-word group
    private static final int PARA_WORDS_START = 0;                             // cols 1..32
    private static final int PARA_TIME_START  = PARA_WORDS_START + PARA_WORDS;  // cols 33..64
    private static final int SEL_WORDS_START  = PARA_TIME_START + PARA_WORDS;   // cols 65..72
    private static final int SEL_TIME_START   = SEL_WORDS_START + SEL_WORDS;    // cols 73..80
    private static final int AR_WORDS_START   = SEL_TIME_START + SEL_WORDS;     // cols 81..88
    private static final int AR_TIME_START    = AR_WORDS_START + SEL_WORDS;     // cols 89..96
    private static final int NEW_WORDS_START  = AR_TIME_START + SEL_WORDS;      // cols 97..104
    private static final int NEW_TIME_START   = NEW_WORDS_START + SEL_WORDS;    // cols 105..112
    private static final int LOGO_COL         = NEW_TIME_START + SEL_WORDS;     // col  113
    private static final int TOTAL_COLS       = LOGO_COL + 1;                   // 113 columns
    private static final String LOGO_TEXT     = "Logolept  AR · EN   [f]"; // logo placeholder

    // Per-group cell fill styles (indices into the xlsx cellXfs table; 0 = no fill).
    private static final int STYLE_PARA = 1, STYLE_SEL = 2, STYLE_AR = 3,
                             STYLE_NEW = 4, STYLE_LOGO = 5;

    /** Map every template column to its group fill style, for coloured export. */
    private static int[] buildColStyles() {
        int[] s = new int[TOTAL_COLS];
        for (int i = 0; i < PARA_WORDS; i++) {
            s[PARA_WORDS_START + i] = STYLE_PARA;
            s[PARA_TIME_START + i]  = STYLE_PARA;
        }
        for (int i = 0; i < SEL_WORDS; i++) {
            s[SEL_WORDS_START + i] = STYLE_SEL;  s[SEL_TIME_START + i] = STYLE_SEL;
            s[AR_WORDS_START + i]  = STYLE_AR;   s[AR_TIME_START + i]  = STYLE_AR;
            s[NEW_WORDS_START + i] = STYLE_NEW;  s[NEW_TIME_START + i] = STYLE_NEW;
        }
        s[LOGO_COL] = STYLE_LOGO;
        return s;
    }

    /** Write up to SEL_WORDS lines of free text into consecutive cells from startCol,
     *  aligned by line index (blank lines advance the index but write nothing).
     *  Returns how many non-empty cells were written. */
    private static int fillWordColumn(List<String> row, int startCol, String text) {
        if (text == null) return 0;
        String[] lines = text.split("\\R", -1);
        int written = 0;
        for (int i = 0; i < lines.length && i < SEL_WORDS; i++) {
            String v = lines[i].trim();
            if (!v.isEmpty()) { row.set(startCol + i, v); written++; }
        }
        return written;
    }

    private void exportWordsToExcel() {
        List<WordStamp> stamps = lastVideoWords;
        String base = lastVideoBaseName;
        if (stamps == null || stamps.isEmpty()) {
            log("Export to Excel: no word timestamps yet — run \"Choose video / audio…\" first.");
            return;
        }

        log("Exporting " + stamps.size() + " word(s) to Excel using the fixed template "
                + "(32 paragraph + 32 timing · 8 selected + 8 · 8 Arabic + 8 · 8 new + 8 · 1 logo, "
                + "each group colour-coded)…");
        if (stamps.size() > PARA_WORDS)
            log("Note: paragraph has " + stamps.size() + " words — more than the " + PARA_WORDS
                    + " reserved cells, so it wraps onto additional rows.");

        List<List<String>> rows = buildExcelRows(stamps);
        List<String> dataRow = rows.get(1);            // first data row (row 2)

        // Logo cell (last column of the first data row).
        dataRow.set(LOGO_COL, LOGO_TEXT);

        // Columns 2 & 3 of the Selected-Words panel: free text, aligned by line with column 1.
        int arWritten  = fillWordColumn(dataRow, AR_WORDS_START,  arabicArea.getText());
        int newWritten = fillWordColumn(dataRow, NEW_WORDS_START, newGroupArea.getText());
        if (arWritten > 0 || newWritten > 0)
            log("Typed meanings written: " + arWritten + " → "
                    + XlsxWriter.colLetter(AR_WORDS_START + 1) + "2… (text41+), "
                    + newWritten + " → " + XlsxWriter.colLetter(NEW_WORDS_START + 1) + "2… (text49+).");

        // Selected words + their timings (row 2), sourced from the resolved phrases.
        List<PhraseHit> phrases = pendingPhrases;
        if (phrases != null && !phrases.isEmpty()) {
            int n = Math.min(phrases.size(), SEL_WORDS);
            if (phrases.size() > SEL_WORDS)
                log("Note: " + phrases.size() + " selected phrases resolved but only " + SEL_WORDS
                        + " slots are reserved — writing the first " + SEL_WORDS + ".");
            for (int i = 0; i < n; i++) {
                PhraseHit p = phrases.get(i);
                String startOnly = String.format(Locale.US, "%.3f", p.start); // start time only
                dataRow.set(SEL_WORDS_START + i, p.text);     // selected word / phrase
                dataRow.set(SEL_TIME_START + i,  startOnly);  // selected timing (start only)
                // text41 / text49 word cells come from columns 2 & 3; their timing mirrors the start time.
                dataRow.set(AR_TIME_START + i,   startOnly);  // text41+ timing = same start time
                dataRow.set(NEW_TIME_START + i,  startOnly);  // text49+ timing = same start time
            }
            log(n + " selected phrase(s) written: "
                    + XlsxWriter.colLetter(SEL_WORDS_START + 1) + "2… (words), "
                    + XlsxWriter.colLetter(SEL_TIME_START + 1) + "2… (start times); "
                    + "text41+ / text49+ timing columns carry the same start times.");
        } else {
            log("No selected phrases set — use \"Find & Set Phrases\" to fill the "
                    + SEL_WORDS + " selected-word slots.");
        }

        File out = new File(workDir(), (base == null || base.isEmpty() ? "words" : base) + "_words.xlsx");
        try {
            XlsxWriter.write(out, rows, buildColStyles());
            log("Saved: " + out.getName() + "  (" + (rows.size() - 1) + " paragraph row(s), "
                    + TOTAL_COLS + " columns)");
        } catch (Exception e) {
            log("Error writing xlsx: " + e.getMessage());
        }
    }

    private void setPhraseOverrides() {
        String raw = phraseArea.getText();
        List<String> wanted = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String t = line.trim();
            if (!t.isEmpty()) wanted.add(t);
        }

        if (wanted.isEmpty()) {
            if (pendingPhrases.isEmpty()) log("Phrases: type at least one phrase (one per line) first.");
            else {
                pendingPhrases = new ArrayList<>();
                log("Phrases cleared — the next export will not write a phrase block.");
            }
            return;
        }

        List<WordStamp> stamps = lastVideoWords;
        if (stamps == null || stamps.isEmpty()) {
            log("Phrases: no transcribed words yet — run \"Choose video / audio…\" first.");
            return;
        }

        List<PhraseHit> hits = new ArrayList<>();
        int missing = 0;
        for (String phrase : wanted) {
            String[] tokens = phrase.split("\\s+");

            List<int[]> matches = findPhraseMatches(stamps, tokens, true);
            boolean normalized = false;
            if (matches.isEmpty()) {
                matches = findPhraseMatches(stamps, tokens, false);
                normalized = !matches.isEmpty();
            }

            if (matches.isEmpty()) {
                missing++;
                log("Phrase NOT found: \"" + phrase + "\" — check the exact wording in the saved *_words.csv.");
                continue;
            }

            int[] first = matches.get(0);
            PhraseHit hit = new PhraseHit(phrase,
                    stamps.get(first[0]).start, stamps.get(first[1]).end);
            hits.add(hit);

            log(String.format(Locale.US, "Phrase %s: \"%s\" -> %.3fs to %.3fs (words #%d-%d)%s",
                    normalized ? "found (normalized match)" : "found",
                    phrase, hit.start, hit.end, first[0] + 1, first[1] + 1,
                    matches.size() > 1 ? "  [+" + (matches.size() - 1) + " more occurrence(s), using the first]" : ""));
        }

        if (hits.isEmpty()) {
            log("No phrase could be resolved — the previous phrase set (if any) is unchanged.");
            return;
        }
        pendingPhrases = hits;
        log(hits.size() + " phrase(s) ready" + (missing > 0 ? "  (" + missing + " not found and skipped)" : "")
                + " — they will be written to row 2, right after the word/time columns, "
                + "on the next \"Export to Excel\".");
    }

    private static List<int[]> findPhraseMatches(List<WordStamp> stamps, String[] tokens, boolean exact) {
        List<int[]> out = new ArrayList<>();
        if (tokens.length == 0) return out;
        for (int start = 0; start + tokens.length <= stamps.size(); start++) {
            boolean ok = true;
            for (int j = 0; j < tokens.length; j++) {
                String a = stamps.get(start + j).text;
                String b = tokens[j];
                boolean same = exact ? a.equals(b) : normalize(a).equals(normalize(b));
                if (!same || (!exact && normalize(b).isEmpty())) { ok = false; break; }
            }
            if (ok) out.add(new int[]{start, start + tokens.length - 1});
        }
        return out;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replaceAll("[^\\p{L}\\p{N}']", "");
    }

    /** Build rows for the fixed template: header + paragraph words (wrapped across the
     *  32 reserved word columns) with paragraph start,end timings. Selected-word, Arabic
     *  and logo cells are filled by the caller on the first data row. */
    private static List<List<String>> buildExcelRows(List<WordStamp> stamps) {
        List<List<String>> rows = new ArrayList<>();

        // Header row.
        List<String> header = new ArrayList<>(Collections.nCopies(TOTAL_COLS, ""));
        for (int i = 0; i < PARA_WORDS; i++) {
            header.set(PARA_WORDS_START + i, "text" + (i + 1));
            header.set(PARA_TIME_START + i,  "text" + (i + 1) + "time");
        }
        for (int i = 0; i < SEL_WORDS; i++) {
            int selN = PARA_WORDS + i + 1;                 // selected:  text33..text40
            int arN  = PARA_WORDS + SEL_WORDS + i + 1;     // Arabic:    text41..text48
            int newN = PARA_WORDS + 2 * SEL_WORDS + i + 1; // new group: text49..text56
            header.set(SEL_WORDS_START + i, "text" + selN);
            header.set(SEL_TIME_START + i,  "text" + selN + "time");
            header.set(AR_WORDS_START + i,  "text" + arN);
            header.set(AR_TIME_START + i,   "text" + arN + "time");
            header.set(NEW_WORDS_START + i, "text" + newN);
            header.set(NEW_TIME_START + i,  "text" + newN + "time");
        }
        header.set(LOGO_COL, "text" + (PARA_WORDS + 3 * SEL_WORDS + 1)); // logo header continues: text57
        rows.add(header);

        if (stamps.isEmpty()) {
            rows.add(new ArrayList<>(Collections.nCopies(TOTAL_COLS, "")));
            return rows;
        }

        // Paragraph words wrapped across the PARA_WORDS reserved columns.
        for (int start = 0; start < stamps.size(); start += PARA_WORDS) {
            List<String> row = new ArrayList<>(Collections.nCopies(TOTAL_COLS, ""));
            int end = Math.min(start + PARA_WORDS, stamps.size());
            for (int i = start; i < end; i++) {
                WordStamp w = stamps.get(i);
                int col = i - start;
                row.set(PARA_WORDS_START + col, w.text);
                row.set(PARA_TIME_START + col, String.format(Locale.US, "%.3f,%.3f", w.start, w.end));
            }
            rows.add(row);
        }
        return rows;
    }
    static final class XlsxWriter {
        private static final String CONTENT_TYPES =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                        "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                        "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                        "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                        "</Types>";

        // One solid fill per group (fill indices 2..6, referenced by cellXfs 1..5).
        private static final String STYLES =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                        "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
                        "<fills count=\"7\">" +
                        "<fill><patternFill patternType=\"none\"/></fill>" +
                        "<fill><patternFill patternType=\"gray125\"/></fill>" +
                        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFBDD7EE\"/><bgColor indexed=\"64\"/></patternFill></fill>" + // paragraph  – blue
                        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFC6E0B4\"/><bgColor indexed=\"64\"/></patternFill></fill>" + // selected   – green
                        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFF2CC\"/><bgColor indexed=\"64\"/></patternFill></fill>" + // arabic     – yellow
                        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFCCC0DA\"/><bgColor indexed=\"64\"/></patternFill></fill>" + // new group  – purple
                        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF8CBAD\"/><bgColor indexed=\"64\"/></patternFill></fill>" + // logo       – peach
                        "</fills>" +
                        "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                        "<cellXfs count=\"6\">" +
                        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>" +
                        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>" +
                        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"4\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>" +
                        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"5\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>" +
                        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"6\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/>" +
                        "</cellXfs>" +
                        "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
                        "</styleSheet>";

        private static final String RELS =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                        "</Relationships>";

        private static final String WORKBOOK =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                        "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                        "<sheets><sheet name=\"Words\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                        "</workbook>";

        private static final String WORKBOOK_RELS =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                        "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                        "</Relationships>";

        static void write(File file, List<List<String>> rows, int[] colStyles) throws IOException {
            try (ZipOutputStream zos = new ZipOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file)))) {
                putEntry(zos, "[Content_Types].xml", CONTENT_TYPES);
                putEntry(zos, "_rels/.rels", RELS);
                putEntry(zos, "xl/workbook.xml", WORKBOOK);
                putEntry(zos, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
                putEntry(zos, "xl/styles.xml", STYLES);
                putEntry(zos, "xl/worksheets/sheet1.xml", sheetXml(rows, colStyles));
            }
        }

        private static void putEntry(ZipOutputStream zos, String name, String content) throws IOException {
            zos.putNextEntry(new ZipEntry(name));
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        private static String sheetXml(List<List<String>> rows, int[] colStyles) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            for (int r = 0; r < rows.size(); r++) {
                List<String> row = rows.get(r);
                sb.append("<row r=\"").append(r + 1).append("\">");
                for (int c = 0; c < row.size(); c++) {
                    String val   = row.get(c);
                    int    style = (colStyles != null && c < colStyles.length) ? colStyles[c] : 0;
                    boolean empty = (val == null || val.isEmpty());
                    if (empty && style == 0) continue;      // nothing to write for this cell
                    String ref = colLetter(c + 1) + (r + 1);
                    if (empty) {
                        sb.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"/>");
                    } else {
                        sb.append("<c r=\"").append(ref).append("\"");
                        if (style != 0) sb.append(" s=\"").append(style).append("\"");
                        sb.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                                .append(escapeXml(val)).append("</t></is></c>");
                    }
                }
                sb.append("</row>");
            }
            sb.append("</sheetData></worksheet>");
            return sb.toString();
        }

        static String colLetter(int n) {
            StringBuilder sb = new StringBuilder();
            while (n > 0) {
                int rem = (n - 1) % 26;
                sb.insert(0, (char) ('A' + rem));
                n = (n - 1) / 26;
            }
            return sb.toString();
        }

        private static String escapeXml(String s) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&':  b.append("&amp;");  break;
                    case '<':  b.append("&lt;");   break;
                    case '>':  b.append("&gt;");   break;
                    case '"':  b.append("&quot;"); break;
                    case '\'': b.append("&apos;"); break;
                    default:
                        if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') b.append(c);
                }
            }
            return b.toString();
        }
    }

    private static String wordsToJson(List<WordStamp> stamps) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < stamps.size(); i++) {
            WordStamp w = stamps.get(i);
            sb.append("  { \"word\": ").append(jsonStr(w.text))
                    .append(String.format(Locale.US, ", \"start\": %.3f, \"end\": %.3f", w.start, w.end));
            if (!w.speaker.isEmpty()) sb.append(", \"speaker\": ").append(jsonStr(w.speaker));
            sb.append(" }").append(i < stamps.size() - 1 ? "," : "").append("\n");
        }
        return sb.append("]").toString();
    }

    private static String wordsToCsv(List<WordStamp> stamps) {
        StringBuilder sb = new StringBuilder("index,word,start,end,speaker\n");
        for (int i = 0; i < stamps.size(); i++) {
            WordStamp w = stamps.get(i);
            sb.append(i + 1).append(',')
                    .append('"').append(w.text.replace("\"", "\"\"")).append('"').append(',')
                    .append(String.format(Locale.US, "%.3f", w.start)).append(',')
                    .append(String.format(Locale.US, "%.3f", w.end)).append(',')
                    .append(w.speaker).append('\n');
        }
        return sb.toString();
    }

    private static String wordsToSrt(List<WordStamp> stamps) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stamps.size(); i++) {
            WordStamp w = stamps.get(i);
            sb.append(i + 1).append('\n')
                    .append(srtTime(w.start)).append(" --> ").append(srtTime(w.end)).append('\n')
                    .append(w.text).append("\n\n");
        }
        return sb.toString();
    }

    private static String srtTime(double seconds) {
        long ms = Math.round(seconds * 1000.0);
        long h = ms / 3_600_000; ms %= 3_600_000;
        long m = ms / 60_000;    ms %= 60_000;
        long s = ms / 1_000;     ms %= 1_000;
        return String.format("%02d:%02d:%02d,%03d", h, m, s, ms);
    }

    private void saveText(String fileName, String content) {
        try {
            Files.write(new File(workDir(), fileName).toPath(),
                    content.getBytes(StandardCharsets.UTF_8));
            log("Saved: " + fileName);
        } catch (Exception e) {
            log("Error saving " + fileName + ": " + e.getMessage());
        }
    }

    private static boolean isVideoFile(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".mkv")
                || n.endsWith(".webm") || n.endsWith(".avi") || n.endsWith(".m4v");
    }

    private static String guessMime(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".mp4") || n.endsWith(".m4v")) return "video/mp4";
        if (n.endsWith(".mov"))  return "video/quicktime";
        if (n.endsWith(".mkv"))  return "video/x-matroska";
        if (n.endsWith(".webm")) return "video/webm";
        if (n.endsWith(".avi"))  return "video/x-msvideo";
        if (n.endsWith(".mp3"))  return "audio/mpeg";
        if (n.endsWith(".wav"))  return "audio/wav";
        if (n.endsWith(".m4a"))  return "audio/mp4";
        if (n.endsWith(".aac"))  return "audio/aac";
        if (n.endsWith(".flac")) return "audio/flac";
        if (n.endsWith(".ogg"))  return "audio/ogg";
        return "application/octet-stream";
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String mb(long bytes) {
        return String.format(Locale.US, "%.1f", bytes / 1_048_576.0);
    }

    private static class WordStamp {
        final String text; final double start, end; final String speaker;
        WordStamp(String t, double s, double e, String spk) { text = t; start = s; end = e; speaker = spk; }
    }

    private static class PhraseHit {
        final String text; final double start, end;
        PhraseHit(String t, double s, double e) { text = t; start = s; end = e; }
    }

    private String speechToText(File audioFile) {
        try {
            log("Transcribing audio: " + audioFile.getName());
            if (!audioFile.exists()) { log("Audio file not found: " + audioFile.getName()); return null; }

            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("model_id", comboVal(sttModel));
            String json = scribeMultipart(audioFile, fields);

            Map<String, Object> data = asObj(MiniJson.parse(json));
            Object text = data.containsKey("transcript") ? data.get("transcript") : data.get("text");
            log("Transcription complete for: " + audioFile.getName());
            return text == null ? "" : String.valueOf(text);

        } catch (Exception e) {
            log("Error transcribing audio: " + e.getMessage());
            return null;
        }
    }

    private void doTranscribe() {
        log("Starting Audio Transcription with ElevenLabs Scribe...");
        log("=====================================================");

        final String merged = mergedFile().getName();
        File[] arr = workDir().listFiles((FilenameFilter)
                (d, name) -> name.toLowerCase().endsWith(".mp3")
                        && !name.equals(merged)
                        && !name.endsWith("_extracted.mp3"));
        if (arr == null || arr.length == 0) { log("No MP3 files found in the folder."); return; }
        List<File> files = new ArrayList<>(Arrays.asList(arr));
        files.sort(Comparator.comparing(File::getName));

        log("Found " + files.size() + " audio files to transcribe.");
        List<String[]> transcriptions = new ArrayList<>();
        int ok = 0;
        for (int i = 0; i < files.size(); i++) {
            String t = speechToText(files.get(i));
            if (t != null) {
                transcriptions.add(new String[]{ files.get(i).getName(), t });
                ok++;
                try {
                    File tx = new File(workDir(), files.get(i).getName().replace(".mp3", "_transcription.txt"));
                    Files.write(tx.toPath(), t.getBytes(StandardCharsets.UTF_8));
                    log("Saved transcription: " + tx.getName());
                } catch (Exception e) { log("Error saving transcription: " + e.getMessage()); }
            }
            if (i < files.size() - 1) sleep(1000);
        }

        if (!transcriptions.isEmpty()) {
            StringBuilder all = new StringBuilder();
            for (String[] t : transcriptions)
                all.append("=== ").append(t[0]).append(" ===\n").append(t[1]).append("\n\n");
            try {
                Files.write(new File(workDir(), "all_transcriptions.txt").toPath(),
                        all.toString().getBytes(StandardCharsets.UTF_8));
                log("All transcriptions saved to: all_transcriptions.txt");
            } catch (Exception e) { log("Error writing all_transcriptions.txt: " + e.getMessage()); }
        }
        log("");
        log("TRANSCRIPTION COMPLETE! Successfully transcribed " + ok + " out of " + files.size() + " files.");
    }

    private void doCompare() {
        log("Comparing Original Text with ElevenLabs Transcriptions...");
        log("========================================================");

        List<String> original = readQuotes();
        if (original.isEmpty()) { log("Cannot compare - original script is empty."); return; }

        File[] arr = workDir().listFiles((FilenameFilter)
                (d, name) -> name.endsWith("_transcription.txt"));
        if (arr == null || arr.length == 0) { log("No transcription files found."); return; }
        List<File> txFiles = new ArrayList<>(Arrays.asList(arr));
        txFiles.sort(Comparator.comparing(File::getName));

        log("Accuracy Analysis:");
        log("====================");

        int totalMatches = 0;
        int n = Math.min(original.size(), txFiles.size());
        for (int i = 0; i < n; i++) {
            String transcribed;
            try {
                transcribed = new String(Files.readAllBytes(txFiles.get(i).toPath()), StandardCharsets.UTF_8).trim();
            } catch (Exception e) { log("Error reading " + txFiles.get(i).getName()); continue; }
            String orig = original.get(i);

            log("");
            log((i + 1) + ". " + txFiles.get(i).getName());
            log("Original:     \"" + orig + "\"");
            log("Transcribed:  \"" + transcribed + "\"");

            if (orig.trim().equalsIgnoreCase(transcribed.trim())) {
                totalMatches++;
                log("Status:       Perfect Match");
            } else {
                log("Status:       Differences Found");
                String[] ow = orig.toLowerCase().trim().split("\\s+");
                String[] tw = transcribed.toLowerCase().trim().split("\\s+");
                int maxLen = Math.max(ow.length, tw.length);
                int matching = 0;
                for (int j = 0; j < Math.min(ow.length, tw.length); j++)
                    if (ow[j].equals(tw[j])) matching++;
                int acc = maxLen == 0 ? 0 : Math.round(matching * 100f / maxLen);
                log("Word Accuracy: " + acc + "%");
            }
        }
        int overall = n == 0 ? 0 : Math.round(totalMatches * 100f / n);
        log("");
        log("Overall Accuracy: " + overall + "% (" + totalMatches + "/" + n + " perfect matches)");
    }

    private void doFullWorkflow() {
        log("COMPLETE ELEVENLABS WORKFLOW");
        log("===============================");
        doGenerateTts();
        log("");
        log("Waiting 3 seconds before transcription...");
        sleep(3000);
        doTranscribe();
        doCompare();
    }

    private byte[] postForBytes(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("xi-api-key", apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400)
            throw new RuntimeException("HTTP " + resp.statusCode() + " "
                    + new String(resp.body(), StandardCharsets.UTF_8));
        return resp.body();
    }

    private String postForString(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("xi-api-key", apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400)
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + resp.body());
        return resp.body();
    }

    private static final String[] MODELS = {
            "eleven_v3", "eleven_multilingual_v2", "eleven_turbo_v2", "eleven_flash_v2_5"
    };
    private static final String[] STT_MODELS = { "scribe_v2", "scribe_v1" };

    private JComboBox<String> voiceCombo(String defaultId) {
        JComboBox<String> cb = new JComboBox<>();
        for (String[] v : VOICES) cb.addItem(v[1]);
        cb.setEditable(true);
        cb.setSelectedItem(defaultId);
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                    int index, boolean isSel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, isSel, focus);
                String id = String.valueOf(value);
                String name = voiceName(id);
                setText(name == null ? id : name + "  —  " + id);
                return this;
            }
        });
        voiceCombos.add(cb);
        return cb;
    }

    /** Sentinel value for the optional 2nd-voice dropdown: empty = "no 2nd voice". */
    private static final String NO_VOICE2 = "";

    /** Like voiceCombo, but with a leading "(no 2nd voice)" option and registered for none-restore. */
    private JComboBox<String> voiceCombo2(String defaultId) {
        JComboBox<String> cb = new JComboBox<>();
        cb.addItem(NO_VOICE2);
        for (String[] v : VOICES) cb.addItem(v[1]);
        cb.setEditable(true);
        cb.setSelectedItem(defaultId == null ? NO_VOICE2 : defaultId);
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                    int index, boolean isSel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, isSel, focus);
                String id = value == null ? "" : String.valueOf(value);
                if (id.isEmpty()) { setText("(no 2nd voice)"); return this; }
                String name = voiceName(id);
                setText(name == null ? id : name + "  \u2014  " + id);
                return this;
            }
        });
        voiceCombos.add(cb);
        voice2Combos.add(cb);
        return cb;
    }

    private static JComboBox<String> modelCombo(String defaultId) {
        JComboBox<String> cb = new JComboBox<>(MODELS);
        cb.setEditable(true);
        cb.setSelectedItem(defaultId);
        return cb;
    }

    private static JComboBox<String> sttModelCombo(String defaultId) {
        JComboBox<String> cb = new JComboBox<>(STT_MODELS);
        cb.setEditable(true);
        cb.setSelectedItem(defaultId);
        return cb;
    }

    private static String comboVal(JComboBox<String> cb) {
        Object o = cb.isEditable() ? cb.getEditor().getItem() : cb.getSelectedItem();
        if (o == null) o = cb.getSelectedItem();
        return o == null ? "" : o.toString().trim();
    }

    private static class Word {
        String word; double start, end;
        Word(String w, double s, double e) { word = w; start = s; end = e; }
    }

    private static class LineRow {
        final JPanel panel;
        final JComboBox<String> voice;
        final JComboBox<String> voice2;   // optional 2nd voice ("" = none)
        final JTextField text;
        LineRow(JPanel p, JComboBox<String> v, JComboBox<String> v2, JTextField t) {
            panel = p; voice = v; voice2 = v2; text = t;
        }
    }

    private static class QuoteItem {
        final String voice, voice2, text;
        QuoteItem(String v, String v2, String t) { voice = v; voice2 = v2; text = t; }
    }

    private static void writeAscii(ByteArrayOutputStream b, String s) {
        byte[] d = s.getBytes(StandardCharsets.UTF_8);
        b.write(d, 0, d.length);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private static String preview(String s) {
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }

    private static String num(JTextField f) {
        String t = f.getText().trim();
        try { Double.parseDouble(t); return t; } catch (Exception e) { return "0"; }
    }

    private static String trim(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v))
            return String.valueOf((long) v);
        return String.valueOf(v);
    }

    private static double dbl(Object o) { return ((Number) o).doubleValue(); }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObj(Object o) { return (Map<String, Object>) o; }
    @SuppressWarnings("unchecked")
    private static List<Object> asArr(Object o) { return (List<Object>) o; }

    private static String jsonStr(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                case '\b': b.append("\\b");  break;
                case '\f': b.append("\\f");  break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append("\"").toString();
    }

    static final class MiniJson {
        private final String s; private int i;
        private MiniJson(String s) { this.s = s; }
        static Object parse(String s) { MiniJson p = new MiniJson(s); p.ws(); return p.value(); }
        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private Object value() {
            char c = s.charAt(i);
            switch (c) {
                case '{': return obj();
                case '[': return arr();
                case '"': return str();
                case 't': i += 4; return Boolean.TRUE;
                case 'f': i += 5; return Boolean.FALSE;
                case 'n': i += 4; return null;
                default:  return num();
            }
        }
        private Map<String, Object> obj() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; ws();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws(); String k = str(); ws();
                i++;
                ws(); m.put(k, value()); ws();
                char c = s.charAt(i++);
                if (c == '}') break;
            }
            return m;
        }
        private List<Object> arr() {
            List<Object> a = new ArrayList<>();
            i++; ws();
            if (s.charAt(i) == ']') { i++; return a; }
            while (true) {
                ws(); a.add(value()); ws();
                char c = s.charAt(i++);
                if (c == ']') break;
            }
            return a;
        }
        private String str() {
            StringBuilder b = new StringBuilder();
            i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"':  b.append('"');  break;
                        case '\\': b.append('\\'); break;
                        case '/':  b.append('/');  break;
                        case 'b':  b.append('\b'); break;
                        case 'f':  b.append('\f'); break;
                        case 'n':  b.append('\n'); break;
                        case 'r':  b.append('\r'); break;
                        case 't':  b.append('\t'); break;
                        case 'u':  b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default:   b.append(e);
                    }
                } else b.append(c);
            }
            return b.toString();
        }
        private Double num() {
            int st = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || (c >= '0' && c <= '9')) i++;
                else break;
            }
            return Double.parseDouble(s.substring(st, i));
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ElevenLabsStudio().setVisible(true));
    }
}