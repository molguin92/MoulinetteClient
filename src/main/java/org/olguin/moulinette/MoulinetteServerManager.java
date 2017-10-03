package org.olguin.moulinette;

import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olguin.moulinette.homework.Homework;
import org.olguin.moulinette.homework.HomeworkItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;

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
    private final SimpleJSONPreferences prefs;

    /**
     * Creates a new server manager using the given properties. If the MOULINETTE_DEBUG environment variable is set
     * to TRUE, the server URL is set to whatever the SERVER_URL environment variable is set to. Otherwise, the
     * server URL is taken from the properties.
     *
     * @param prop  Properties object containing the URL of the server.
     * @param prefs
     */
    MoulinetteServerManager(Properties prop, SimpleJSONPreferences prefs)
    {

        this.prefs = prefs;

        if (System.getenv("MOULINETTE_DEBUG") != null && System.getenv("MOULINETTE_DEBUG").equals("TRUE"))
            serveruri = System.getenv("SERVER_URL") + "/api/v1/";
        else
            serveruri = prop.getProperty("serveruri");
    }

    /**
     * Fetches the list of active homework assignments from the server.
     */
    void updateHomeworks()
    {
        // first, check client id
        clientid = prefs.get(CLIENT_ID_PREF, null);
        if (clientid == null)
        {
            clientid = HttpRequest.get(serveruri + "clients").followRedirects(true).body();
            prefs.put(CLIENT_ID_PREF, clientid);
        }

        // now, homeworks
        homeworks = new ArrayList<>(10);
        String res = HttpRequest.get(serveruri + "homeworks").followRedirects(true).body();
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
                    item.addTest(test.getString("id"), test.getString("description"), test.getString("input"),
                                 test.getInt("timeout"));
                }

                items.add(item);
            }

            Homework homework = new Homework(id, name, description, items);
            homeworks.add(homework);
        }
    }

    /**
     * Validates a batch of test outputs. The outputs are to be passed in as a JSONArray containing a JSONObject for
     * each test. Each test needs to contain the fields "id" and "output".
     *
     * @param tests The batch of tests to send to the server for verification.
     * @return A list of results, one for each of the sent tests.
     * @throws ServerError In case of a non-200 response code from the server.
     */
    List<TestResult> validateTests(JSONArray tests) throws ServerError
    {
        JSONObject data = new JSONObject();
        data.put("results", tests);
        data.put("client_id", clientid);

        HttpRequest req =
                HttpRequest.post(serveruri + "validate_tests").followRedirects(true).contentType("application/json").send(data.toString());

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

    List<Homework> getHomeworks()
    {
        return this.homeworks;
    }

    private void clearPrefs() throws BackingStoreException
    {
        System.out.println("Clearing all preferences.");
        prefs.clear();
    }

    class TestResult
    {
        String id;
        boolean test_ok;
        String error;

        TestResult(String id, boolean test_ok, String error)
        {
            this.id = id;
            this.test_ok = test_ok;
            this.error = error;
        }
    }

    class ServerError extends Exception
    {
    }
}
