package org.olguin.moulinette;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-15.
 * Part of org.olguin.moulinette.
 */
public class MoulinetteApplication {

    private MoulinetteApplication(int width, int height)
    {
        /*JFrame window = new JFrame();
        window.setSize(width, height);

        JButton b=new JButton("click");//creating instance of JButton
        b.setBounds(width/2 - 50,height/2 - 20,100, 40);//x axis, y axis, width, height
        b.addActionListener(new ButtonActionListener());

        window.add(b);//adding button in JFrame

        window.setLayout(null);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);*/

        JFileChooser fc = new JFileChooser();
        int retVal = fc.showDialog(new JFrame(), null);
        if(retVal != JFileChooser.APPROVE_OPTION) System.exit(1);

        File file = fc.getSelectedFile();
        try {
            ProgramRunner pr = new ProgramRunner(file, "/usr/bin");
            pr.compile();
            System.out.print(pr.run("multi\nline\necho\n", 3, TimeUnit.SECONDS));
        } catch (IOException | ProgramRunner.ExecutionError | ProgramRunner.ProgramNotCompiled | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String [] args)
    {
        new MoulinetteApplication(800, 600);
    }

    private class ButtonActionListener implements ActionListener
    {

        public void actionPerformed(ActionEvent e) {
            String response = HttpRequest.get("https://moulinetteweb.herokuapp.com/api/v1/homeworks").body();
            JSONObject obj = new JSONObject(response);
            int len = obj.getInt("len");
            JSONArray arr = obj.getJSONArray("result");


            System.out.println(len);
            System.out.println(arr);
        }
    }
}
