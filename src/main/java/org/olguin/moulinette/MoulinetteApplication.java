package org.olguin.moulinette;

import com.github.kevinsawicki.http.HttpRequest;
import org.olguin.moulinette.homework.Homework;
import org.olguin.moulinette.homework.HomeworkItem;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-15.
 * Part of org.olguin.moulinette.
 */
public class MoulinetteApplication extends JFrame {

    private static String linebreak = System.getProperty("line.separator");
    private final JButton refresh;
    private final JButton pchoose;
    private final JButton prun;
    private MoulinetteServerManager serverManager;
    private JComboBox hwbox;
    private JComboBox itembox;
    private Map<String, String> tests;
    private File mainclass;
    private StyledDocument doc;
    private SimpleAttributeSet errorstyle;
    private SimpleAttributeSet correctstyle;
    private SimpleAttributeSet infostyle;

    private JTextArea hwdescription;
    private JTextArea itemdescription;

    private String java_home;


    private MoulinetteApplication(int width, int height, Properties prop) {
        super(prop.getProperty("name") + " " + prop.getProperty("version"));

        java_home = System.getenv("JAVA_HOME");

        this.setSize(width, height);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));

        JPanel bpanel = new JPanel(new FlowLayout());
        refresh = new JButton("Refresh");
        pchoose = new JButton("Browse...");
        prun = new JButton("Run");

        refresh.addActionListener(e -> this.updateHomeworks());
        pchoose.addActionListener(e -> {
            mainclass = selectMainClass();
            if (mainclass != null)
                pchoose.setText(mainclass.getName());
            else
                pchoose.setText("Browse...");
        });
        prun.addActionListener(e -> this.runProgram());
        bpanel.add(refresh);
        bpanel.add(pchoose);
        bpanel.add(prun);

        hwbox = new JComboBox();
        hwbox.addActionListener(e -> this.selectHomework());
        JLabel hwboxlabel = new JLabel("Homework: ");
        hwboxlabel.setPreferredSize(new Dimension(110, 10));

        itembox = new JComboBox();
        itembox.addActionListener(e -> this.selectHomeworkItem());
        JLabel itemboxlabel = new JLabel("Item: ");
        itemboxlabel.setPreferredSize(new Dimension(110, 10));


        JPanel hboxpanel = new JPanel();
        hboxpanel.setLayout(new BoxLayout(hboxpanel, BoxLayout.X_AXIS));
        hboxpanel.add(hwboxlabel);
        hboxpanel.add(hwbox);

        JPanel itemboxpanel = new JPanel();
        itemboxpanel.setLayout(new BoxLayout(itemboxpanel, BoxLayout.X_AXIS));
        itemboxpanel.add(itemboxlabel);
        itemboxpanel.add(itembox);

        JLabel hwlabel = new JLabel("Description: ");
        JLabel itemlabel = new JLabel("Description: ");
        hwlabel.setPreferredSize(new Dimension(110, 10));
        itemlabel.setPreferredSize(new Dimension(110, 10));

        JPanel hwlabelpanel = new JPanel();
        hwlabelpanel.setLayout(new BoxLayout(hwlabelpanel, BoxLayout.X_AXIS));
        hwlabelpanel.setBorder(new EmptyBorder(new Insets(5, 0, 5, 0)));

        JPanel itemlabelpanel = new JPanel();
        itemlabelpanel.setLayout(new BoxLayout(itemlabelpanel, BoxLayout.X_AXIS));
        itemlabelpanel.setBorder(new EmptyBorder(new Insets(5, 0, 5, 0)));

        hwlabelpanel.add(hwlabel);
        itemlabelpanel.add(itemlabel);

        hwdescription = new JTextArea(3, 50);
        hwdescription.setEditable(false);
        hwdescription.setLineWrap(true);
        JScrollPane hwscroll = new JScrollPane(hwdescription);
        itemdescription = new JTextArea(3, 50);
        itemdescription.setEditable(false);
        itemdescription.setLineWrap(true);
        JScrollPane itemscroll = new JScrollPane(itemdescription);

        hwlabelpanel.add(hwscroll);
        itemlabelpanel.add(itemscroll);

        topPanel.add(bpanel);

        JPanel boxpanel = new JPanel();
        boxpanel.setLayout(new BoxLayout(boxpanel, BoxLayout.Y_AXIS));

        boxpanel.add(hboxpanel);
        boxpanel.add(hwlabelpanel);
        boxpanel.add(itemboxpanel);
        boxpanel.add(itemlabelpanel);

        topPanel.add(boxpanel);


        JPanel tpanel = new JPanel(new BorderLayout());
        JPanel auxpanel = new JPanel(new BorderLayout());
        auxpanel.setBorder(new EmptyBorder(new Insets(10, 0, 0, 0)));
        tpanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        textPane.setMargin(new Insets(1, 1, 1, 1));
        doc = textPane.getStyledDocument();
        JScrollPane textscroll = new JScrollPane(textPane);
        textscroll.setAutoscrolls(true);

        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        tpanel.add(textscroll);
        auxpanel.add(tpanel);

        errorstyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorstyle, Color.RED);
        StyleConstants.setBold(errorstyle, true);

        correctstyle = new SimpleAttributeSet();
        StyleConstants.setForeground(correctstyle, Color.GREEN);
        StyleConstants.setBold(correctstyle, true);

        infostyle = new SimpleAttributeSet();
        StyleConstants.setBold(infostyle, true);


        mainPanel.add(topPanel);
        mainPanel.add(auxpanel);
        this.add(mainPanel);

        serverManager = new MoulinetteServerManager();
        this.setVisible(true);
        this.updateHomeworks();

    }

    private void updateHomeworks() {
        JDialog dialog = new JDialog(this, "Updating...", true);
        dialog.setLayout(new BorderLayout());
        JPanel dpanel = new JPanel(new BorderLayout());
        dpanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        dpanel.add(new JLabel("Updating homework assignments, please wait..."));
        dialog.add(dpanel);
        dialog.pack();
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        Thread dt = new Thread(() ->
        {
            dialog.setLocationRelativeTo(this);
            serverManager.updateHomeworks();
            java.util.List<Homework> homeworks = serverManager.getHomeworks();
            hwbox.removeAllItems();
            for (Homework homework : homeworks)
                if (homework != null) hwbox.addItem(homework);

            dialog.setVisible(false);
        });
        dt.start();
        dialog.setVisible(true);

        try {
            dt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private File selectMainClass() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String filename = f.getName().toLowerCase();
                    return filename.endsWith(".java");
                }
            }

            @Override
            public String getDescription() {
                return "Java source files (.java)";
            }
        });

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }

    private void runProgram() {
        if (mainclass == null) {
            JDialog errordialog = new JDialog(this, "Error", true);
            JPanel dpanel = new JPanel(new BorderLayout());
            dpanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
            errordialog.add(dpanel);
            dpanel.add(new JLabel("Error: Please select a program to test."));
            errordialog.pack();
            errordialog.setLocationRelativeTo(this);
            errordialog.setVisible(true);
            return;
        }

        refresh.setEnabled(false);
        pchoose.setEnabled(false);
        prun.setEnabled(false);
        hwbox.setEnabled(false);
        itembox.setEnabled(false);

        Thread t = new Thread(() -> {

            try {
                String ISOnow = LocalDateTime.now().toLocalTime().toString();
                doc.insertString(doc.getLength(), "[" + ISOnow + "] ", infostyle);
                doc.insertString(doc.getLength(), "Evaluating " + mainclass.getName() + linebreak, infostyle);
                ProgramRunner pr = new ProgramRunner(mainclass, java_home + "/bin");
                doc.insertString(doc.getLength(), "Compiling... ", null);
                pr.compile();
                doc.insertString(doc.getLength(), "Done." + linebreak, null);
                doc.insertString(doc.getLength(), "Verifying results..." + linebreak, null);
                int testcnt = 1;
                for (String test : tests.keySet()) {
                    doc.insertString(doc.getLength(), "Test " + testcnt + "...\t", null);
                    String result = pr.run(tests.get(test), 3, TimeUnit.SECONDS);

                    try {
                        serverManager.validateTestOutput(test, result);
                        doc.insertString(doc.getLength(), "Correct ✓" + linebreak, correctstyle);
                    } catch (MoulinetteServerManager.WrongResult wrongResult) {
                        doc.insertString(doc.getLength(), "Incorrect ✗: " + wrongResult.error + linebreak, errorstyle);
                    } catch (HttpRequest.HttpRequestException e) {
                        doc.insertString(doc.getLength(), linebreak + "Error when contacting server. Please retry or refresh the application." + linebreak, errorstyle);
                        return;
                    }
                    testcnt++;
                }
            } catch (IOException | InterruptedException | ProgramRunner.ProgramNotCompiled | BadLocationException e) {
                e.printStackTrace();
            } catch (ProgramRunner.ExecutionError executionError) {
                try {
                    doc.insertString(doc.getLength(), linebreak + "Error when executing " + mainclass.getName() + linebreak, errorstyle);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            } catch (ProgramRunner.CompileError compileError) {
                try {
                    doc.insertString(doc.getLength(), linebreak + compileError.stderr + linebreak, errorstyle);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

            refresh.setEnabled(true);
            pchoose.setEnabled(true);
            prun.setEnabled(true);
            hwbox.setEnabled(true);
            itembox.setEnabled(true);

        });

        t.start();
    }

    private void selectHomework() {
        Homework selected = (Homework) hwbox.getSelectedItem();
        itembox.removeAllItems();
        if (selected != null) {
            for (HomeworkItem item : selected.getItems())
                if (item != null) itembox.addItem(item);
            hwdescription.setText(selected.getDescription() + linebreak);
        }

    }

    private void selectHomeworkItem() {
        HomeworkItem selected = (HomeworkItem) itembox.getSelectedItem();
        if (selected != null) {
            itemdescription.setText(selected.getDescription() + linebreak);
            tests = selected.getTests();
        }
    }

    public static void main(String[] args) throws IOException {

        InputStream resourceAsStream =
                MoulinetteApplication.class.getResourceAsStream(
                        "/version.properties"
                );
        Properties prop = new Properties();
        prop.load(resourceAsStream);

        new MoulinetteApplication(800, 600, prop);
    }
}
