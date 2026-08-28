import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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

    // ---- 7) Excel / CSV batch: column(s) of text -> audio -> column(s) of links ----
    private final JTextField xlFileField  = new JTextField("");
    private final JTextField xlSheetField = new JTextField("");      // blank = first sheet
    private final JTextField xlTextCol    = new JTextField("A");
    private final JTextField xlLinkCol    = new JTextField("B");
    private final JTextField xlStartRow   = new JTextField("2");
    private final JTextField xlBaseUrl    = new JTextField("");
    private final JComboBox<String> xlVoice  = voiceCombo("TX3LPaxmHKxFdv7VOQHJ");
    private final JComboBox<String> xlVoice2 = voiceCombo2(null);   // blank = 1st voice everywhere

    private final JComboBox<String> sttModel  = sttModelCombo("scribe_v2");
    private final JCheckBox vidExtract = new JCheckBox("FFmpeg audio extract", true);
    private final JCheckBox vidDiarize = new JCheckBox("Speaker diarization", false);
    private final JCheckBox vidEvents  = new JCheckBox("Tag audio events", false);

    /** One transcribed video/audio file: its words, its transcript and its resolved phrases. */
    private static final class MediaResult {
        final String baseName;
        final String transcript;
        final List<WordStamp> words;
        List<PhraseHit> phrases = new ArrayList<>();
        MediaResult(String baseName, String transcript, List<WordStamp> words) {
            this.baseName = baseName; this.transcript = transcript; this.words = words;
        }
    }

    /** Every file of the last batch, in the order it was transcribed. One entry = one Excel row block. */
    private volatile List<MediaResult> lastVideoResults = new ArrayList<>();

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
        List<File> media = new ArrayList<>();
        for (File f : files) {
            if (f != null && f.isFile() && isMediaFile(f)) media.add(f);
        }
        if (media.isEmpty()) {
            log("Drag & drop: no supported video/audio file found "
                    + "(accepted: mp4, mov, mkv, webm, avi, m4v, mp3, wav, m4a, aac, flac, ogg).");
            return;
        }
        media.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        int skipped = files.size() - media.size();
        if (media.size() > 1)
            log("Drag & drop: " + media.size() + " media files — transcribing them in name order…"
                    + (skipped > 0 ? "  (" + skipped + " unsupported file(s) ignored)" : ""));
        else
            log("Drag & drop: \"" + media.get(0).getName() + "\" — transcribing…"
                    + (skipped > 0 ? "  (" + skipped + " unsupported file(s) ignored)" : ""));
        runInBackground(() -> doVideoWordTimestamps(media));
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
        JButton btnVid = new JButton("Choose video(s) / audio…");
        btnVid.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnVid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnVid.setToolTipText("<html>Upload one or more video/audio files and get word-by-word start/end "
                + "timestamps (JSON + CSV + SRT per file).<br>Ctrl/Shift-click to pick several — they are "
                + "transcribed one after another, the scripts are stacked into one combined transcript, "
                + "and each file gets its own Excel row.</html>");
        btnVid.addActionListener(e -> chooseMediaAndTranscribe());
        actionButtons.add(btnVid);
        vidBox.add(Box.createVerticalStrut(4));
        vidBox.add(btnVid);
        JLabel dropHint = new JLabel("…or drag & drop one or more files onto the window");
        dropHint.setFont(dropHint.getFont().deriveFont(Font.ITALIC, 11f));
        dropHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        vidBox.add(Box.createVerticalStrut(2));
        vidBox.add(dropHint);

        JButton btnPick = new JButton("Select Words…");
        btnPick.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPick.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnPick.setToolTipText("<html>Show every transcribed word — of one file or of all of them — and "
                + "pick the ones to export.<br>Click a word to select it, ctrl-click to add more, "
                + "shift-click for a run of words.<br>\"Add as one phrase\" joins a run into a single "
                + "entry; the Arabic and new-group cells are typed beside each pick.</html>");
        btnPick.addActionListener(e -> openWordPicker());
        actionButtons.add(btnPick);
        vidBox.add(Box.createVerticalStrut(4));
        vidBox.add(btnPick);

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
                + "Selected words come from \"Select Words…\" (up to 8 per file).<br>"
                + "With several files, each one gets its own row block, stacked in the order "
                + "they were transcribed.</html>");
        btnExport.addActionListener(e -> runInBackground(this::exportWordsToExcel));
        actionButtons.add(btnExport);
        vidBox.add(Box.createVerticalStrut(4));
        vidBox.add(btnExport);



        west.add(vidBox);

        JPanel xlBox = settingsBox("7 · Excel / CSV batch");
        xlFileField.setToolTipText("The .xlsx / .xlsm / .csv / .tsv file holding the text to speak.");
        addRow(xlBox, "Excel file", xlFileField);
        JButton xlBrowse = new JButton("Browse file…");
        xlBrowse.setAlignmentX(Component.LEFT_ALIGNMENT);
        xlBrowse.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        xlBrowse.addActionListener(e -> chooseExcelFile());
        xlBox.add(xlBrowse);
        xlSheetField.setToolTipText("Sheet name or 1-based number. Blank = the first sheet.");
        addRow(xlBox, "Sheet", xlSheetField);
        xlTextCol.setToolTipText("<html>Column(s) the text is read from.<br>"
                + "<b>A-J</b> is the span A through J · <b>A,J</b> is just those two · "
                + "<b>A-C,F</b> mixes both.<br>"
                + "Every non-empty cell becomes its own audio file, read row by row.</html>");
        addRow(xlBox, "Text col(s)", xlTextCol);
        xlLinkCol.setToolTipText("<html>Column(s) the audio links are written into — same syntax, "
                + "so <b>K-T</b> is K through T.<br>"
                + "They pair with the text columns in order: A-J → K-T writes A's links to K, "
                + "B's to L … J's to T.<br>Each link lands on the same row as its text.</html>");
        addRow(xlBox, "Link col(s)", xlLinkCol);
        xlStartRow.setToolTipText("First data row — 2 skips a header row.");
        addRow(xlBox, "First row", xlStartRow);
        xlVoice.setToolTipText("<html>Voice for the batch. With no 2nd voice below it speaks every "
                + "column.<br>Model and voice settings come from box 1.</html>");
        addRow(xlBox, "Voice", xlVoice);
        xlVoice2.setToolTipText("<html>Optional 2nd voice. When set, the columns <b>alternate</b> between "
                + "the two voices<br>in the order they are listed: with A-J, voice 1 speaks A, C, E, G, I "
                + "and voice 2 speaks B, D, F, H, J.<br>Leave on \"(no 2nd voice)\" to use voice 1 "
                + "everywhere.</html>");
        addRow(xlBox, "Voice 2", xlVoice2);
        xlBaseUrl.setToolTipText("<html>Optional. Blank writes the full file path.<br>"
                + "Set e.g. https://my.site/audio to write a web link instead:<br>"
                + "https://my.site/audio/&lt;new folder&gt;/&lt;file&gt;.mp3</html>");
        addRow(xlBox, "Link base URL", xlBaseUrl);
        JButton btnXlLoad = action("Load column → lines", this::doExcelLoadColumn);
        btnXlLoad.setToolTipText("Copy the text column(s) into the quotes panel, so they can be "
                + "reviewed or edited before generating.");
        xlBox.add(Box.createVerticalStrut(4));
        xlBox.add(btnXlLoad);
        JButton btnXlRun = action("Generate from Excel", this::doExcelBatch);
        btnXlRun.setToolTipText("<html>Reads the text column(s), generates one MP3 per filled cell into a NEW "
                + "folder named after the first data cell + a timestamp,<br>then writes every file's full link "
                + "back into the paired link column, on the row its text came from.</html>");
        xlBox.add(Box.createVerticalStrut(4));
        xlBox.add(btnXlRun);
        west.add(xlBox);

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

    /**
     * Where a single line's audio lives: the file it owns in an Excel-batch folder
     * when it came from one, otherwise the panel's own m1, m2, m3… in the work folder.
     * Bulk actions (Generate Audio, Merge) always use the panel numbering.
     */
    private File rowAudioFile(LineRow row, int zeroBasedIdx) {
        return row.batch != null ? row.batch.audio : ttsAudioFile(zeroBasedIdx);
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

        File f = rowAudioFile(row, idx);
        if (f.isFile() && f.length() > 0) { player.playAsync(f); return; }

        log("Listen: no audio yet at " + f.getName() + " — generating it now …");
        runInBackground(() -> {
            regenerateLine(row);
            File g = rowAudioFile(row, idx);
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
        File out = rowAudioFile(row, idx);
        log("Regenerating " + (row.batch != null ? out.getName() : "audio " + (idx + 1))
                + " [voice " + voiceLabel(v1)
                + (two ? " + " + voiceLabel(v2) : "") + "]: \"" + preview(text) + "\"");
        if (generateLineTts(v1, v2, text, comboVal(ttsModel), ttsSettingsJson(), out))
            fillMissingLink(row);
    }

    /**
     * After regenerating a batch line, put its link in the sheet if that cell is still
     * empty — which is the case when the cell failed during the batch, so no link was
     * ever written. A cell that already holds a link is left exactly as it is: the
     * audio path has not changed, so what is there is still right.
     */
    private void fillMissingLink(LineRow row) {
        BatchCell b = row.batch;
        if (b == null) return;
        try {
            if (!b.book.isFile()) return;
            for (SheetCell c : readSheetCells(b.book, b.sheet,
                    Collections.singletonList(b.linkCol), b.linkRow))
                if (c.row == b.linkRow) return;                    // a link is already there
            String link = audioLink(b.audio.getParentFile(), b.audio);
            writeSheetCells(b.book, b.sheet,
                    Collections.singletonList(new SheetCell(b.linkRow, b.linkCol, link)));
            log("Link was missing — wrote it into " + b.linkRef() + " of " + b.book.getName());
        } catch (Exception e) {
            log("Note: could not write the link into " + b.linkRef() + ": " + e.getMessage()
                    + " (close the workbook in Excel and click ↻ Regen again)");
        }
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
        fc.setDialogTitle("Choose one or more video / audio files");
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileNameExtensionFilter(
                "Video / Audio (mp4, mov, mkv, webm, avi, m4v, mp3, wav, m4a, aac, flac, ogg)",
                "mp4", "mov", "mkv", "webm", "avi", "m4v",
                "mp3", "wav", "m4a", "aac", "flac", "ogg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        List<File> media = new ArrayList<>(Arrays.asList(fc.getSelectedFiles()));
        if (media.isEmpty() && fc.getSelectedFile() != null) media.add(fc.getSelectedFile());
        if (media.isEmpty()) return;
        media.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        runInBackground(() -> doVideoWordTimestamps(media));
    }

    /**
     * Transcribe every chosen file in turn, exactly as the single-file run always did, then
     * stack the results: one combined transcript with each script below the previous one, and
     * one Excel row block per file on the next "Export to Excel".
     */
    private void doVideoWordTimestamps(List<File> media) {
        if (media == null || media.isEmpty()) return;

        boolean hadPhrases = false;
        for (MediaResult r : lastVideoResults) if (!r.phrases.isEmpty()) hadPhrases = true;

        if (media.size() > 1) {
            log("VIDEO → WORD-BY-WORD TIMESTAMPS (Scribe) — " + media.size() + " files");
            log("========================================");
            for (int i = 0; i < media.size(); i++)
                log("   " + (i + 1) + ". " + media.get(i).getName());
            log("");
        }

        List<MediaResult> results = new ArrayList<>();
        for (int i = 0; i < media.size(); i++) {
            File f = media.get(i);
            if (media.size() > 1) {
                log("");
                log("---- File " + (i + 1) + " of " + media.size() + ": " + f.getName() + " ----");
            }
            MediaResult r = transcribeOneMedia(f);
            if (r != null) results.add(r);
        }

        if (results.isEmpty()) {
            log("");
            log("Nothing transcribed — the previous results (if any) are unchanged.");
            return;
        }

        lastVideoResults = results;
        if (hadPhrases)
            log("Note: previously picked words were cleared — click \"Select Words…\" again "
                    + "to pick from the new transcript(s).");

        if (results.size() > 1) {
            saveText(combinedBaseName(results) + "_transcript.txt", combinedTranscript(results));
            int total = 0;
            for (MediaResult r : results) total += r.words.size();
            log("");
            log("Batch done: " + results.size() + " of " + media.size() + " file(s) transcribed, "
                    + total + " word(s) in total. \"Export to Excel\" writes one row block per file, "
                    + "in this order.");
        }
    }

    /** All scripts below each other, each under its file name. */
    private static String combinedTranscript(List<MediaResult> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            MediaResult r = results.get(i);
            if (i > 0) sb.append("\n\n");
            sb.append("=== ").append(i + 1).append(". ").append(r.baseName).append(" ===\n");
            sb.append(r.transcript.trim());
        }
        sb.append('\n');
        return sb.toString();
    }

    /** Output name for a batch: the first file plus how many followed it. */
    private static String combinedBaseName(List<MediaResult> results) {
        String first = results.get(0).baseName;
        if (first == null || first.isEmpty()) first = "words";
        return results.size() == 1 ? first : first + "_and_" + (results.size() - 1) + "_more";
    }

    /** The original single-file pipeline: transcribe one file and save its own .txt/.json/.csv/.srt. */
    private MediaResult transcribeOneMedia(File media) {
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
            return null;
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

            if (stamps.isEmpty()) {
                log("No word timestamps returned. Raw transcript:");
                log(transcript.isEmpty() ? "(empty)" : transcript);
                return null;
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
            return new MediaResult(base, transcript, stamps);

        } catch (Exception e) {
            log("Error parsing Scribe response: " + e.getMessage());
            return null;
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

    private void exportWordsToExcel() {
        List<MediaResult> results = lastVideoResults;
        if (results.isEmpty()) {
            log("Export to Excel: no word timestamps yet — run \"Choose video(s) / audio…\" first.");
            return;
        }

        int totalWords = 0;
        for (MediaResult r : results) totalWords += r.words.size();
        log("Exporting " + totalWords + " word(s) from " + results.size() + " file(s) to Excel using the "
                + "fixed template (32 paragraph + 32 timing · 8 selected + 8 · 8 Arabic + 8 · 8 new + 8 · "
                + "1 logo, each group colour-coded)…");

        List<List<String>> rows = new ArrayList<>();
        rows.add(buildExcelHeader());

        for (int f = 0; f < results.size(); f++) {
            MediaResult r = results.get(f);
            String tag = results.size() == 1 ? "" : "[" + r.baseName + "] ";

            if (r.words.size() > PARA_WORDS)
                log(tag + "Note: paragraph has " + r.words.size() + " words — more than the " + PARA_WORDS
                        + " reserved cells, so it wraps onto additional rows.");

            List<List<String>> block = buildParagraphRows(r.words);
            List<String> dataRow = block.get(0);   // this file's first row — its whole panel lives here

            // Logo cell (last column of the file's first row).
            dataRow.set(LOGO_COL, LOGO_TEXT);

            // Words/phrases picked in the word chooser, each with the meaning cells typed beside it.
            List<PhraseHit> phrases = r.phrases;
            if (!phrases.isEmpty()) {
                int n = Math.min(phrases.size(), SEL_WORDS);
                if (phrases.size() > SEL_WORDS)
                    log(tag + "Note: " + phrases.size() + " words picked but only " + SEL_WORDS
                            + " slots are reserved — writing the first " + SEL_WORDS + ".");
                int meanings = 0;
                for (int i = 0; i < n; i++) {
                    PhraseHit ph = phrases.get(i);
                    String startOnly = String.format(Locale.US, "%.3f", ph.start); // start time only
                    dataRow.set(SEL_WORDS_START + i, ph.text);     // picked word / phrase
                    dataRow.set(SEL_TIME_START + i,  startOnly);   // picked timing (start only)
                    // text41 / text49 carry the meanings typed beside the pick; their timing mirrors it.
                    if (!ph.arabic.isEmpty())   { dataRow.set(AR_WORDS_START + i,  ph.arabic);   meanings++; }
                    if (!ph.newGroup.isEmpty()) { dataRow.set(NEW_WORDS_START + i, ph.newGroup); meanings++; }
                    dataRow.set(AR_TIME_START + i,   startOnly);   // text41+ timing = same start time
                    dataRow.set(NEW_TIME_START + i,  startOnly);   // text49+ timing = same start time
                }
                log(tag + n + " picked word(s)/phrase(s) written to row " + (rows.size() + 1) + ": "
                        + XlsxWriter.colLetter(SEL_WORDS_START + 1) + " (words), "
                        + XlsxWriter.colLetter(SEL_TIME_START + 1) + " (start times), "
                        + meanings + " meaning cell(s) in "
                        + XlsxWriter.colLetter(AR_WORDS_START + 1) + " / "
                        + XlsxWriter.colLetter(NEW_WORDS_START + 1) + ".");
            } else {
                log(tag + "No words picked — use \"Select Words…\" to fill the "
                        + SEL_WORDS + " selected-word slots.");
            }

            rows.addAll(block);
        }

        File out = new File(workDir(), combinedBaseName(results) + "_words.xlsx");
        try {
            XlsxWriter.write(out, rows, buildColStyles());
            log("Saved: " + out.getName() + "  (" + results.size() + " file(s), "
                    + (rows.size() - 1) + " paragraph row(s), " + TOTAL_COLS + " columns)");
        } catch (Exception e) {
            log("Error writing xlsx: " + e.getMessage());
        }
    }

    /** Open the word chooser on the last batch, so words/phrases can be picked for the export. */
    private void openWordPicker() {
        List<MediaResult> results = lastVideoResults;
        if (results.isEmpty()) {
            log("Select Words: no transcribed words yet — run \"Choose video(s) / audio…\" first.");
            return;
        }
        new WordPickerDialog(this, results).setVisible(true);
    }

    /** Log what a finished pick left on each file's row. */
    private void logPicks(List<MediaResult> results) {
        int total = 0, files = 0;
        for (MediaResult r : results) {
            if (r.phrases.isEmpty()) continue;
            files++; total += r.phrases.size();
            StringBuilder sb = new StringBuilder();
            for (PhraseHit ph : r.phrases) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(String.format(Locale.US, "\"%s\" @%.3fs", ph.text, ph.start));
            }
            log((results.size() == 1 ? "" : "[" + r.baseName + "] ") + r.phrases.size()
                    + " pick(s): " + sb);
        }
        if (total == 0) log("No words picked — the next export writes no selected-word block.");
        else log(total + " word(s)/phrase(s) picked across " + files + " of " + results.size()
                + " file(s) — each file's picks go on its own row on the next \"Export to Excel\".");
    }

    /**
     * Every transcribed word of every file, laid out as a grid you can click. Picked words and
     * phrases land in the table below, where the text41 and text49 cells are typed beside them,
     * so the three Excel columns stay aligned by construction.
     */
    private final class WordPickerDialog extends JDialog {
        private final List<MediaResult> results;
        private final List<List<PhraseHit>> picks = new ArrayList<>();   // per file, in export order
        private final DefaultTableModel model;
        private final JTable table;
        private final JTabbedPane tabs = new JTabbedPane();
        private final List<JList<String>> wordLists = new ArrayList<>();
        private final JLabel status = new JLabel(" ");

        WordPickerDialog(JFrame owner, List<MediaResult> results) {
            super(owner, "Select words for the Excel export", true);
            this.results = results;
            for (MediaResult r : results) picks.add(new ArrayList<>(r.phrases));  // keep earlier picks

            // ---- top: one word grid per file ----
            for (MediaResult r : results) {
                DefaultListModel<String> lm = new DefaultListModel<>();
                for (int i = 0; i < r.words.size(); i++) {
                    WordStamp w = r.words.get(i);
                    lm.addElement(String.format(Locale.US, "%d. %s", i + 1, w.text));
                }
                JList<String> list = new JList<>(lm);
                list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                list.setVisibleRowCount(-1);                 // wrap to the viewport width
                list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                list.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                list.setToolTipText("<html>Click a word · ctrl-click to add more · shift-click for a run."
                        + "<br>Double-click adds the word on its own.</html>");
                list.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (e.getClickCount() == 2) addSelection(false);
                    }
                });
                wordLists.add(list);

                JScrollPane sp = new JScrollPane(list);
                sp.getVerticalScrollBar().setUnitIncrement(16);
                tabs.addTab(r.baseName + "  (" + r.words.size() + ")", sp);
            }
            tabs.addChangeListener(e -> refreshStatus());

            JButton addWords  = new JButton("Add as separate words");
            addWords.setToolTipText("Each selected word becomes its own entry (text33, text34, …).");
            addWords.addActionListener(e -> addSelection(false));
            JButton addPhrase = new JButton("Add as one phrase");
            addPhrase.setToolTipText("Join the selected run of consecutive words into a single entry, "
                    + "timed from the first word.");
            addPhrase.addActionListener(e -> addSelection(true));

            JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            addBar.add(addWords);
            addBar.add(addPhrase);
            addBar.add(status);

            JPanel top = new JPanel(new BorderLayout());
            top.setBorder(new TitledBorder("All transcribed words — click to select"));
            top.add(tabs, BorderLayout.CENTER);
            top.add(addBar, BorderLayout.SOUTH);

            // ---- bottom: what will be written, with the two meaning columns ----
            model = new DefaultTableModel(new Object[]{"#", "File", "Word / phrase",
                    "start", "text41 (Arabic)", "text49 (new group)"}, 0) {
                @Override public boolean isCellEditable(int row, int col) { return col >= 4; }
            };
            table = new JTable(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            table.getColumnModel().getColumn(0).setPreferredWidth(30);
            table.getColumnModel().getColumn(1).setPreferredWidth(120);
            table.getColumnModel().getColumn(2).setPreferredWidth(220);
            table.getColumnModel().getColumn(3).setPreferredWidth(60);
            table.getColumnModel().getColumn(4).setPreferredWidth(160);
            table.getColumnModel().getColumn(5).setPreferredWidth(160);
            model.addTableModelListener(e -> {
                if (e.getColumn() < 4 || e.getFirstRow() < 0) return;
                PhraseHit ph = rowToPick(e.getFirstRow());
                if (ph == null) return;
                Object v = model.getValueAt(e.getFirstRow(), e.getColumn());
                String text = v == null ? "" : String.valueOf(v).trim();
                if (e.getColumn() == 4) ph.arabic = text; else ph.newGroup = text;
            });

            JButton remove = new JButton("Remove selected");
            remove.addActionListener(e -> removeSelectedRows());
            JButton clear  = new JButton("Clear all");
            clear.addActionListener(e -> { for (List<PhraseHit> p : picks) p.clear(); rebuildTable(); });

            JPanel rowBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            rowBar.add(remove);
            rowBar.add(clear);
            rowBar.add(new JLabel("— text41 and text49 are typed straight into the table"));

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBorder(new TitledBorder("Picked for export  (up to " + SEL_WORDS
                    + " per file → text33-40, one row per file)"));
            bottom.add(new JScrollPane(table), BorderLayout.CENTER);
            bottom.add(rowBar, BorderLayout.SOUTH);

            JButton ok = new JButton("Apply");
            ok.addActionListener(e -> apply());
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> dispose());
            JPanel okBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
            okBar.add(cancel);
            okBar.add(ok);
            getRootPane().setDefaultButton(ok);

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
            split.setResizeWeight(0.6);
            setLayout(new BorderLayout());
            add(split, BorderLayout.CENTER);
            add(okBar, BorderLayout.SOUTH);
            setSize(900, 620);
            setLocationRelativeTo(owner);
            rebuildTable();
        }

        private int fileIdx() { return Math.max(0, tabs.getSelectedIndex()); }

        /** Add the current word selection, either as one entry per word or as a single phrase. */
        private void addSelection(boolean asPhrase) {
            int f = fileIdx();
            JList<String> list = wordLists.get(f);
            int[] sel = list.getSelectedIndices();
            if (sel.length == 0) { status("Select one or more words first."); return; }

            List<PhraseHit> mine = picks.get(f);
            List<WordStamp> words = results.get(f).words;

            if (asPhrase) {
                if (sel[sel.length - 1] - sel[0] + 1 != sel.length) {
                    status("A phrase needs consecutive words — shift-click a run.");
                    return;
                }
                if (mine.size() >= SEL_WORDS) { status("That file already has " + SEL_WORDS + " picks."); return; }
                StringBuilder sb = new StringBuilder();
                for (int i : sel) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(words.get(i).text);
                }
                mine.add(new PhraseHit(sb.toString(), words.get(sel[0]).start,
                        words.get(sel[sel.length - 1]).end, sel[0], sel[sel.length - 1]));
                status("Added phrase \"" + sb + "\".");
            } else {
                int added = 0;
                for (int i : sel) {
                    if (mine.size() >= SEL_WORDS) break;
                    WordStamp w = words.get(i);
                    mine.add(new PhraseHit(w.text, w.start, w.end, i, i));
                    added++;
                }
                status(added == sel.length
                        ? "Added " + added + " word(s)."
                        : "Added " + added + " of " + sel.length + " — " + SEL_WORDS + " per file is the limit.");
            }
            list.clearSelection();
            rebuildTable();
        }

        private void removeSelectedRows() {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) { status("Select a table row to remove."); return; }
            List<PhraseHit> doomed = new ArrayList<>();
            for (int r : rows) doomed.add(rowToPick(r));
            for (List<PhraseHit> p : picks) p.removeAll(doomed);
            rebuildTable();
            status("Removed " + rows.length + " pick(s).");
        }

        /** Table rows run file by file, in the same order the export writes them. */
        private PhraseHit rowToPick(int row) {
            int n = 0;
            for (List<PhraseHit> p : picks) {
                if (row < n + p.size()) return p.get(row - n);
                n += p.size();
            }
            return null;
        }

        private void rebuildTable() {
            model.setRowCount(0);
            for (int f = 0; f < picks.size(); f++) {
                List<PhraseHit> mine = picks.get(f);
                for (int i = 0; i < mine.size(); i++) {
                    PhraseHit ph = mine.get(i);
                    model.addRow(new Object[]{"text" + (PARA_WORDS + i + 1), results.get(f).baseName,
                            ph.text, String.format(Locale.US, "%.3f", ph.start), ph.arabic, ph.newGroup});
                }
            }
            refreshStatus();
        }

        private void refreshStatus() {
            int f = fileIdx();
            status.setText("  " + picks.get(f).size() + " of " + SEL_WORDS
                    + " picked for " + results.get(f).baseName);
        }

        private void status(String msg) { status.setText("  " + msg); }

        private void apply() {
            if (table.isEditing()) table.getCellEditor().stopCellEditing();
            for (int f = 0; f < results.size(); f++) results.get(f).phrases = picks.get(f);
            dispose();
            logPicks(results);
        }
    }

    /** The template header row: text1..text57 plus the matching *time columns. */
    private static List<String> buildExcelHeader() {
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
        return header;
    }

    /** One file's row block (no header): its paragraph words wrapped across the 32 reserved word
     *  columns with their start,end timings. Always at least one row, so the caller can fill the
     *  selected-word, Arabic, new-group and logo cells on row 0 of the block. */
    private static List<List<String>> buildParagraphRows(List<WordStamp> stamps) {
        List<List<String>> rows = new ArrayList<>();

        if (stamps.isEmpty()) {
            rows.add(new ArrayList<>(Collections.nCopies(TOTAL_COLS, "")));
            return rows;
        }

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

    /** One word or phrase picked in the word chooser, with the two meaning cells typed beside it. */
    private static class PhraseHit {
        final String text; final double start, end;
        final int firstIdx, lastIdx;        // the word range it was picked from
        String arabic = "";                 // -> text41+ on this file's row
        String newGroup = "";               // -> text49+ on this file's row
        PhraseHit(String t, double s, double e, int firstIdx, int lastIdx) {
            text = t; start = s; end = e; this.firstIdx = firstIdx; this.lastIdx = lastIdx;
        }
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

    // ---- 7) Excel / CSV batch --------------------------------------------
    //  Read one or more columns of text, voice every cell into a new timestamped
    //  folder, then write each audio file's full link back into another column.
    //  Text and link columns pair up 1-to-1: A,B,C -> X,Y,Z means A's links go
    //  to X, B's to Y and C's to Z, each on the row its text came from.

    private void chooseExcelFile() {
        String cur = xlFileField.getText().trim();
        JFileChooser fc = new JFileChooser(cur.isEmpty() ? workDir() : new File(cur).getParentFile());
        fc.setFileFilter(new FileNameExtensionFilter(
                "Spreadsheets (*.xlsx, *.xlsm, *.csv, *.tsv)", "xlsx", "xlsm", "csv", "tsv"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            xlFileField.setText(fc.getSelectedFile().getAbsolutePath());
    }

    /** The spreadsheet chosen in the Excel box, validated. */
    private File excelFile() {
        String p = xlFileField.getText().trim();
        if (p.isEmpty()) throw new IllegalArgumentException("no file selected — click Browse file…");
        File f = new File(p);
        if (!f.isFile()) throw new IllegalArgumentException("file not found: " + p);
        return f;
    }

    private String sheetSel()   { return xlSheetField.getText().trim(); }
    private int    xlFirstRow() {
        try { return Math.max(1, Integer.parseInt(xlStartRow.getText().trim())); }
        catch (Exception e) { return 1; }
    }

    /** Copy the text column(s) into the quotes panel so they can be reviewed / edited. */
    private void doExcelLoadColumn() {
        List<SheetCell> cells;
        List<Integer> textCols;
        File book;
        try {
            book     = excelFile();
            textCols = colIndexList(xlTextCol.getText());
            cells    = readSheetCells(book, sheetSel(), textCols, xlFirstRow());
        } catch (Exception e) { log("Excel: " + e.getMessage()); return; }

        if (cells.isEmpty()) {
            log("Excel: no text found in " + colList(textCols)
                    + " from row " + xlFirstRow() + " of " + book.getName());
            return;
        }
        Map<Integer, String> voiceOf = voiceByColumn(textCols);
        Map<String, BatchCell> earlier = lastBatchFiles(book);   // audio from a previous batch, if any
        int[] attached = {0};
        SwingUtilities.invokeLater(() -> {
            clearLineRows();
            for (SheetCell c : cells) {
                LineRow r = insertLineRow(lineRows.size(), c.text, voiceOf.get(c.col));
                BatchCell b = earlier.get(XlsxWriter.colLetter(c.col) + c.row);
                if (b != null) { r.batch = b; attached[0]++; }
            }
            if (lineRows.isEmpty()) addLineRow("", comboVal(ttsVoice));
            refreshLines();
            if (attached[0] > 0)
                log("Excel: " + attached[0] + " of them already have audio from an earlier batch — "
                        + "▶ Listen plays it, ↻ Regen redoes it in place.");
        });
        log("Excel: loaded " + cells.size() + " cells from " + colList(textCols)
                + " of " + book.getName() + " (row by row)");
    }

    /** Read the text column(s), voice every cell into a fresh folder, write the links back. */
    private void doExcelBatch() {
        log("EXCEL BATCH — text column(s) → audio → link column(s)");
        log("=====================================================");

        File book;
        List<SheetCell> cells;
        List<Integer> textCols, linkCols;
        try {
            book     = excelFile();
            textCols = colIndexList(xlTextCol.getText());
            linkCols = colIndexList(xlLinkCol.getText());
            checkColumnPairs(textCols, linkCols);
            cells    = readSheetCells(book, sheetSel(), textCols, xlFirstRow());
        } catch (Exception e) { log("Excel: " + e.getMessage()); return; }

        if (cells.isEmpty()) {
            log("Excel: no text found in " + colList(textCols)
                    + " from row " + xlFirstRow() + " of " + book.getName());
            return;
        }
        // text column -> the link column it writes to
        Map<Integer, Integer> linkOf = new LinkedHashMap<>();
        for (int i = 0; i < textCols.size(); i++) linkOf.put(textCols.get(i), linkCols.get(i));

        Map<Integer, String> voiceOf = voiceByColumn(textCols);

        log("File  : " + book.getAbsolutePath());
        log("Pairs : " + pairList(textCols, linkCols));
        log("Voices: " + voiceSplit(textCols, voiceOf));
        log("Found " + cells.size() + " cells of text (rows " + cells.get(0).row
                + "–" + cells.get(cells.size() - 1).row + ")");

        // per-column tally, so an empty column is obvious instead of silent
        Map<Integer, Integer> perCol = new LinkedHashMap<>();
        for (int c : textCols) perCol.put(c, 0);
        for (SheetCell c : cells) perCol.merge(c.col, 1, Integer::sum);
        StringBuilder tally = new StringBuilder();
        List<String> empty = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : perCol.entrySet()) {
            if (tally.length() > 0) tally.append(", ");
            tally.append(XlsxWriter.colLetter(e.getKey())).append(' ').append(e.getValue());
            if (e.getValue() == 0) empty.add(XlsxWriter.colLetter(e.getKey()));
        }
        log("Per column: " + tally);
        if (!empty.isEmpty())
            log("Note: nothing to say in " + colList(colIndexList(String.join(",", empty)))
                    + " — if you meant a span of columns write A-J, because A,J is just those two.");

        // new folder named after the first data cell + timestamp
        File outDir = new File(workDir(), batchFolderName(cells.get(0).text));
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            log("Excel: could not create folder " + outDir.getAbsolutePath());
            return;
        }
        log("Output folder: " + outDir.getAbsolutePath());
        log("");

        String model    = comboVal(ttsModel);
        String settings = ttsSettingsJson(model);
        String prefix   = ttsPrefix.getText().trim();
        if (prefix.isEmpty()) prefix = "audio";

        // one text column keeps the plain m1, m2, m3… naming; with several the
        // cell reference goes in the name, so every file says where it came from
        boolean multi = textCols.size() > 1;

        List<SheetCell> links = new ArrayList<>();
        List<MadeCell> made = new ArrayList<>();
        StringBuilder index = new StringBuilder();
        int ok = 0;
        for (int i = 0; i < cells.size(); i++) {
            SheetCell c = cells.get(i);
            String cellRef = XlsxWriter.colLetter(c.col) + c.row;
            String voice   = voiceOf.get(c.col);
            File out = new File(outDir, multi ? prefix + "_" + cellRef + ".mp3"
                                              : prefix + (i + 1) + ".mp3");
            log("Generating audio " + (i + 1) + "/" + cells.size() + " (" + cellRef + ") [voice "
                    + voiceLabel(voice) + "]: \"" + preview(c.text) + "\"");
            made.add(new MadeCell(cellRef, c.col, c.text, out, c.row, linkOf.get(c.col)));
            if (generateOneTts(voice, c.text, model, settings, out)) {
                ok++;
                int linkCol = linkOf.get(c.col);
                String link = audioLink(outDir, out);
                links.add(new SheetCell(c.row, linkCol, link));
                index.append(cellRef).append(" → ").append(XlsxWriter.colLetter(linkCol)).append(c.row)
                     .append('\t').append(link).append('\n');
            }
        }
        log("");
        log("Generated " + ok + " out of " + cells.size() + " audio files.");
        if (links.isEmpty()) { log("No links to write back."); return; }

        // always leave a plain-text copy of the links next to the audio
        try {
            Files.write(new File(outDir, "links.txt").toPath(),
                    index.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { log("Note: could not write links.txt: " + e.getMessage()); }

        // a manifest so a later "Load column → lines" can find these files again
        writeBatchManifest(outDir, book, made);

        // put the generated cells in the quotes panel, each line owning its file,
        // so ▶ Listen and ↻ Regen work on the batch audio straight away
        showBatchInPanel(made, book, voiceOf);
        log("The " + made.size() + " lines are in the quotes panel — ▶ Listen to check one, "
                + "↻ Regen to redo it in place (its link stays valid).");

        try {
            File backup = backupOf(book);
            writeSheetCells(book, sheetSel(), links);
            log("Wrote " + links.size() + " links into " + colList(linkCols)
                    + " of " + book.getName());
            log("Backup of the original: " + backup.getName());
        } catch (Exception e) {
            log("Excel: could not write the links back: " + e.getMessage());
            log("       (close the file in Excel and try again — links.txt in the "
                    + "output folder has them all)");
        }
    }

    /** What one quotes-panel line owns: its batch audio and the sheet cell its link belongs in. */
    private static class BatchCell {
        final File audio, book; final String sheet; final int linkRow, linkCol;
        BatchCell(File audio, File book, String sheet, int linkRow, int linkCol) {
            this.audio = audio; this.book = book; this.sheet = sheet;
            this.linkRow = linkRow; this.linkCol = linkCol;
        }
        String linkRef() { return XlsxWriter.colLetter(linkCol) + linkRow; }
    }

    /** One generated cell: where it came from, what it says, and the file it owns. */
    private static class MadeCell {
        final String ref, text; final int col, linkRow, linkCol; final File file;
        MadeCell(String ref, int col, String text, File file, int linkRow, int linkCol) {
            this.ref = ref; this.col = col; this.text = text; this.file = file;
            this.linkRow = linkRow; this.linkCol = linkCol;
        }
    }

    /** Name of the manifest that ties a batch folder's files back to their cells. */
    private static final String BATCH_MANIFEST = "batch_cells.tsv";

    /** Records cell → file for this batch so a later session can re-attach to it. */
    private void writeBatchManifest(File dir, File book, List<MadeCell> made) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(book.getName()).append('\t').append(sheetSel()).append('\n');
        for (MadeCell m : made)
            sb.append(m.ref).append('\t').append(m.file.getName()).append('\t')
              .append(XlsxWriter.colLetter(m.linkCol)).append(m.linkRow).append('\n');
        try {
            Files.write(new File(dir, BATCH_MANIFEST).toPath(),
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { log("Note: could not write " + BATCH_MANIFEST + ": " + e.getMessage()); }
    }

    /** Fills the quotes panel with the cells just generated, each line owning its file. */
    private void showBatchInPanel(List<MadeCell> made, File book, Map<Integer, String> voiceOf) {
        String sheet = sheetSel();
        SwingUtilities.invokeLater(() -> {
            clearLineRows();
            for (MadeCell m : made) {
                LineRow r = insertLineRow(lineRows.size(), m.text, voiceOf.get(m.col));
                r.batch = new BatchCell(m.file, book, sheet, m.linkRow, m.linkCol);
            }
            if (lineRows.isEmpty()) addLineRow("", comboVal(ttsVoice));
            refreshLines();
        });
    }

    /**
     * The newest batch folder under the work folder that was generated from this
     * workbook and sheet, or null. Lets "Load column → lines" re-attach to audio
     * made in an earlier session.
     */
    private Map<String, BatchCell> lastBatchFiles(File book) {
        File[] dirs = workDir().listFiles(File::isDirectory);
        if (dirs == null) return Collections.emptyMap();
        File[] sorted = dirs.clone();
        Arrays.sort(sorted, Comparator.comparingLong(File::lastModified).reversed());
        for (File d : sorted) {
            File man = new File(d, BATCH_MANIFEST);
            if (!man.isFile()) continue;
            try {
                List<String> lines = Files.readAllLines(man.toPath(), StandardCharsets.UTF_8);
                if (lines.isEmpty() || !lines.get(0).startsWith("# ")) continue;
                String[] head = lines.get(0).substring(2).split("\t", -1);
                if (!head[0].equals(book.getName())) continue;
                String sheet = head.length > 1 ? head[1] : "";
                if (!sheet.equals(sheetSel())) continue;
                Map<String, BatchCell> out = new HashMap<>();
                for (String line : lines.subList(1, lines.size())) {
                    String[] p = line.split("\t", -1);
                    if (p.length < 3) continue;
                    File f = new File(d, p[1]);
                    if (!f.isFile()) continue;
                    Matcher ref = Pattern.compile("([A-Za-z]+)(\\d+)").matcher(p[2].trim());
                    if (!ref.matches()) continue;
                    out.put(p[0], new BatchCell(f, book, sheet,
                            Integer.parseInt(ref.group(2)), colIndex(ref.group(1))));
                }
                if (!out.isEmpty()) return out;
            } catch (Exception ignored) { }
        }
        return Collections.emptyMap();
    }

    /** Full link written into the sheet: a web URL when a base URL is set, else the file path. */
    private String audioLink(File dir, File audio) {
        String base = xlBaseUrl.getText().trim();
        if (base.isEmpty()) return audio.getAbsolutePath();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/" + urlPart(dir.getName()) + "/" + urlPart(audio.getName());
    }

    /** Percent-encode one path segment so folder / file names survive in a URL. */
    private static String urlPart(String s) {
        StringBuilder sb = new StringBuilder();
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') sb.append(c);
            else sb.append(String.format("%%%02X", b & 0xff));
        }
        return sb.toString();
    }

    /** Folder name for one batch: first data cell (sanitised) + _yyyyMMdd_HHmmss. */
    private static String batchFolderName(String firstCell) {
        StringBuilder sb = new StringBuilder();
        for (char c : firstCell.trim().toCharArray()) {
            if (Character.isLetterOrDigit(c)) sb.append(c);
            else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') sb.append('_');
            if (sb.length() >= 40) break;
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') sb.setLength(sb.length() - 1);
        String name = sb.length() == 0 ? "audio" : sb.toString();
        return name + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /** Timestamped copy of the workbook, made before it is modified. */
    private static File backupOf(File f) throws Exception {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        String stem = dot < 0 ? name : name.substring(0, dot);
        String ext  = dot < 0 ? ""   : name.substring(dot);
        File bak = new File(f.getParentFile(), stem + ".backup-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ext);
        Files.copy(f.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return bak;
    }

    // ---- Spreadsheet I/O: .xlsx / .xlsm (zip + XML) and .csv / .tsv -------
    //  Reads and edits EXISTING workbooks in place, where XlsxWriter above
    //  writes new ones. Dependency-free, like the rest of this program.

    /** One cell: 1-based sheet row and column, plus its text. */
    private static class SheetCell {
        final int row, col; final String text;
        SheetCell(int row, int col, String text) { this.row = row; this.col = col; this.text = text; }
    }

    private static boolean isXlsx(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".xlsx") || n.endsWith(".xlsm");
    }

    /** Every non-empty cell of the given columns, ordered row by row. */
    private static List<SheetCell> readSheetCells(File f, String sheetSel, List<Integer> cols, int startRow)
            throws Exception {
        List<SheetCell> out = isXlsx(f) ? readXlsxCells(f, sheetSel, cols, startRow)
                                        : readCsvCells(f, cols, startRow);
        Map<Integer, Integer> order = new HashMap<>();          // keep the user's column order
        for (int i = 0; i < cols.size(); i++) order.putIfAbsent(cols.get(i), i);
        out.sort(Comparator.<SheetCell>comparingInt(c -> c.row)
                .thenComparingInt(c -> order.getOrDefault(c.col, 0)));
        return out;
    }

    private static void writeSheetCells(File f, String sheetSel, List<SheetCell> writes)
            throws Exception {
        if (writes.isEmpty()) return;
        if (isXlsx(f)) writeXlsxCells(f, sheetSel, writes);
        else           writeCsvCells(f, writes);
    }

    // ---- column letters ----------------------------------------------------

    /** "A" -> 1, "b" -> 2, "AA" -> 27. Also accepts a plain number. */
    private static int colIndex(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("empty column");
        if (t.chars().allMatch(Character::isDigit)) {
            int n = Integer.parseInt(t);
            if (n < 1) throw new IllegalArgumentException("bad column: " + s);
            return n;
        }
        int n = 0;
        for (char c : t.toCharArray()) {
            char u = Character.toUpperCase(c);
            if (u < 'A' || u > 'Z') throw new IllegalArgumentException("bad column: " + s);
            n = n * 26 + (u - 'A' + 1);
        }
        return n;
    }

    /** Splits a range: A-J, A:J, A..J or "A to J" (the word needs spaces around it). */
    private static final Pattern COL_RANGE = Pattern.compile("\\s*(?:\\.\\.|[-:])\\s*|\\s+(?i:to)\\s+");

    /**
     * "A" -> [1]; "A,C,E" -> [1,3,5]; "A-J" -> [1..10]; "A-C,F" -> [1,2,3,6].
     * A comma always means "just these columns", never a range — use A-J for a span.
     */
    private static List<Integer> colIndexList(String s) {
        List<Integer> out = new ArrayList<>();
        for (String part : (s == null ? "" : s).split("[,;]")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            String[] ends = COL_RANGE.split(t, -1);   // -1 keeps a trailing "" so "A-" is caught
            if (ends.length == 1) { out.add(colIndex(ends[0])); continue; }
            if (ends.length != 2 || ends[0].trim().isEmpty() || ends[1].trim().isEmpty())
                throw new IllegalArgumentException("cannot read the column range \"" + t + "\"");
            int from = colIndex(ends[0]), to = colIndex(ends[1]);
            int step = from <= to ? 1 : -1;
            for (int c = from; ; c += step) {
                out.add(c);
                if (c == to) break;
            }
        }
        if (out.isEmpty()) throw new IllegalArgumentException("no column given");
        return out;
    }

    /** Reject the pairings that would lose data before a single file is generated. */
    private static void checkColumnPairs(List<Integer> textCols, List<Integer> linkCols) {
        if (textCols.size() != linkCols.size())
            throw new IllegalArgumentException(textCols.size() + " text column(s) but "
                    + linkCols.size() + " link column(s) — they pair up one to one, "
                    + "so list the same number of each");
        if (new HashSet<>(textCols).size() != textCols.size())
            throw new IllegalArgumentException("the same text column is listed twice");
        if (new HashSet<>(linkCols).size() != linkCols.size())
            throw new IllegalArgumentException("the same link column is listed twice — "
                    + "each text column needs its own");
        for (int c : linkCols)
            if (textCols.contains(c))
                throw new IllegalArgumentException("column " + XlsxWriter.colLetter(c)
                        + " is listed as both text and link — the links would overwrite the text");
    }

    /**
     * The voice each text column is spoken in. With a 2nd voice set, the columns
     * alternate between the two in the order they were listed; otherwise every
     * column uses the 1st voice.
     */
    private Map<Integer, String> voiceByColumn(List<Integer> textCols) {
        String v1 = comboVal(xlVoice), v2 = comboVal(xlVoice2);
        boolean alt = !v2.isEmpty() && !v2.equals(v1);
        Map<Integer, String> out = new LinkedHashMap<>();
        for (int i = 0; i < textCols.size(); i++)
            out.put(textCols.get(i), (alt && i % 2 == 1) ? v2 : v1);
        return out;
    }

    /** "A, C, E → Liam · B, D → Brian", for the log. */
    private static String voiceSplit(List<Integer> textCols, Map<Integer, String> voiceOf) {
        Map<String, List<String>> byVoice = new LinkedHashMap<>();
        for (int c : textCols)
            byVoice.computeIfAbsent(voiceOf.get(c), v -> new ArrayList<>())
                   .add(XlsxWriter.colLetter(c));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> e : byVoice.entrySet()) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(String.join(", ", e.getValue())).append(" → ").append(voiceLabel(e.getKey()));
        }
        return sb.toString();
    }

    /** "column A" / "columns A, C, E". */
    private static String colList(List<Integer> cols) {
        StringBuilder sb = new StringBuilder(cols.size() == 1 ? "column " : "columns ");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(XlsxWriter.colLetter(cols.get(i)));
        }
        return sb.toString();
    }

    /** "A → X, C → Y". */
    private static String pairList(List<Integer> textCols, List<Integer> linkCols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < textCols.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(XlsxWriter.colLetter(textCols.get(i)))
              .append(" → ").append(XlsxWriter.colLetter(linkCols.get(i)));
        }
        return sb.toString();
    }

    // ---- CSV / TSV ---------------------------------------------------------

    private static char csvSep(File f) {
        return f.getName().toLowerCase(Locale.ROOT).endsWith(".tsv") ? '\t' : ',';
    }

    private static List<List<String>> readCsvRows(File f) throws Exception {
        String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        if (s.startsWith("﻿")) s = s.substring(1);
        char sep = csvSep(f);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else quoted = false;
                } else cur.append(c);
            } else if (c == '"')      quoted = true;
            else if (c == sep)        { row.add(cur.toString()); cur.setLength(0); }
            else if (c == '\n')       { row.add(cur.toString()); cur.setLength(0); rows.add(row); row = new ArrayList<>(); }
            else if (c != '\r')       cur.append(c);
        }
        if (cur.length() > 0 || !row.isEmpty()) { row.add(cur.toString()); rows.add(row); }
        return rows;
    }

    private static List<SheetCell> readCsvCells(File f, List<Integer> cols, int startRow) throws Exception {
        List<List<String>> rows = readCsvRows(f);
        List<SheetCell> out = new ArrayList<>();
        for (int r = startRow; r <= rows.size(); r++) {
            List<String> row = rows.get(r - 1);
            for (int col : cols) {
                if (col > row.size()) continue;
                String t = row.get(col - 1).trim();
                if (!t.isEmpty()) out.add(new SheetCell(r, col, t));
            }
        }
        return out;
    }

    private static void writeCsvCells(File f, List<SheetCell> writes) throws Exception {
        List<List<String>> rows = readCsvRows(f);
        for (SheetCell w : writes) {
            while (rows.size() < w.row) rows.add(new ArrayList<>());
            List<String> row = rows.get(w.row - 1);
            while (row.size() < w.col) row.add("");
            row.set(w.col - 1, w.text);
        }
        char sep = csvSep(f);
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) sb.append(sep);
                sb.append(csvEscape(row.get(i), sep));
            }
            sb.append('\n');
        }
        Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csvEscape(String v, char sep) {
        if (v.indexOf(sep) < 0 && v.indexOf('"') < 0 && v.indexOf('\n') < 0 && v.indexOf('\r') < 0)
            return v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    // ---- XLSX --------------------------------------------------------------

    private static List<SheetCell> readXlsxCells(File f, String sheetSel, List<Integer> cols, int startRow)
            throws Exception {
        Set<Integer> want = new HashSet<>(cols);
        try (ZipFile zip = new ZipFile(f)) {
            String path = sheetPath(zip, sheetSel);
            List<String> shared = readSharedStrings(zip);
            Document doc = parseXml(zipBytes(zip, path));
            List<SheetCell> out = new ArrayList<>();
            int rowNum = 0;
            for (Element row : byLocalName(doc.getDocumentElement(), "row")) {
                rowNum = attrInt(row, "r", rowNum + 1);
                if (rowNum < startRow) continue;
                int colNum = 0;
                for (Element c : childrenByLocalName(row, "c")) {
                    colNum = cellCol(c, colNum + 1);
                    if (!want.contains(colNum)) continue;
                    String t = cellText(c, shared);
                    if (t != null && !t.trim().isEmpty()) out.add(new SheetCell(rowNum, colNum, t.trim()));
                }
            }
            return out;
        }
    }

    private static void writeXlsxCells(File f, String sheetSel, List<SheetCell> writes)
            throws Exception {
        if (writes.isEmpty()) return;
        String path;
        Document doc;
        try (ZipFile zip = new ZipFile(f)) {
            path = sheetPath(zip, sheetSel);
            doc  = parseXml(zipBytes(zip, path));
        }
        Element sheetData = firstByLocalName(doc.getDocumentElement(), "sheetData");
        if (sheetData == null) throw new IllegalStateException("sheet has no <sheetData>");

        int maxCol = 0, maxRow = 0;
        for (SheetCell w : writes) {
            setCellText(doc, sheetData, w.row, w.col, w.text);
            maxCol = Math.max(maxCol, w.col);
            maxRow = Math.max(maxRow, w.row);
        }
        widenDimension(doc, maxCol, maxRow);
        replaceZipEntry(f, path, serializeXml(doc));
    }

    /** Put text into (row, col), creating the row / cell when Excel never stored one. */
    private static void setCellText(Document doc, Element sheetData, int rowNum, int col, String text) {
        Element row = null, rowBefore = null;
        int seen = 0;
        for (Element r : childrenByLocalName(sheetData, "row")) {
            seen = attrInt(r, "r", seen + 1);
            if (seen == rowNum) { row = r; break; }
            if (seen > rowNum && rowBefore == null) rowBefore = r;
        }
        if (row == null) {
            row = doc.createElement(sameNameStyle(sheetData, "row"));
            row.setAttribute("r", String.valueOf(rowNum));
            if (rowBefore != null) sheetData.insertBefore(row, rowBefore);
            else                   sheetData.appendChild(row);
        }

        Element cell = null, cellBefore = null;
        int colNum = 0;
        for (Element c : childrenByLocalName(row, "c")) {
            colNum = cellCol(c, colNum + 1);
            if (colNum == col) { cell = c; break; }
            if (colNum > col && cellBefore == null) cellBefore = c;
        }
        if (cell == null) {
            cell = doc.createElement(sameNameStyle(row, "c"));
            if (cellBefore != null) row.insertBefore(cell, cellBefore);
            else                    row.appendChild(cell);
        }
        cell.setAttribute("r", XlsxWriter.colLetter(col) + rowNum);
        while (cell.getFirstChild() != null) cell.removeChild(cell.getFirstChild());

        cell.setAttribute("t", "inlineStr");                  // self-contained: no sharedStrings edit
        Element is = doc.createElement(sameNameStyle(row, "is"));
        Element t  = doc.createElement(sameNameStyle(row, "t"));
        t.setAttribute("xml:space", "preserve");
        t.appendChild(doc.createTextNode(text));
        is.appendChild(t);
        cell.appendChild(is);
    }

    /** Grow <dimension ref="…"> so Excel still sees the whole used range. */
    private static void widenDimension(Document doc, int col, int lastRow) {
        Element dim = firstByLocalName(doc.getDocumentElement(), "dimension");
        if (dim == null) return;
        String ref = dim.getAttribute("ref");
        int colon = ref.indexOf(':');
        if (colon < 0) return;
        String start = ref.substring(0, colon), end = ref.substring(colon + 1);
        Matcher m = Pattern.compile("([A-Za-z]+)(\\d+)").matcher(end);
        if (!m.matches()) return;
        int endCol = Math.max(colIndex(m.group(1)), col);
        int endRow = Math.max(Integer.parseInt(m.group(2)), lastRow);
        dim.setAttribute("ref", start + ":" + XlsxWriter.colLetter(endCol) + endRow);
    }

    /** Locate xl/worksheets/sheetN.xml for a sheet name, 1-based number, or blank = first. */
    private static String sheetPath(ZipFile zip, String sel) throws Exception {
        Document wb   = parseXml(zipBytes(zip, "xl/workbook.xml"));
        Document rels = parseXml(zipBytes(zip, "xl/_rels/workbook.xml.rels"));
        Map<String, String> targets = new HashMap<>();
        for (Element rel : byLocalName(rels.getDocumentElement(), "Relationship"))
            targets.put(rel.getAttribute("Id"), rel.getAttribute("Target"));

        List<Element> sheets = byLocalName(wb.getDocumentElement(), "sheet");
        if (sheets.isEmpty()) throw new IllegalStateException("workbook has no sheets");

        Element chosen = null;
        if (sel.isEmpty()) chosen = sheets.get(0);
        else if (sel.chars().allMatch(Character::isDigit)) {
            int n = Integer.parseInt(sel);
            if (n < 1 || n > sheets.size())
                throw new IllegalArgumentException("sheet " + n + " does not exist (workbook has "
                        + sheets.size() + ")");
            chosen = sheets.get(n - 1);
        } else {
            for (Element s : sheets)
                if (s.getAttribute("name").equalsIgnoreCase(sel)) { chosen = s; break; }
            if (chosen == null) {
                List<String> names = new ArrayList<>();
                for (Element s : sheets) names.add(s.getAttribute("name"));
                throw new IllegalArgumentException("no sheet named \"" + sel + "\" — found: " + names);
            }
        }

        String rid = chosen.getAttribute("r:id");
        if (rid.isEmpty()) rid = chosen.getAttribute("id");
        String target = targets.get(rid);
        if (target == null) throw new IllegalStateException("cannot resolve sheet \""
                + chosen.getAttribute("name") + "\"");
        if (target.startsWith("/")) return target.substring(1);
        return target.startsWith("xl/") ? target : "xl/" + target;
    }

    private static List<String> readSharedStrings(ZipFile zip) throws Exception {
        List<String> out = new ArrayList<>();
        ZipEntry e = zip.getEntry("xl/sharedStrings.xml");
        if (e == null) return out;
        Document doc = parseXml(zipBytes(zip, "xl/sharedStrings.xml"));
        for (Element si : byLocalName(doc.getDocumentElement(), "si")) {
            StringBuilder sb = new StringBuilder();
            for (Element t : byLocalName(si, "t")) sb.append(textOf(t));
            out.add(sb.toString());
        }
        return out;
    }

    /** Value of one <c>, resolving shared / inline strings and tidying whole numbers. */
    private static String cellText(Element c, List<String> shared) {
        String type = c.getAttribute("t");
        if ("inlineStr".equals(type)) {
            StringBuilder sb = new StringBuilder();
            for (Element t : byLocalName(c, "t")) sb.append(textOf(t));
            return sb.toString();
        }
        Element v = firstByLocalName(c, "v");
        if (v == null) return "";
        String raw = textOf(v);
        if ("s".equals(type)) {
            try {
                int i = Integer.parseInt(raw.trim());
                return i >= 0 && i < shared.size() ? shared.get(i) : "";
            } catch (Exception e) { return ""; }
        }
        if ("b".equals(type)) return "1".equals(raw.trim()) ? "TRUE" : "FALSE";
        if (type.isEmpty() || "n".equals(type)) {
            try { return trim(Double.parseDouble(raw.trim())); } catch (Exception ignored) {}
        }
        return raw;
    }

    /** Column of a cell from its r="B7"; falls back to its position in the row. */
    private static int cellCol(Element c, int fallback) {
        String r = c.getAttribute("r");
        if (r.isEmpty()) return fallback;
        int i = 0;
        while (i < r.length() && Character.isLetter(r.charAt(i))) i++;
        if (i == 0) return fallback;
        try { return colIndex(r.substring(0, i)); } catch (Exception e) { return fallback; }
    }

    // ---- tiny XML / zip helpers -------------------------------------------

    private static byte[] zipBytes(ZipFile zip, String entry) throws Exception {
        ZipEntry e = zip.getEntry(entry);
        if (e == null) throw new IllegalStateException("not an Excel file (missing " + entry + ")");
        try (InputStream in = zip.getInputStream(e)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static Document parseXml(byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);   // keep prefixes verbatim so re-serialising is faithful
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private static byte[] serializeXml(Document doc) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        String out = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" + sw;
        return out.getBytes(StandardCharsets.UTF_8);
    }

    /** Rewrite one entry of a zip in place, copying everything else through. */
    private static void replaceZipEntry(File zipFile, String entryName, byte[] content) throws Exception {
        File tmp = File.createTempFile("elstudio", ".tmp", zipFile.getAbsoluteFile().getParentFile());
        boolean found = false;
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))) {
            byte[] buf = new byte[8192];
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                ZipEntry copy = new ZipEntry(e.getName());
                if (e.getTime() >= 0) copy.setTime(e.getTime());
                out.putNextEntry(copy);
                if (e.getName().equals(entryName)) { out.write(content); found = true; }
                else { int n; while ((n = in.read(buf)) > 0) out.write(buf, 0, n); }
                out.closeEntry();
            }
        } catch (Exception ex) {
            tmp.delete();
            throw ex;
        }
        if (!found) { tmp.delete(); throw new IllegalStateException("sheet " + entryName + " vanished"); }
        Files.move(tmp.toPath(), zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String localName(Node n) {
        String name = n.getNodeName();
        int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(colon + 1);
    }

    /** Same prefix as the reference element, so created tags match the file's style. */
    private static String sameNameStyle(Element reference, String local) {
        String name = reference.getNodeName();
        int colon = name.indexOf(':');
        return colon < 0 ? local : name.substring(0, colon + 1) + local;
    }

    /** All descendants with this local name, document order. */
    private static List<Element> byLocalName(Element root, String local) {
        List<Element> out = new ArrayList<>();
        NodeList all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n instanceof Element && localName(n).equals(local)) out.add((Element) n);
        }
        return out;
    }

    private static Element firstByLocalName(Element root, String local) {
        List<Element> l = byLocalName(root, local);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Direct children with this local name. */
    private static List<Element> childrenByLocalName(Element parent, String local) {
        List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element && localName(n).equals(local)) out.add((Element) n);
        return out;
    }

    private static String textOf(Element e) {
        String s = e.getTextContent();
        return s == null ? "" : s;
    }

    private static int attrInt(Element e, String name, int fallback) {
        String v = e.getAttribute(name);
        if (v == null || v.trim().isEmpty()) return fallback;
        try { return Integer.parseInt(v.trim()); } catch (Exception ex) { return fallback; }
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
        /**
         * The Excel-batch cell this line owns, or null for an ordinary line. Only
         * ▶ Listen and ↻ Regen look at it: when it is set they act on that file, so
         * the link already written into the sheet stays valid, and a regen fills the
         * link in if the sheet cell is still empty (the cell failed its batch).
         */
        BatchCell batch;
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