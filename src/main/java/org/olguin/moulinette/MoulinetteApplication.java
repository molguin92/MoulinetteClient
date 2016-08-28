package org.olguin.moulinette;

import org.json.JSONArray;
import org.json.JSONObject;
import org.olguin.moulinette.homework.Homework;
import org.olguin.moulinette.homework.HomeworkItem;
import org.olguin.moulinette.homework.HomeworkTest;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-15.
 * Part of org.olguin.moulinette.
 */
@SuppressWarnings("Convert2Diamond")
public class MoulinetteApplication extends JFrame
{

    private static String linebreak = System.getProperty("line.separator");
    private final JButton refresh;
    private final JButton pchoose;
    private final JButton prun;
    private MoulinetteServerManager serverManager;
    private JComboBox<Homework> hwbox;
    private JComboBox<HomeworkItem> itembox;
    private Map<String, HomeworkTest> tests;
    private File mainclass;
    private StyledDocument doc;
    private SimpleAttributeSet errorstyle;
    private SimpleAttributeSet correctstyle;
    private SimpleAttributeSet infostyle;

    private JTextArea hwdescription;
    private JTextArea itemdescription;

    private String java_home;
    private final JTextField pfield;


    private MoulinetteApplication(int width, int height, Properties prop)
    {
        super(prop.getProperty("name") + " " + prop.getProperty("version"));

        java_home = getJavaHomeEnv();

        this.setSize(width, height);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));

        refresh = new JButton("Refresh");
        pchoose = new JButton("Browse...");
        prun = new JButton("Run");

        JPanel ppanel = new JPanel();
        ppanel.setLayout(new BoxLayout(ppanel, BoxLayout.X_AXIS));
        ppanel.setBorder(new EmptyBorder(new Insets(5, 0, 10, 0)));
        JLabel plabel = new JLabel("Main class: ");
        plabel.setPreferredSize(new Dimension(110, 10));
        pfield = new JTextField();
        pfield.setMaximumSize(new Dimension(Integer.MAX_VALUE, pchoose.getPreferredSize().height));
        pfield.setEditable(false);

        ((DefaultCaret) pfield.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JPanel pfieldpanel = new JPanel();
        pfieldpanel.setLayout(new BoxLayout(pfieldpanel, BoxLayout.X_AXIS));
        pfieldpanel.setBorder(new EmptyBorder(new Insets(0, 5, 0, 5)));
        pfieldpanel.add(pfield);

        ppanel.add(plabel);
        ppanel.add(pchoose);
        ppanel.add(pfieldpanel);
        ppanel.add(prun);

        refresh.addActionListener(e -> this.updateHomeworks());
        pchoose.addActionListener(e ->
                                  {
                                      mainclass = selectMainClass();
                                      if (mainclass != null)
                                          try
                                          {
                                              pfield.setText(mainclass.getCanonicalPath());
                                          }
                                          catch (IOException e1)
                                          {
                                              e1.printStackTrace();
                                          }
                                      else
                                          pfield.setText("");
                                  });
        prun.addActionListener(e -> this.runProgram());


        hwbox = new JComboBox<>();
        hwbox.addActionListener(e -> this.selectHomework());
        JLabel hwboxlabel = new JLabel("Homework: ");
        hwboxlabel.setPreferredSize(new Dimension(110, 10));

        itembox = new JComboBox<HomeworkItem>();
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

        JPanel boxpanel = new JPanel();
        boxpanel.setLayout(new BoxLayout(boxpanel, BoxLayout.Y_AXIS));
        boxpanel.setBorder(new EmptyBorder(new Insets(10, 0, 0, 0)));

        boxpanel.add(hboxpanel);
        boxpanel.add(hwlabelpanel);
        boxpanel.add(itemboxpanel);
        boxpanel.add(itemlabelpanel);

        JPanel bpanel = new JPanel();
        bpanel.setLayout(new BoxLayout(bpanel, BoxLayout.X_AXIS));
        JSeparator spacer = new JSeparator();
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, refresh.getHeight()));
        bpanel.add(spacer);
        bpanel.add(refresh);


        topPanel.add(ppanel);
        topPanel.add(new JSeparator());
        topPanel.add(boxpanel);
        topPanel.add(bpanel);


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
        StyleConstants.setForeground(errorstyle, new Color(187, 19, 0)); // red
        StyleConstants.setBold(errorstyle, true);

        correctstyle = new SimpleAttributeSet();
        StyleConstants.setForeground(correctstyle, new Color(0, 92, 0)); // green
        StyleConstants.setBold(correctstyle, true);

        infostyle = new SimpleAttributeSet();
        StyleConstants.setBold(infostyle, true);


        mainPanel.add(topPanel);
        mainPanel.add(auxpanel);
        this.add(mainPanel);

        serverManager = new MoulinetteServerManager();
        this.setVisible(true);

        //welcome message:
        try
        {
            doc.insertString(doc.getLength(),
                             "Welcome to " + prop.getProperty("name") + " v" + prop.getProperty("version") + "." +
                                     linebreak,
                             infostyle);
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }

        this.updateHomeworks();

    }

    private void updateHomeworks()
    {
        JDialog dialog = new JDialog(this, "Updating...", true);
        dialog.setLayout(new BorderLayout());
        JPanel dpanel = new JPanel(new BorderLayout());
        dpanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        dpanel.add(new JLabel("Updating homework assignments, please wait..."));

        try
        {
            doc.insertString(doc.getLength(), "Getting homework list from server... ", infostyle);
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }

        dialog.add(dpanel);
        dialog.pack();
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        Thread dt = new Thread(() ->
                               {
                                   dialog.setLocationRelativeTo(this);
                                   serverManager.updateHomeworks();
                                   java.util.List<Homework> homeworks = serverManager.getHomeworks();
                                   hwbox.removeAllItems();
                                   homeworks.stream().filter(homework -> homework != null)
                                            .forEach(homework -> hwbox.addItem(homework));

                                   dialog.setVisible(false);
                               });
        dt.start();
        dialog.setVisible(true);

        try
        {
            dt.join();
            doc.insertString(doc.getLength(), "Done!" + linebreak, infostyle);
        }
        catch (InterruptedException | BadLocationException e)
        {
            e.printStackTrace();
        }

    }

    private File selectMainClass()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File f)
            {
                if (f.isDirectory())
                {
                    return true;
                }
                else
                {
                    String filename = f.getName().toLowerCase();
                    return filename.endsWith(".java");
                }
            }

            @Override
            public String getDescription()
            {
                return "Java source files (.java)";
            }
        });

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            return fc.getSelectedFile();
        }
        return null;
    }

    private void runProgram()
    {
        if (mainclass == null)
        {
            showErrorDialog("Please select a program.", "Please select a program to run and test.");
            return;
        }

        refresh.setEnabled(false);
        pchoose.setEnabled(false);
        prun.setEnabled(false);
        hwbox.setEnabled(false);
        itembox.setEnabled(false);

        Thread t = new Thread(() ->
                              {

                                  try
                                  {
                                      String ISOnow = LocalDateTime.now().toLocalTime().toString();
                                      doc.insertString(doc.getLength(), linebreak, null);
                                      doc.insertString(doc.getLength(), "[" + ISOnow + "] ", infostyle);
                                      doc.insertString(doc.getLength(), "Evaluating " + mainclass.getName() + linebreak,
                                                       infostyle);
                                      ProgramRunner pr =
                                              new ProgramRunner(mainclass, java_home + File.separator + "bin");
                                      doc.insertString(doc.getLength(), "Compiling... ", null);
                                      pr.compile();
                                      doc.insertString(doc.getLength(), "Done." + linebreak, null);
                                      doc.insertString(doc.getLength(), "Running tests..." + linebreak, null);

                                      JSONArray results = new JSONArray();

                                      for (HomeworkTest test : tests.values())
                                      {
                                          JSONObject tobj = new JSONObject();
                                          String output = pr.run(test.input, 5, TimeUnit.SECONDS);
                                          tobj.put("id", test.id);
                                          tobj.put("output", output);
                                          results.put(tobj);
                                      }

                                      java.util.List<MoulinetteServerManager.TestResult> res =
                                              serverManager.validateTests(results);

                                      doc.insertString(doc.getLength(), linebreak, null);

                                      for (MoulinetteServerManager.TestResult result : res)
                                      {
                                          doc.insertString(doc.getLength(), "Test ID: ", null);
                                          doc.insertString(doc.getLength(), result.id + linebreak, infostyle);
                                          doc.insertString(doc.getLength(), "Description: ", null);
                                          doc.insertString(doc.getLength(),
                                                           tests.get(result.id).description + linebreak, null);
                                          doc.insertString(doc.getLength(), "Result: ", null);
                                          if (result.test_ok)
                                              doc.insertString(doc.getLength(), "Correct ✓" + linebreak, correctstyle);
                                          else
                                              doc.insertString(doc.getLength(), "Incorrect ✗" + linebreak,
                                                               errorstyle);
                                          doc.insertString(doc.getLength(), linebreak, null);
                                      }

                                  }
                                  catch (IOException | InterruptedException | ProgramRunner.ProgramNotCompiled |
                                          BadLocationException | MoulinetteServerManager.ServerError e)
                                  {
                                      try
                                      {
                                          doc.insertString(doc.getLength(), "Something went wrong. Please refresh the" +
                                                  " application and try again." + linebreak, errorstyle);
                                      }
                                      catch (BadLocationException e1)
                                      {
                                          e1.printStackTrace();
                                      }
                                  }
                                  catch (ProgramRunner.ExecutionError executionError)
                                  {
                                      try
                                      {
                                          doc.insertString(doc.getLength(),
                                                           linebreak + "Error when executing " + mainclass.getName() +
                                                                   linebreak, errorstyle);
                                      }
                                      catch (BadLocationException e)
                                      {
                                          e.printStackTrace();
                                      }
                                  }
                                  catch (ProgramRunner.CompileError compileError)
                                  {
                                      try
                                      {
                                          doc.insertString(doc.getLength(), linebreak + compileError.stderr + linebreak,
                                                           errorstyle);
                                      }
                                      catch (BadLocationException e)
                                      {
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

    private void selectHomework()
    {
        Homework selected = (Homework) hwbox.getSelectedItem();
        itembox.removeAllItems();
        if (selected != null)
        {
            selected.getItems().stream().filter(item -> item != null).forEach(item -> itembox.addItem(item));
            hwdescription.setText(selected.getDescription() + linebreak);
        }

    }

    private void selectHomeworkItem()
    {
        HomeworkItem selected = (HomeworkItem) itembox.getSelectedItem();
        if (selected != null)
        {
            itemdescription.setText(selected.getDescription() + linebreak);
            tests = new HashMap<>(selected.getTests().size());
            for (HomeworkTest t : selected.getTests())
                tests.put(t.id, t);
        }
    }

    private void showErrorDialog(String title, String error)
    {
        JDialog errordialog = new JDialog(this, title, true);
        JPanel dpanel = new JPanel(new BorderLayout());
        dpanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        errordialog.add(dpanel);
        dpanel.add(new JLabel(error));
        errordialog.pack();
        errordialog.setLocationRelativeTo(this);
        errordialog.setVisible(true);
    }

    private String getJavaHomeEnv()
    {

        String jhome = System.getenv("JAVA_HOME");
        if (jhome == null)
        {
            showErrorDialog("JAVA_HOME not set!",
                            "Please set your JAVA_HOME environment variable and restart this application.");
            System.exit(1);
        }
        return jhome;
    }

    public static void main(String[] args) throws IOException
    {

        InputStream resourceAsStream =
                MoulinetteApplication.class.getResourceAsStream(
                        "/version.properties"
                );
        Properties prop = new Properties();
        prop.load(resourceAsStream);

        new MoulinetteApplication(800, 600, prop);
    }
}
