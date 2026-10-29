import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

// ==========================================
// 1. DATA MODELS & PERSISTENCE
// ==========================================

class User {
    private final String username;
    private final String password;
    private final String role;      // "Admin" or "Student"
    private final String fullName;

    public User(String username, String password, String role, String fullName) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public boolean isAdmin() { return "Admin".equals(role); }
}

class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String subject;
    private String topic;
    private String difficulty;   // Easy / Medium / Hard
    private String text;
    private String[] options;    // always 4 options
    private int correctIndex;    // 0..3
    private int marks;

    public Question(String id, String subject, String topic, String difficulty, String text,
                    String[] options, int correctIndex, int marks) {
        this.id = id;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
        this.marks = marks;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getDifficulty() { return difficulty; }
    public String getText() { return text; }
    public String[] getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public int getMarks() { return marks; }
}

class Quiz implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String subject;
    private int durationMinutes;
    private List<String> questionIds;
    private String dateCreated;

    public Quiz(String id, String title, String subject, int durationMinutes, List<String> questionIds) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.durationMinutes = durationMinutes;
        this.questionIds = new ArrayList<>(questionIds);
        this.dateCreated = new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date());
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public int getDurationMinutes() { return durationMinutes; }
    public List<String> getQuestionIds() { return questionIds; }
    public String getDateCreated() { return dateCreated; }
    @Override public String toString() { return id + " - " + title; }
}

class QuizResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String studentName;
    private String quizTitle;
    private int correct;
    private int totalQuestions;
    private int score;
    private int totalMarks;
    private String dateTaken;

    public QuizResult(String id, String username, String studentName, String quizTitle,
                      int correct, int totalQuestions, int score, int totalMarks) {
        this.id = id;
        this.username = username;
        this.studentName = studentName;
        this.quizTitle = quizTitle;
        this.correct = correct;
        this.totalQuestions = totalQuestions;
        this.score = score;
        this.totalMarks = totalMarks;
        this.dateTaken = new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date());
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getStudentName() { return studentName; }
    public String getQuizTitle() { return quizTitle; }
    public int getCorrect() { return correct; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getScore() { return score; }
    public int getTotalMarks() { return totalMarks; }
    public String getDateTaken() { return dateTaken; }
    public double getPercentage() { return totalMarks > 0 ? score * 100.0 / totalMarks : 0; }
}

class StorageService {
    private static final String DATA_FILE = "quiz_data.dat";

    public static void saveData(Map<String, Question> questions, List<Quiz> quizzes, List<QuizResult> results) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(new LinkedHashMap<>(questions));
            oos.writeObject(new ArrayList<>(quizzes));
            oos.writeObject(new ArrayList<>(results));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadData(Map<String, Question> questions, List<Quiz> quizzes, List<QuizResult> results) {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            questions.putAll((Map<String, Question>) ois.readObject());
            quizzes.addAll((List<Quiz>) ois.readObject());
            results.addAll((List<QuizResult>) ois.readObject());
        } catch (Exception e) {
            System.out.println("No previous data found or file format mismatch.");
        }
    }

    // ---------- CSV helpers (question bank import / export, result export) ----------

    public static void writeCsv(File file, List<String[]> rows) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            for (String[] row : rows) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) sb.append(',');
                    String v = row[i] == null ? "" : row[i].replace("\r", " ").replace("\n", " ");
                    sb.append('"').append(v.replace("\"", "\"\"")).append('"');
                }
                pw.println(sb);
            }
        }
    }

    public static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else inQuotes = false;
                } else sb.append(c);
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { out.add(sb.toString()); sb.setLength(0); }
                else sb.append(c);
            }
        }
        out.add(sb.toString());
        return out;
    }

    public static void exportQuestionsCsv(Collection<Question> questions, File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"subject", "topic", "difficulty", "question", "optionA", "optionB",
                "optionC", "optionD", "correct(A-D)", "marks"});
        for (Question q : questions) {
            String[] o = q.getOptions();
            rows.add(new String[]{q.getSubject(), q.getTopic(), q.getDifficulty(), q.getText(),
                    o[0], o[1], o[2], o[3], String.valueOf((char) ('A' + q.getCorrectIndex())),
                    String.valueOf(q.getMarks())});
        }
        writeCsv(file, rows);
    }

    /** Returns parsed questions (ids are blank - the caller assigns them). Bad rows are skipped. */
    public static List<Question> importQuestionsCsv(File file) throws IOException {
        List<Question> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                List<String> c = parseCsvLine(line);
                if (first) {
                    first = false;
                    if (c.get(0).trim().equalsIgnoreCase("subject")) continue; // header row
                }
                if (c.size() < 9) continue;
                String letter = c.get(8).trim().toUpperCase();
                if (letter.length() != 1 || letter.charAt(0) < 'A' || letter.charAt(0) > 'D') continue;
                int marks = 1;
                if (c.size() > 9) {
                    try { marks = Math.max(1, Integer.parseInt(c.get(9).trim())); } catch (NumberFormatException ignored) { }
                }
                String diff = c.get(2).trim();
                if (!diff.equals("Easy") && !diff.equals("Medium") && !diff.equals("Hard")) diff = "Medium";
                list.add(new Question("", c.get(0).trim(), c.get(1).trim(), diff, c.get(3).trim(),
                        new String[]{c.get(4).trim(), c.get(5).trim(), c.get(6).trim(), c.get(7).trim()},
                        letter.charAt(0) - 'A', marks));
            }
        }
        return list;
    }
}

// ==========================================
// 2. MAIN APPLICATION & CONTROLLER
// ==========================================

public class QuizSystemApp {
    public static Map<String, Question> questionBank = new LinkedHashMap<>();
    public static List<Quiz> quizzes = new ArrayList<>();
    public static List<QuizResult> results = new ArrayList<>();
    public static Map<String, User> users = new LinkedHashMap<>();
    public static User currentUser;

    public static void main(String[] args) {
        // Built-in accounts (change these or load them from a file/database for real use)
        users.put("admin", new User("admin", "admin123", "Admin", "System Administrator"));
        users.put("student", new User("student", "student123", "Student", "Demo Student"));

        // Load saved state
        StorageService.loadData(questionBank, quizzes, results);

        // Preload sample data if empty
        if (questionBank.isEmpty()) seedSampleData();

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    public static void save() {
        StorageService.saveData(questionBank, quizzes, results);
    }

    public static String nextId(String prefix, Collection<String> ids) {
        int max = 0;
        for (String id : ids) {
            try { max = Math.max(max, Integer.parseInt(id.substring(prefix.length()))); }
            catch (Exception ignored) { }
        }
        return prefix + (max + 1);
    }

    public static List<Question> questionsOf(Quiz quiz) {
        List<Question> list = new ArrayList<>();
        for (String id : quiz.getQuestionIds()) {
            Question q = questionBank.get(id);
            if (q != null) list.add(q);
        }
        return list;
    }

    public static int totalMarks(Quiz quiz) {
        int sum = 0;
        for (Question q : questionsOf(quiz)) sum += q.getMarks();
        return sum;
    }

    public static List<String> subjects() {
        TreeSet<String> set = new TreeSet<>();
        for (Question q : questionBank.values()) set.add(q.getSubject());
        return new ArrayList<>(set);
    }

    private static void addSample(String subject, String topic, String diff, String text,
                                  String a, String b, String c, String d, int correct) {
        String id = nextId("Q", questionBank.keySet());
        questionBank.put(id, new Question(id, subject, topic, diff, text, new String[]{a, b, c, d}, correct, 1));
    }

    private static void seedSampleData() {
        addSample("Java", "OOP", "Easy", "Which keyword is used to inherit a class in Java?",
                "implements", "extends", "inherits", "super", 1);
        addSample("Java", "Collections", "Easy", "Which collection does not allow duplicate elements?",
                "ArrayList", "LinkedList", "HashSet", "Vector", 2);
        addSample("Java", "Basics", "Easy", "What is the default value of an int instance variable in Java?",
                "0", "null", "undefined", "-1", 0);
        addSample("Java", "OOP", "Medium", "What does the 'final' keyword on a method prevent?",
                "Overloading", "Overriding", "Calling it", "Inheriting the class", 1);
        addSample("DBMS", "SQL", "Medium", "Which SQL clause filters groups after GROUP BY?",
                "WHERE", "HAVING", "ORDER BY", "LIMIT", 1);
        addSample("DBMS", "Normalization", "Medium", "Which normal form removes transitive dependencies?",
                "1NF", "2NF", "3NF", "None of these", 2);
        addSample("DBMS", "Keys", "Easy", "Which key uniquely identifies each row in a table?",
                "Foreign key", "Primary key", "Alternate label", "Index key", 1);
        addSample("Networks", "OSI Model", "Medium", "Which OSI layer is responsible for routing packets?",
                "Data Link", "Network", "Transport", "Session", 1);
        addSample("Operating Systems", "Scheduling", "Medium", "Which scheduling algorithm can cause starvation?",
                "FCFS", "Round Robin", "Priority Scheduling", "None of these", 2);
        addSample("Data Structures", "Searching", "Easy", "What is the time complexity of binary search on a sorted array?",
                "O(n)", "O(log n)", "O(n log n)", "O(1)", 1);

        quizzes.add(new Quiz("QZ1", "Computer Science Fundamentals", "Mixed", 10,
                new ArrayList<>(questionBank.keySet())));
        save();
    }
}

// ==========================================
// 3. SHARED UI HELPERS
// ==========================================

class UI {
    static final Color PRIMARY = new Color(24, 119, 242);
    static final Color DARK = new Color(15, 75, 160);
    static final Color BG = new Color(245, 247, 250);

    static JButton button(String text, Color bg, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        if (al != null) b.addActionListener(al);
        return b;
    }

    static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static String grade(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B";
        if (pct >= 60) return "C";
        if (pct >= 50) return "D";
        return "F";
    }

    static JPanel header(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel l = new JLabel(title);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.add(l, BorderLayout.WEST);
        return header;
    }
}

// ==========================================
// 4. GRAPHICAL USER INTERFACE (SWING)
// ==========================================

// --- Login Window ---
class LoginFrame extends JFrame {
    private JComboBox<String> roleCombo;
    private JTextField userField;
    private JPasswordField passField;
    private JCheckBox showPass;

    public LoginFrame() {
        setTitle("Quiz System | LOG IN");
        setSize(380, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        mainPanel.setBackground(Color.WHITE);

        JLabel logo = new JLabel("Q?", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 64));
        logo.setForeground(UI.PRIMARY);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel appName = new JLabel("Quiz & Question Bank System");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLevelLabel = new JLabel("user level");
        userLevelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleCombo = new JComboBox<>(new String[]{"Admin", "Student"});
        roleCombo.setMaximumSize(new Dimension(300, 30));

        JLabel userLabel = new JLabel("username");
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userField = new JTextField();
        userField.setMaximumSize(new Dimension(300, 30));

        JLabel passLabel = new JLabel("password");
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passField = new JPasswordField();
        passField.setMaximumSize(new Dimension(300, 30));
        final char defaultEcho = passField.getEchoChar();

        showPass = new JCheckBox();
        showPass.setBackground(Color.WHITE);
        showPass.setToolTipText("Show password");
        showPass.addActionListener(e -> passField.setEchoChar(showPass.isSelected() ? (char) 0 : defaultEcho));

        JPanel passRow = new JPanel(new BorderLayout());
        passRow.setBackground(Color.WHITE);
        passRow.setMaximumSize(new Dimension(300, 30));
        passRow.add(passField, BorderLayout.CENTER);
        passRow.add(showPass, BorderLayout.EAST);

        JButton loginBtn = UI.button("LOG IN", new Color(0, 180, 0), e -> login());
        loginBtn.setMaximumSize(new Dimension(300, 35));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton clearBtn = new JButton("CLEAR");
        clearBtn.setBorderPainted(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> { userField.setText(""); passField.setText(""); });

        JLabel hint = new JLabel("<html><center><small>Demo: admin / admin123<br>student / student123</small></center></html>");
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(logo);
        mainPanel.add(appName);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(userLevelLabel);
        mainPanel.add(roleCombo);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(userLabel);
        mainPanel.add(userField);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(passLabel);
        mainPanel.add(passRow);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(loginBtn);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(clearBtn);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(hint);

        add(mainPanel, BorderLayout.CENTER);
        getRootPane().setDefaultButton(loginBtn);
    }

    private void login() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        String role = (String) roleCombo.getSelectedItem();
        User u = QuizSystemApp.users.get(username);
        if (u == null || !u.getPassword().equals(password) || !u.getRole().equals(role)) {
            JOptionPane.showMessageDialog(this, "Invalid username, password or user level.",
                    "Login failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        QuizSystemApp.currentUser = u;
        new MainMenuFrame(u).setVisible(true);
        dispose();
    }
}

// --- Main Dashboard ---
class MainMenuFrame extends JFrame {
    private final User user;

    public MainMenuFrame(User user) {
        this.user = user;
        setTitle("Quiz System | QUIZ & QUESTION BANK MANAGEMENT SYSTEM");
        setSize(900, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                new ExitDialog(MainMenuFrame.this).setVisible(true);
            }
        });
        setLayout(new BorderLayout());

        // Header Bar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UI.PRIMARY);
        header.setPreferredSize(new Dimension(900, 50));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("QUIZ & QUESTION BANK MANAGEMENT SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel navActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        navActions.setOpaque(false);
        JButton logoutBtn = new JButton("LOG OUT");
        logoutBtn.addActionListener(e -> {
            QuizSystemApp.currentUser = null;
            new LoginFrame().setVisible(true);
            dispose();
        });
        navActions.add(logoutBtn);
        header.add(title, BorderLayout.WEST);
        header.add(navActions, BorderLayout.EAST);

        // Center Tile Grid
        JPanel centerPanel;
        if (user.isAdmin()) {
            centerPanel = new JPanel(new GridLayout(2, 3, 20, 20));
            centerPanel.add(createTile("ADD NEW QUESTION", new Color(33, 150, 243),
                    e -> new QuestionDialog(this, null, null).setVisible(true)));
            centerPanel.add(createTile("QUESTION BANK", new Color(76, 175, 80),
                    e -> new QuestionBankFrame().setVisible(true)));
            centerPanel.add(createTile("CREATE QUIZ", new Color(255, 152, 0),
                    e -> new CreateQuizFrame().setVisible(true)));
            centerPanel.add(createTile("MANAGE QUIZZES", new Color(96, 125, 139),
                    e -> new QuizListFrame().setVisible(true)));
            centerPanel.add(createTile("TAKE QUIZ (PREVIEW)", new Color(244, 67, 54),
                    e -> startQuiz()));
            centerPanel.add(createTile("VIEW ALL RESULTS", new Color(156, 39, 176),
                    e -> new ResultsFrame(true).setVisible(true)));
        } else {
            centerPanel = new JPanel(new GridLayout(1, 2, 20, 20));
            centerPanel.add(createTile("TAKE A QUIZ", new Color(76, 175, 80), e -> startQuiz()));
            centerPanel.add(createTile("MY RESULTS", new Color(156, 39, 176),
                    e -> new ResultsFrame(false).setVisible(true)));
        }
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        centerPanel.setBackground(UI.BG);

        // Footer Bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UI.DARK);
        footer.setPreferredSize(new Dimension(900, 25));
        footer.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        JLabel info = new JLabel("Logged in As: " + user.getUsername() + " (" + user.getRole() + ")");
        info.setForeground(Color.WHITE);
        SimpleDateFormat clockFmt = new SimpleDateFormat("EEEE, hh:mm:ss a");
        JLabel dateLabel = new JLabel(clockFmt.format(new Date()));
        dateLabel.setForeground(Color.WHITE);
        new javax.swing.Timer(1000, e -> dateLabel.setText(clockFmt.format(new Date()))).start();
        footer.add(info, BorderLayout.WEST);
        footer.add(dateLabel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private JButton createTile(String title, Color bg, java.awt.event.ActionListener al) {
        JButton btn = new JButton("<html><center><h3>" + title + "</h3></center></html>");
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.addActionListener(al);
        return btn;
    }

    private void startQuiz() {
        List<Quiz> available = new ArrayList<>();
        for (Quiz q : QuizSystemApp.quizzes) {
            if (!QuizSystemApp.questionsOf(q).isEmpty()) available.add(q);
        }
        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No quizzes are available yet.");
            return;
        }
        JComboBox<Quiz> box = new JComboBox<>(available.toArray(new Quiz[0]));
        int ok = JOptionPane.showConfirmDialog(this, box, "Select a quiz to start",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            new QuizPlayerFrame((Quiz) box.getSelectedItem()).setVisible(true);
        }
    }
}

// --- Add / Edit Question Dialog ---
class QuestionDialog extends JDialog {
    private final JComboBox<String> subjectBox = new JComboBox<>();
    private final JTextField topicField = new JTextField();
    private final JComboBox<String> diffBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
    private final JTextArea textArea = new JTextArea(3, 30);
    private final JTextField[] optionFields = {new JTextField(), new JTextField(), new JTextField(), new JTextField()};
    private final JComboBox<String> correctBox = new JComboBox<>(new String[]{"A", "B", "C", "D"});
    private final JSpinner marksSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final Question existing;
    private final Runnable onSaved;

    public QuestionDialog(Window owner, Question existing, Runnable onSaved) {
        super(owner, existing == null ? "Add Question" : "Edit Question " + existing.getId(),
                Dialog.ModalityType.APPLICATION_MODAL);
        this.existing = existing;
        this.onSaved = onSaved;
        setSize(560, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        subjectBox.setEditable(true);
        for (String s : QuizSystemApp.subjects()) subjectBox.addItem(s);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        int r = 0;
        addRow(form, r++, "Subject:", subjectBox, 0);
        addRow(form, r++, "Topic:", topicField, 0);
        addRow(form, r++, "Difficulty:", diffBox, 0);
        addRow(form, r++, "Question:", new JScrollPane(textArea), 1);
        for (int i = 0; i < 4; i++) addRow(form, r++, "Option " + (char) ('A' + i) + ":", optionFields[i], 0);
        addRow(form, r++, "Correct Answer:", correctBox, 0);
        addRow(form, r++, "Marks:", marksSpinner, 0);

        JButton saveBtn = UI.button(existing == null ? "Add Record" : "Update Record", UI.PRIMARY, e -> save());
        saveBtn.setPreferredSize(new Dimension(100, 36));

        if (existing != null) {
            subjectBox.setSelectedItem(existing.getSubject());
            topicField.setText(existing.getTopic());
            diffBox.setSelectedItem(existing.getDifficulty());
            textArea.setText(existing.getText());
            for (int i = 0; i < 4; i++) optionFields[i].setText(existing.getOptions()[i]);
            correctBox.setSelectedIndex(existing.getCorrectIndex());
            marksSpinner.setValue(existing.getMarks());
        } else if (subjectBox.getItemCount() == 0) {
            subjectBox.setSelectedItem("");
        } else {
            subjectBox.setSelectedIndex(-1);
        }

        add(form, BorderLayout.CENTER);
        add(saveBtn, BorderLayout.SOUTH);
    }

    private void addRow(JPanel p, int row, String label, JComponent comp, double weightY) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.gridy = row;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        g.weightx = 1;
        g.weighty = weightY;
        g.fill = weightY > 0 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        p.add(comp, g);
    }

    private void save() {
        Object subjObj = subjectBox.getEditor().getItem();
        String subject = subjObj == null ? "" : subjObj.toString().trim();
        String text = textArea.getText().trim();
        String[] opts = new String[4];
        for (int i = 0; i < 4; i++) opts[i] = optionFields[i].getText().trim();

        if (subject.isEmpty() || text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Subject and question text are required.");
            return;
        }
        for (String o : opts) {
            if (o.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All four options must be filled in.");
                return;
            }
        }
        String id = existing != null ? existing.getId()
                : QuizSystemApp.nextId("Q", QuizSystemApp.questionBank.keySet());
        Question q = new Question(id, subject, topicField.getText().trim(), (String) diffBox.getSelectedItem(),
                text, opts, correctBox.getSelectedIndex(), (Integer) marksSpinner.getValue());
        QuizSystemApp.questionBank.put(id, q);
        QuizSystemApp.save();
        JOptionPane.showMessageDialog(this, existing == null ? "Question Added Successfully!" : "Question Updated!");
        if (onSaved != null) onSaved.run();
        dispose();
    }
}

// --- Question Bank (search / filter / edit / delete / import / export) ---
class QuestionBankFrame extends JFrame {
    private final DefaultTableModel model =
            UI.readOnlyModel("ID", "Subject", "Topic", "Difficulty", "Question", "Marks");
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> subjectBox = new JComboBox<>();
    private final JComboBox<String> diffBox = new JComboBox<>(new String[]{"All", "Easy", "Medium", "Hard"});
    private final JLabel countLabel = new JLabel();
    private boolean updating = false;

    public QuestionBankFrame() {
        setTitle("Quiz System | QUESTION BANK");
        setSize(950, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        top.add(new JLabel("Subject:"));
        top.add(subjectBox);
        top.add(new JLabel("Difficulty:"));
        top.add(diffBox);
        top.add(countLabel);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { reload(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { reload(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { reload(); }
        });
        subjectBox.addActionListener(e -> { if (!updating) reload(); });
        diffBox.addActionListener(e -> reload());

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setPreferredWidth(400);
        table.getColumnModel().getColumn(5).setPreferredWidth(50);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        bottom.add(UI.button("Add", new Color(33, 150, 243),
                e -> new QuestionDialog(this, null, this::refreshAll).setVisible(true)));
        bottom.add(UI.button("Edit", new Color(255, 152, 0), e -> editSelected()));
        bottom.add(UI.button("Delete", new Color(244, 67, 54), e -> deleteSelected()));
        bottom.add(UI.button("Import CSV", new Color(96, 125, 139), e -> importCsv()));
        bottom.add(UI.button("Export CSV", new Color(156, 39, 176), e -> exportCsv()));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        refreshAll();
    }

    private void refreshAll() {
        updating = true;
        Object selected = subjectBox.getSelectedItem();
        subjectBox.removeAllItems();
        subjectBox.addItem("All");
        for (String s : QuizSystemApp.subjects()) subjectBox.addItem(s);
        subjectBox.setSelectedItem(selected != null ? selected : "All");
        if (subjectBox.getSelectedItem() == null) subjectBox.setSelectedIndex(0);
        updating = false;
        reload();
    }

    private void reload() {
        model.setRowCount(0);
        String q = searchField.getText().trim().toLowerCase();
        String sub = (String) subjectBox.getSelectedItem();
        String diff = (String) diffBox.getSelectedItem();
        for (Question qu : QuizSystemApp.questionBank.values()) {
            if (sub != null && !sub.equals("All") && !qu.getSubject().equals(sub)) continue;
            if (diff != null && !diff.equals("All") && !qu.getDifficulty().equals(diff)) continue;
            if (!q.isEmpty() && !(qu.getText().toLowerCase().contains(q)
                    || qu.getTopic().toLowerCase().contains(q)
                    || qu.getId().toLowerCase().contains(q))) continue;
            model.addRow(new Object[]{qu.getId(), qu.getSubject(), qu.getTopic(), qu.getDifficulty(),
                    qu.getText(), qu.getMarks()});
        }
        countLabel.setText("   " + model.getRowCount() + " question(s)");
    }

    private Question selectedQuestion() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a question first.");
            return null;
        }
        String id = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
        return QuizSystemApp.questionBank.get(id);
    }

    private void editSelected() {
        Question q = selectedQuestion();
        if (q != null) new QuestionDialog(this, q, this::refreshAll).setVisible(true);
    }

    private void deleteSelected() {
        Question q = selectedQuestion();
        if (q == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Delete question " + q.getId() + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            QuizSystemApp.questionBank.remove(q.getId());
            QuizSystemApp.save();
            refreshAll();
        }
    }

    private void importCsv() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            List<Question> imported = StorageService.importQuestionsCsv(fc.getSelectedFile());
            for (Question q : imported) {
                q.setId(QuizSystemApp.nextId("Q", QuizSystemApp.questionBank.keySet()));
                QuizSystemApp.questionBank.put(q.getId(), q);
            }
            QuizSystemApp.save();
            refreshAll();
            JOptionPane.showMessageDialog(this, imported.size() + " question(s) imported.\n"
                    + "Expected columns: subject, topic, difficulty, question, optionA-D, correct(A-D), marks");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read file: " + ex.getMessage());
        }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("question_bank.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            StorageService.exportQuestionsCsv(QuizSystemApp.questionBank.values(), fc.getSelectedFile());
            JOptionPane.showMessageDialog(this, "Question bank exported.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not write file: " + ex.getMessage());
        }
    }
}

// --- Create Quiz from the bank ---
class CreateQuizFrame extends JFrame {
    private final JTextField titleField = new JTextField(20);
    private final JComboBox<String> subjectBox = new JComboBox<>();
    private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 180, 1));
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final DefaultTableModel model = UI.readOnlyModel("ID", "Subject", "Difficulty", "Question", "Marks");
    private final JTable table = new JTable(model);
    private boolean updating = false;

    public CreateQuizFrame() {
        setTitle("Quiz System | CREATE QUIZ");
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("Quiz Title:"));
        top.add(titleField);
        top.add(new JLabel("Subject:"));
        top.add(subjectBox);
        top.add(new JLabel("Duration (min):"));
        top.add(durationSpinner);

        updating = true;
        subjectBox.addItem("All");
        for (String s : QuizSystemApp.subjects()) subjectBox.addItem(s);
        updating = false;
        subjectBox.addActionListener(e -> { if (!updating) reload(); });

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(400);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        bottom.add(UI.button("Select All", new Color(96, 125, 139), e -> table.selectAll()));
        bottom.add(UI.button("Clear Selection", new Color(96, 125, 139), e -> table.clearSelection()));
        bottom.add(new JLabel("Random count:"));
        bottom.add(countSpinner);
        bottom.add(UI.button("Pick Random", new Color(255, 152, 0), e -> pickRandom()));
        bottom.add(UI.button("Create Quiz", new Color(76, 175, 80), e -> createQuiz()));

        JLabel tip = new JLabel("  Tip: Ctrl+click or Shift+click to choose questions manually, or use Pick Random.");
        tip.setForeground(Color.GRAY);

        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.NORTH);
        north.add(tip, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        reload();
    }

    private void reload() {
        model.setRowCount(0);
        String sub = (String) subjectBox.getSelectedItem();
        for (Question q : QuizSystemApp.questionBank.values()) {
            if (sub != null && !sub.equals("All") && !q.getSubject().equals(sub)) continue;
            model.addRow(new Object[]{q.getId(), q.getSubject(), q.getDifficulty(), q.getText(), q.getMarks()});
        }
    }

    private void pickRandom() {
        int rows = model.getRowCount();
        if (rows == 0) return;
        int n = Math.min((Integer) countSpinner.getValue(), rows);
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < rows; i++) idx.add(i);
        Collections.shuffle(idx);
        table.clearSelection();
        for (int i = 0; i < n; i++) table.addRowSelectionInterval(idx.get(i), idx.get(i));
    }

    private void createQuiz() {
        String title = titleField.getText().trim();
        int[] rows = table.getSelectedRows();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a quiz title.");
            return;
        }
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select at least one question.");
            return;
        }
        List<String> ids = new ArrayList<>();
        Set<String> subjects = new TreeSet<>();
        for (int r : rows) {
            String id = (String) model.getValueAt(table.convertRowIndexToModel(r), 0);
            ids.add(id);
            subjects.add((String) model.getValueAt(table.convertRowIndexToModel(r), 1));
        }
        List<String> quizIds = new ArrayList<>();
        for (Quiz q : QuizSystemApp.quizzes) quizIds.add(q.getId());
        String quizId = QuizSystemApp.nextId("QZ", quizIds);
        String subject = subjects.size() == 1 ? subjects.iterator().next() : "Mixed";
        QuizSystemApp.quizzes.add(new Quiz(quizId, title, subject, (Integer) durationSpinner.getValue(), ids));
        QuizSystemApp.save();
        JOptionPane.showMessageDialog(this, "Quiz '" + title + "' created with " + ids.size() + " question(s).");
        dispose();
    }
}

// --- Manage existing quizzes ---
class QuizListFrame extends JFrame {
    private final DefaultTableModel model =
            UI.readOnlyModel("ID", "Title", "Subject", "Questions", "Total Marks", "Duration (min)", "Created");
    private final JTable table = new JTable(model);

    public QuizListFrame() {
        setTitle("Quiz System | MANAGE QUIZZES");
        setSize(850, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bottom = new JPanel();
        bottom.add(UI.button("Delete Quiz", new Color(244, 67, 54), e -> deleteSelected()));
        add(UI.header("QUIZZES"), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        reload();
    }

    private void reload() {
        model.setRowCount(0);
        for (Quiz q : QuizSystemApp.quizzes) {
            model.addRow(new Object[]{q.getId(), q.getTitle(), q.getSubject(),
                    QuizSystemApp.questionsOf(q).size(), QuizSystemApp.totalMarks(q),
                    q.getDurationMinutes(), q.getDateCreated()});
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a quiz first.");
            return;
        }
        String id = (String) model.getValueAt(row, 0);
        int ok = JOptionPane.showConfirmDialog(this, "Delete quiz " + id + "? Past results are kept.",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            QuizSystemApp.quizzes.removeIf(q -> q.getId().equals(id));
            QuizSystemApp.save();
            reload();
        }
    }
}

// --- Quiz Player (timer, navigation, auto-grading) ---
class QuizPlayerFrame extends JFrame {
    private final Quiz quiz;
    private final List<Question> questions;
    private final int[] answers;
    private int current = 0;
    private int remainingSeconds;
    private final javax.swing.Timer timer;

    private final JLabel timerLabel = new JLabel();
    private final JLabel progressLabel = new JLabel();
    private final JTextArea questionArea = new JTextArea();
    private final JRadioButton[] optionButtons = new JRadioButton[4];
    private final ButtonGroup group = new ButtonGroup();
    private final JButton prevBtn = new JButton("< Previous");
    private final JButton nextBtn = new JButton("Next >");
    private boolean finished = false;

    public QuizPlayerFrame(Quiz quiz) {
        this.quiz = quiz;
        this.questions = new ArrayList<>(QuizSystemApp.questionsOf(quiz));
        Collections.shuffle(questions);
        this.answers = new int[questions.size()];
        Arrays.fill(answers, -1);
        this.remainingSeconds = quiz.getDurationMinutes() * 60;

        setTitle("Quiz System | " + quiz.getTitle());
        setSize(720, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                int ok = JOptionPane.showConfirmDialog(QuizPlayerFrame.this,
                        "Quit without submitting? Your answers will be lost.", "Quit quiz",
                        JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) { timer.stop(); dispose(); }
            }
        });
        setLayout(new BorderLayout());

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UI.PRIMARY);
        top.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel(quiz.getTitle());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        progressLabel.setForeground(Color.WHITE);
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        right.setOpaque(false);
        right.add(progressLabel);
        right.add(timerLabel);
        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        // Question + options
        questionArea.setEditable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setFont(new Font("Segoe UI", Font.BOLD, 17));
        questionArea.setBackground(getBackground());
        questionArea.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        for (int i = 0; i < 4; i++) {
            final int choice = i;
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Segoe UI", Font.PLAIN, 15));
            optionButtons[i].setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
            optionButtons[i].addActionListener(e -> answers[current] = choice);
            group.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        center.add(questionArea, BorderLayout.NORTH);
        center.add(optionsPanel, BorderLayout.CENTER);

        // Bottom navigation
        prevBtn.addActionListener(e -> { current--; showQuestion(); });
        nextBtn.addActionListener(e -> { current++; showQuestion(); });
        JButton submitBtn = UI.button("SUBMIT QUIZ", new Color(0, 160, 0), e -> confirmSubmit());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottom.add(prevBtn);
        bottom.add(nextBtn);
        bottom.add(submitBtn);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        timer = new javax.swing.Timer(1000, e -> tick());
        updateTimerLabel();
        showQuestion();
        timer.start();
    }

    private void tick() {
        remainingSeconds--;
        updateTimerLabel();
        if (remainingSeconds <= 0) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Time is up! Your quiz will be submitted now.");
            finish();
        }
    }

    private void updateTimerLabel() {
        int s = Math.max(0, remainingSeconds);
        timerLabel.setText(String.format("%02d:%02d", s / 60, s % 60));
        timerLabel.setForeground(s <= 60 ? new Color(255, 200, 200) : Color.WHITE);
    }

    private void showQuestion() {
        Question q = questions.get(current);
        progressLabel.setText("Question " + (current + 1) + " of " + questions.size()
                + "   (" + q.getMarks() + (q.getMarks() == 1 ? " mark)" : " marks)"));
        questionArea.setText(q.getText());
        for (int i = 0; i < 4; i++) optionButtons[i].setText((char) ('A' + i) + ".  " + q.getOptions()[i]);
        group.clearSelection();
        if (answers[current] >= 0) optionButtons[answers[current]].setSelected(true);
        prevBtn.setEnabled(current > 0);
        nextBtn.setEnabled(current < questions.size() - 1);
    }

    private void confirmSubmit() {
        int unanswered = 0;
        for (int a : answers) if (a < 0) unanswered++;
        String msg = unanswered > 0
                ? "You have " + unanswered + " unanswered question(s). Submit anyway?"
                : "Submit your quiz now?";
        if (JOptionPane.showConfirmDialog(this, msg, "Submit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            finish();
        }
    }

    private void finish() {
        if (finished) return;
        finished = true;
        timer.stop();

        int correct = 0, score = 0, total = 0;
        StringBuilder review = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            total += q.getMarks();
            boolean ok = answers[i] == q.getCorrectIndex();
            if (ok) { correct++; score += q.getMarks(); }
            review.append(i + 1).append(". ").append(q.getText()).append('\n');
            review.append("   Your answer   : ")
                    .append(answers[i] < 0 ? "(not answered)" : q.getOptions()[answers[i]]).append('\n');
            review.append("   Correct answer: ").append(q.getOptions()[q.getCorrectIndex()])
                    .append(ok ? "   [Correct]" : "   [Wrong]").append("\n\n");
        }

        User u = QuizSystemApp.currentUser;
        List<String> resultIds = new ArrayList<>();
        for (QuizResult r : QuizSystemApp.results) resultIds.add(r.getId());
        QuizResult result = new QuizResult(QuizSystemApp.nextId("R", resultIds),
                u.getUsername(), u.getFullName(), quiz.getTitle(), correct, questions.size(), score, total);
        QuizSystemApp.results.add(result);
        QuizSystemApp.save();

        String summary = String.format("Quiz: %s%nScore: %d / %d   (%.1f%%)   Grade: %s%nCorrect answers: %d of %d%n%n",
                quiz.getTitle(), score, total, result.getPercentage(), UI.grade(result.getPercentage()),
                correct, questions.size());
        JTextArea area = new JTextArea(summary + "---------- ANSWER REVIEW ----------\n\n" + review);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(560, 380));
        JOptionPane.showMessageDialog(this, sp, "Quiz Result", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}

// --- Results viewer ---
class ResultsFrame extends JFrame {
    private final boolean allUsers;
    private final DefaultTableModel model =
            UI.readOnlyModel("ID", "Student", "Quiz", "Correct", "Score", "Percent", "Grade", "Date");
    private final JTable table = new JTable(model);
    private final JLabel statsLabel = new JLabel();

    public ResultsFrame(boolean allUsers) {
        this.allUsers = allUsers;
        setTitle(allUsers ? "Quiz System | ALL RESULTS" : "Quiz System | MY RESULTS");
        setSize(900, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bottom = new JPanel(new BorderLayout());
        statsLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        bottom.add(statsLabel, BorderLayout.NORTH);
        if (allUsers) {
            JPanel btns = new JPanel();
            btns.add(UI.button("Delete Selected", new Color(244, 67, 54), e -> deleteSelected()));
            btns.add(UI.button("Export CSV", new Color(156, 39, 176), e -> exportCsv()));
            bottom.add(btns, BorderLayout.SOUTH);
        }

        add(UI.header(allUsers ? "ALL QUIZ RESULTS" : "MY QUIZ RESULTS"), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        reload();
    }

    private void reload() {
        model.setRowCount(0);
        int count = 0;
        double sum = 0, best = 0;
        String me = QuizSystemApp.currentUser.getUsername();
        for (QuizResult r : QuizSystemApp.results) {
            if (!allUsers && !r.getUsername().equals(me)) continue;
            model.addRow(new Object[]{r.getId(), r.getStudentName(), r.getQuizTitle(),
                    r.getCorrect() + "/" + r.getTotalQuestions(), r.getScore() + "/" + r.getTotalMarks(),
                    String.format("%.1f%%", r.getPercentage()), UI.grade(r.getPercentage()), r.getDateTaken()});
            count++;
            sum += r.getPercentage();
            best = Math.max(best, r.getPercentage());
        }
        statsLabel.setText(count == 0 ? "No attempts recorded yet."
                : String.format("Attempts: %d    Average: %.1f%%    Highest: %.1f%%", count, sum / count, best));
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a result first.");
            return;
        }
        String id = (String) model.getValueAt(row, 0);
        QuizSystemApp.results.removeIf(r -> r.getId().equals(id));
        QuizSystemApp.save();
        reload();
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("quiz_results.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"id", "student", "quiz", "correct", "totalQuestions", "score", "totalMarks", "percent", "date"});
        for (QuizResult r : QuizSystemApp.results) {
            rows.add(new String[]{r.getId(), r.getStudentName(), r.getQuizTitle(),
                    String.valueOf(r.getCorrect()), String.valueOf(r.getTotalQuestions()),
                    String.valueOf(r.getScore()), String.valueOf(r.getTotalMarks()),
                    String.format("%.1f", r.getPercentage()), r.getDateTaken()});
        }
        try {
            StorageService.writeCsv(fc.getSelectedFile(), rows);
            JOptionPane.showMessageDialog(this, "Results exported.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not write file: " + ex.getMessage());
        }
    }
}

// --- System Exit Options Dialog ---
class ExitDialog extends JDialog {
    public ExitDialog(JFrame parent) {
        super(parent, "Quiz System | EXIT", true);
        setSize(650, 220);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JLabel header = new JLabel("Please choose action:", SwingConstants.LEFT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(UI.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel choices = new JPanel(new GridLayout(1, 3, 15, 15));
        choices.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        JButton fullExit = new JButton("<html><center><b>Full Exit<br>Quiz Mgmt.<br>System</b></center></html>");
        fullExit.addActionListener(e -> System.exit(0));

        JButton minimize = new JButton("<html><center><b>Minimize<br>Program Window</b></center></html>");
        minimize.addActionListener(e -> {
            parent.setState(Frame.ICONIFIED);
            dispose();
        });

        JButton logout = new JButton("<html><center><b>Logout</b></center></html>");
        logout.addActionListener(e -> {
            QuizSystemApp.currentUser = null;
            new LoginFrame().setVisible(true);
            parent.dispose();
            dispose();
        });

        choices.add(fullExit);
        choices.add(minimize);
        choices.add(logout);

        add(header, BorderLayout.NORTH);
        add(choices, BorderLayout.CENTER);
    }
}
