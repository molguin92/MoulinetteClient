package org.olguin.moulinette;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-28.
 * Part of org.olguin.moulinette.
 */
public class SimpleJSONPreferences
{
    private File prefsfile;
    private JSONObject jsonprefs;


    public static SimpleJSONPreferences loadFile(String uri)
    {
        SimpleJSONPreferences prefs = new SimpleJSONPreferences();
        prefs.prefsfile = new File(uri);
        prefs.loadFromFile();
        return prefs;
    }

    private void loadFromFile()
    {
        try
        {
            prefsfile.createNewFile();
            byte[] in = Files.readAllBytes(prefsfile.toPath());
            String enc_in = new String(in, StandardCharsets.UTF_8);
            jsonprefs = new JSONObject(enc_in);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            jsonprefs = new JSONObject();
        }
    }

    private void flushToFile()
    {
        try
        {
            PrintWriter pw = new PrintWriter(prefsfile);
            pw.flush();
            pw.close();

            pw = new PrintWriter(prefsfile, StandardCharsets.UTF_8.name());
            pw.print(jsonprefs.toString());
            pw.flush();
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void put(String key, String value)
    {
        this.jsonprefs.put(key, value);
        this.flushToFile();
        this.loadFromFile();
    }

    public String get(String key, String def)
    {
        try
        {
            return this.jsonprefs.getString(key);
        }
        catch (JSONException e)
        {
            return def;
        }
    }

    public void remove(String key)
    {
        try
        {
            this.jsonprefs.remove(key);
        }
        catch (JSONException ignored)
        {
        }
    }

    public void clear()
    {
        this.jsonprefs = new JSONObject();
        this.flushToFile();
        this.loadFromFile();
    }
}
