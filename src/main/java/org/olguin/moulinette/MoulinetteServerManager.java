package org.olguin.moulinette;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.*;
import org.olguin.moulinette.homework.Homework;
import org.olguin.moulinette.homework.HomeworkItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-21.
 * Part of org.olguin.moulinette.
 */
public class MoulinetteServerManager {

    private String serveruri = "https://moulinetteweb.herokuapp.com/api/v1/";
    private List<Homework> homeworks;

    public void updateHomeworks() {
        System.err.println("Updating homeworks...");
        homeworks = new ArrayList<>(10);
        String res = HttpRequest.get(serveruri + "homeworks").body();
        JSONArray hws = new JSONObject(res).getJSONArray("result");
        for (Object o : hws) {
            JSONObject hw = (JSONObject) o;
            String id = hw.getString("id");
            String name = hw.getString("name");
            String description = hw.getString("description");
            List<HomeworkItem> items = new ArrayList<>(3);
            JSONArray jitems = hw.getJSONArray("items");

            for (Object o1 : jitems) {
                JSONObject it = (JSONObject) o1;
                HomeworkItem item = new HomeworkItem(
                        it.getString("id"),
                        it.getString("name"),
                        it.getString("description")
                );

                JSONArray jtests = it.getJSONArray("tests");
                for (Object o2 : jtests) {
                    JSONObject test = (JSONObject) o2;
                    item.addTest(test.getString("id"), test.getString("input"));
                }

                items.add(item);
            }

            Homework homework = new Homework(id, name, description, items);
            homeworks.add(homework);
        }

        System.err.println("Done updating!");
    }

    public List<Homework> getHomeworks()
    {
        return this.homeworks;
    }
}
