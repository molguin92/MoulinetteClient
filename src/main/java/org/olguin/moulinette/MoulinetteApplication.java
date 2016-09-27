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


    /**
     * Creates a new instance of the application. Instantiates the window and all its components, along with the
     * ServerManager and ProgramRunner instances needed.
     *
     * @param width  Width of the window.
     * @param height Height of the window.
     * @param prop   Properties object for accessing static properties of the application (version, name, etc).
     */
    private MoulinetteApplication(int width, int height, Properties prop)
    {
        super(prop.getProperty("name") + " " + prop.getProperty("version"));
        java_home = getJavaHomeEnv();

        // Initialize basic window
        this.setSize(width, height);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // mainPanel is the principal container, which holds two secondary panels:
        // topPanel for the buttons, textfields and comboboxes; and botPanel, which
        // holds the textArea with its ScrollPane.
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));

        // Buttons:
        refresh = new JButton("Refresh");
        pchoose = new JButton("Browse...");
        prun = new JButton("Run");

        // ppanel goes inside topPanel, and holds the browse and run buttons, along with
        // a textfield which indicates the chosen program to run and a textlabel
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

        //actionlisteners for the buttons
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


        // comboboxes for homeworks and items
        hwbox = new JComboBox<>();
        hwbox.addActionListener(e -> this.selectHomework());
        JLabel hwboxlabel = new JLabel("Homework: ");
        hwboxlabel.setPreferredSize(new Dimension(110, 10));

        itembox = new JComboBox<HomeworkItem>();
        itembox.addActionListener(e -> this.selectHomeworkItem());
        JLabel itemboxlabel = new JLabel("Item: ");
        itemboxlabel.setPreferredSize(new Dimension(110, 10));


        // panels to hold the boxes and their labels
        JPanel hboxpanel = new JPanel();
        hboxpanel.setLayout(new BoxLayout(hboxpanel, BoxLayout.X_AXIS));
        hboxpanel.add(hwboxlabel);
        hboxpanel.add(hwbox);

        JPanel itemboxpanel = new JPanel();
        itemboxpanel.setLayout(new BoxLayout(itemboxpanel, BoxLayout.X_AXIS));
        itemboxpanel.add(itemboxlabel);
        itemboxpanel.add(itembox);

        // labels for the descriptions
        JLabel hwlabel = new JLabel("Description: ");
        JLabel itemlabel = new JLabel("Description: ");
        hwlabel.setPreferredSize(new Dimension(110, 10));
        itemlabel.setPreferredSize(new Dimension(110, 10));

        // and panels for the descriptions
        JPanel hwlabelpanel = new JPanel();
        hwlabelpanel.setLayout(new BoxLayout(hwlabelpanel, BoxLayout.X_AXIS));
        hwlabelpanel.setBorder(new EmptyBorder(new Insets(5, 0, 5, 0)));

        JPanel itemlabelpanel = new JPanel();
        itemlabelpanel.setLayout(new BoxLayout(itemlabelpanel, BoxLayout.X_AXIS));
        itemlabelpanel.setBorder(new EmptyBorder(new Insets(5, 0, 5, 0)));

        hwlabelpanel.add(hwlabel);
        itemlabelpanel.add(itemlabel);

        // description fields and scrollpanes
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

        // auxiliary panel to hold the panels which hold the
        // comboboxes and descriptions
        JPanel boxpanel = new JPanel();
        boxpanel.setLayout(new BoxLayout(boxpanel, BoxLayout.Y_AXIS));
        boxpanel.setBorder(new EmptyBorder(new Insets(10, 0, 0, 0)));

        boxpanel.add(hboxpanel);
        boxpanel.add(hwlabelpanel);
        boxpanel.add(itemboxpanel);
        boxpanel.add(itemlabelpanel);

        // auxiliary panel to hold the Refresh button and push it to the right.
        JPanel bpanel = new JPanel();
        bpanel.setLayout(new BoxLayout(bpanel, BoxLayout.X_AXIS));
        JSeparator spacer = new JSeparator();
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, refresh.getHeight()));
        bpanel.add(spacer);
        bpanel.add(refresh);

        // put together to complete top half of the window
        topPanel.add(ppanel);
        topPanel.add(new JSeparator());
        topPanel.add(boxpanel);
        topPanel.add(bpanel);

        // build the bottom half:
        // panels to hold the scrollpane and textPane, and give them
        // a nice border.
        JPanel tpanel = new JPanel(new BorderLayout());
        JPanel botPanel = new JPanel(new BorderLayout());
        botPanel.setBorder(new EmptyBorder(new Insets(10, 0, 0, 0)));
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
        botPanel.add(tpanel);

        // document styles
        errorstyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorstyle, new Color(187, 19, 0)); // red
        StyleConstants.setBold(errorstyle, true);

        correctstyle = new SimpleAttributeSet();
        StyleConstants.setForeground(correctstyle, new Color(0, 92, 0)); // green
        StyleConstants.setBold(correctstyle, true);

        infostyle = new SimpleAttributeSet();
        StyleConstants.setBold(infostyle, true);

        // put the window together
        mainPanel.add(topPanel);
        mainPanel.add(botPanel);
        this.add(mainPanel);

        // initialize the ServerManager
        serverManager = new MoulinetteServerManager(prop);
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

        // finally, get homeworks from the server
        this.updateHomeworks();

    }

    /**
     * Shows a dialog and fires an asynchronous request to the server to update the homework collection.
     */
    private void updateHomeworks()
    {
        // while we update, show a nice, uncloseable dialog.
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
        // make it uncloseable
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // start a separate thread to do the update, otherwise it locks up the whole
        // window indefinitely.
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

    /**
     * Brings up the standard java FileChooser dialog to select the main class of the program to be evaluated.
     *
     * @return The chose file, null if cancelled.
     */
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

    /**
     * Runs all the associated tests for a selected homework item on the selected student program, and shows the
     * results on the textpane.
     */
    private void runProgram()
    {
        if (mainclass == null)
        {
            showErrorDialog("Please select a program.", "Please select a program to run and test.");
            return;
        }

        // disable interaction with the window while we work
        refresh.setEnabled(false);
        pchoose.setEnabled(false);
        prun.setEnabled(false);
        hwbox.setEnabled(false);
        itembox.setEnabled(false);

        // verification is done in a separate thread to avoid hangups
        Thread t = new Thread(() ->
                              {

                                  try
                                  {
                                      // some info before tests...
                                      String ISOnow = LocalDateTime.now().toLocalTime().toString();
                                      doc.insertString(doc.getLength(), linebreak, null);
                                      doc.insertString(doc.getLength(), "[" + ISOnow + "] ", infostyle);
                                      doc.insertString(doc.getLength(), "Evaluating " + mainclass.getName() + linebreak,
                                                       infostyle);

                                      // initialize the program runner object.
                                      ProgramRunner pr =
                                              new ProgramRunner(mainclass, java_home + File.separator + "bin");

                                      // compile
                                      doc.insertString(doc.getLength(), "Compiling... ", null);
                                      pr.compile();

                                      doc.insertString(doc.getLength(), "Done." + linebreak, null);
                                      doc.insertString(doc.getLength(), "Running tests..." + linebreak, null);
                                      doc.insertString(doc.getLength(), "------ * ------" + linebreak, infostyle);

                                      // results are stored in a JSON array which is then passed to the server
                                      // manager for verification on the remote server
                                      JSONArray results = new JSONArray();
                                      for (HomeworkTest test : tests.values())
                                      {
                                          doc.insertString(doc.getLength(), "Running Test with ID: ", null);
                                          doc.insertString(doc.getLength(), test.id + linebreak, infostyle);
                                          doc.insertString(doc.getLength(), "Description: ", null);
                                          doc.insertString(doc.getLength(), test.description + linebreak, null);
                                          doc.insertString(doc.getLength(), "------ * ------" + linebreak, infostyle);

                                          JSONObject tobj = new JSONObject();
                                          String output = pr.run(test.input, test.timeout, TimeUnit.SECONDS);
                                          tobj.put("id", test.id);
                                          tobj.put("output", output);
                                          results.put(tobj);
                                      }

                                      // verify
                                      doc.insertString(doc.getLength(), "Verifying results..." + linebreak, null);
                                      java.util.List<MoulinetteServerManager.TestResult> res =
                                              serverManager.validateTests(results);


                                      // finally, show the verification results on the textPane
                                      for (MoulinetteServerManager.TestResult result : res)
                                      {
                                          doc.insertString(doc.getLength(), result.id, infostyle);
                                          doc.insertString(doc.getLength(), " - Result: ", null);
                                          if (result.test_ok)
                                              doc.insertString(doc.getLength(), "Correct ✓" + linebreak, correctstyle);
                                          else
                                              doc.insertString(doc.getLength(), "Incorrect ✗" + linebreak,
                                                               errorstyle);
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
                                          doc.insertString(doc.getLength(), linebreak, errorstyle);
                                          doc.insertString(doc.getLength(), executionError.getErrorStub() + linebreak,
                                                           errorstyle);
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

                                  // reenable the window
                                  refresh.setEnabled(true);
                                  pchoose.setEnabled(true);
                                  prun.setEnabled(true);
                                  hwbox.setEnabled(true);
                                  itembox.setEnabled(true);

                              });

        t.start();
    }

    /**
     * Updates the selected homework from the combobox.
     */
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

    /**
     * Updates the selected item from the combobox.
     */
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

    /**
     * Auxiliary funtion to show a simple dialog.
     *
     * @param title Title of the dialog window.
     * @param error The error text to show.
     */
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

    /**
     * Gets the JAVA_HOME variable from the environment, and shows a warning dialog in case it is not set.
     *
     * @return A string containing the JAVA_HOME variable value.
     */
    private String getJavaHomeEnv()
    {

        String jhome = System.getenv("JAVA_HOME");
        if (jhome == null)
        {
            showErrorDialog("JAVA_HOME not set!",
                            "Please set your JAVA_HOME environment variable and restart this application.");
            System.exit(1);
        }

        // fix problems with people having their JAVA_HOME ending in a slash.
        if (jhome.endsWith(File.separator))
            jhome = jhome.substring(0, jhome.length() - 1);

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
