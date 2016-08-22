package org.olguin.moulinette;

import javafx.stage.FileChooser;
import org.olguin.moulinette.homework.Homework;
import org.olguin.moulinette.homework.HomeworkItem;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-15.
 * Part of org.olguin.moulinette.
 */
public class MoulinetteApplication extends JFrame {

    private MoulinetteServerManager serverManager;
    private JComboBox hwbox;
    private JComboBox itembox;
    private JLabel hwlabel;
    private JLabel itemlabel;
    private JTextArea textArea;
    private static String linebreak = System.getProperty("line.separator");
    private final JPanel mainPanel;
    private Map<String, String> tests;
    private File mainclass;
    private final JButton refresh;
    private final JButton pchoose;
    private final JButton prun;

    private MoulinetteApplication(int width, int height) {
        super("Moulinette");
        this.setSize(width, height);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        JPanel bpanel = new JPanel(new FlowLayout());
        refresh = new JButton("Refresh");
        pchoose = new JButton("Browse...");
        prun = new JButton("Run");

        refresh.addActionListener(e -> this.updateHomeworks());
        pchoose.addActionListener(e -> {
            mainclass = selectMainClass();
            pchoose.setText(mainclass.getName());
        });
        prun.addActionListener(e -> this.runProgram());
        bpanel.add(refresh);
        bpanel.add(pchoose);
        bpanel.add(prun);

        hwbox = new JComboBox();
        hwbox.addActionListener(e -> this.selectHomework());
        itembox = new JComboBox();
        itembox.addActionListener(e -> this.selectHomeworkItem());

        hwlabel = new JLabel();
        itemlabel = new JLabel();

        mainPanel.add(bpanel);
        mainPanel.add(hwbox);
        mainPanel.add(hwlabel);
        mainPanel.add(itembox);
        mainPanel.add(itemlabel);

        JPanel tpanel = new JPanel(new BorderLayout());
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        tpanel.add(textArea);
        mainPanel.add(tpanel);

        this.add(mainPanel);

        serverManager = new MoulinetteServerManager();
        this.setVisible(true);
        this.updateHomeworks();

    }

    private void updateHomeworks() {
        JDialog dialog = new JDialog(this, "Updating...", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JLabel("Updating homework assignments, please wait..."));
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

    private void selectHomework() {
        Homework selected = (Homework) hwbox.getSelectedItem();
        itembox.removeAllItems();
        if (selected != null) {
            for (HomeworkItem item : selected.getItems())
                if (item != null) itembox.addItem(item);
            hwlabel.setText("Description: " + selected.getDescription() + linebreak);
            hwlabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

    }

    private void selectHomeworkItem() {
        HomeworkItem selected = (HomeworkItem) itembox.getSelectedItem();
        if (selected != null) {
            itemlabel.setText("Description: " + selected.getDescription() + linebreak);
            itemlabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            tests = selected.getTests();
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

    private void runProgram()
    {
        if(mainclass == null)
        {
            JDialog errordialog = new JDialog(this, "Error", true);
            errordialog.add(new JLabel("Error: Please select a program to test."));
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
                ProgramRunner pr = new ProgramRunner(mainclass, "/usr/bin");
                pr.compile();
                for(String test: tests.keySet())
                {
                    String result = pr.run(tests.get(test), 3, TimeUnit.SECONDS);
                    boolean res = serverManager.validateTestOutput(test, result);
                    textArea.append((res ? "Correct" : "Incorrect") + linebreak);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ProgramRunner.ExecutionError executionError) {
                executionError.printStackTrace();
            } catch (ProgramRunner.ProgramNotCompiled programNotCompiled) {
                programNotCompiled.printStackTrace();
            }

            refresh.setEnabled(true);
            pchoose.setEnabled(true);
            prun.setEnabled(true);
            hwbox.setEnabled(true);
            itembox.setEnabled(true);

        });

        t.start();
    }

    public static void main(String[] args) {
        new MoulinetteApplication(800, 600);
    }
}
