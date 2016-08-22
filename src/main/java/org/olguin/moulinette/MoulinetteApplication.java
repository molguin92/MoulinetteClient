package org.olguin.moulinette;

import org.olguin.moulinette.homework.Homework;
import org.olguin.moulinette.homework.HomeworkItem;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-15.
 * Part of org.olguin.moulinette.
 */
public class MoulinetteApplication extends JFrame {

    private MoulinetteServerManager serverManager;
    private JComboBox hwbox;
    private JComboBox itembox;
    private TextArea textArea;
    private static String linebreak = System.getProperty("line.separator");

    private MoulinetteApplication(int width, int height)
    {
        super("Moulinette");
        this.setSize(width, height);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel desktopPane = new JPanel();
        desktopPane.setLayout(new BoxLayout(desktopPane, BoxLayout.PAGE_AXIS));

        JPanel bpanel = new JPanel(new FlowLayout());
        Button refresh = new Button("Refresh");
        refresh.addActionListener(e -> this.updateHomeworks());
        bpanel.add(refresh);

        hwbox = new JComboBox();
        hwbox.addActionListener(e -> this.selectHomework());
        itembox = new JComboBox();

        desktopPane.add(bpanel);
        desktopPane.add(hwbox);
        desktopPane.add(itembox);

        JPanel tpanel = new JPanel(new BorderLayout());
        textArea = new TextArea();
        textArea.setEditable(false);
        tpanel.add(textArea);
        desktopPane.add(tpanel);

        this.add(desktopPane);

        serverManager = new MoulinetteServerManager();
        this.setVisible(true);
        this.updateHomeworks();

    }

    private void updateHomeworks()
    {
        JDialog dialog = new JDialog(this, "Updating...", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(new Label("Updating homework assignments, please wait..."));
        dialog.pack();
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        Thread dt = new Thread(() ->
        {
            dialog.setLocationRelativeTo(this);
            serverManager.updateHomeworks();
            java.util.List<Homework> homeworks = serverManager.getHomeworks();
            hwbox.removeAllItems();
            for(Homework homework: homeworks)
                if(homework != null) hwbox.addItem(homework);

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

    private void selectHomework()
    {
        Homework selected = (Homework) hwbox.getSelectedItem();
        itembox.removeAllItems();
        if(selected != null) {
            for (HomeworkItem item : selected.getItems())
                if (item != null) itembox.addItem(item);
        }

    }

    public static void main(String [] args) {
        new MoulinetteApplication(800, 600);
    }
}
