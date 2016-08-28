package org.olguin.moulinette;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olguin.moulinette.homework.Homework;
import org.olguin.moulinette.homework.HomeworkItem;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-21.
 * Part of org.olguin.moulinette.
 */
public class MoulinetteServerManager
{

    private static String CLIENT_ID_PREF = "CLIENT_ID";

    private String serveruri;
    private String clientid;
    private List<Homework> homeworks;
    private final Preferences prefs;

    public MoulinetteServerManager()
    {

        prefs = Preferences.userNodeForPackage(MoulinetteServerManager.class);

        if (System.getenv("MOULINETTE_DEBUG") != null && System.getenv("MOULINETTE_DEBUG").equals("TRUE"))
            serveruri = System.getenv("SERVER_URL") + "/api/v1/";
        else
            serveruri = "https://moulinetteweb.herokuapp.com/api/v1/";
    }


    public void updateHomeworks()
    {
        // first, check client id
        clientid = prefs.get(CLIENT_ID_PREF, null);
        if (clientid == null)
        {
            clientid = HttpRequest.get(serveruri + "clients").body();
            prefs.put(CLIENT_ID_PREF, clientid);
        }

        // now, homeworks
        homeworks = new ArrayList<>(10);
        String res = HttpRequest.get(serveruri + "homeworks").body();
        JSONArray hws = new JSONObject(res).getJSONArray("result");
        for (Object o : hws)
        {
            JSONObject hw = (JSONObject) o;
            String id = hw.getString("id");
            String name = hw.getString("name");
            String description = hw.getString("description");
            List<HomeworkItem> items = new ArrayList<>(3);
            JSONArray jitems = hw.getJSONArray("items");

            for (Object o1 : jitems)
            {
                JSONObject it = (JSONObject) o1;
                HomeworkItem item = new HomeworkItem(
                        it.getString("id"),
                        it.getString("name"),
                        it.getString("description")
                );

                JSONArray jtests = it.getJSONArray("tests");
                for (Object o2 : jtests)
                {
                    JSONObject test = (JSONObject) o2;
                    item.addTest(test.getString("id"), test.getString("description"), test.getString("input"));
                }

                items.add(item);
            }

            Homework homework = new Homework(id, name, description, items);
            homeworks.add(homework);
        }
    }

    public List<TestResult> validateTests(JSONArray tests) throws ServerError
    {
        JSONObject data = new JSONObject();
        data.put("results", tests);
        data.put("client_id", clientid);

        HttpRequest req =
                HttpRequest.post(serveruri + "validate_tests")
                           .contentType("application/json")
                           .send(data.toString());

        if (req.badRequest() || req.code() == 401)
        {
            clientid = null;
            prefs.remove(CLIENT_ID_PREF);
            throw new ServerError();
        }

        JSONArray results = new JSONObject(req.body()).getJSONArray("results");
        List<TestResult> ret = new ArrayList<>(results.length());
        for (Object t : results)
        {
            JSONObject o = (JSONObject) t;
            ret.add(new TestResult(o.getString("test_id"), o.getBoolean("result_ok"), o.getString("error")));
        }

        return ret;
    }

    public List<Homework> getHomeworks()
    {
        return this.homeworks;
    }


    public static void main(String[] args) throws BackingStoreException
    {
        new MoulinetteServerManager().clearPrefs();
    }

    private void clearPrefs() throws BackingStoreException
    {
        System.out.println("Clearing all preferences.");
        prefs.clear();
    }

    public class TestResult
    {
        public String id;
        public boolean test_ok;
        public String error;

        public TestResult(String id, boolean test_ok, String error)
        {
            this.id = id;
            this.test_ok = test_ok;
            this.error = error;
        }
    }

    public class ServerError extends Exception
    {
    }
}
