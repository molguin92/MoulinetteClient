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
 * <p>
 * Simple class for managing program preferences in a text file containing the preferences in json format.
 */
class SimpleJSONPreferences
{
    private File prefsfile;
    private JSONObject jsonprefs;

    private SimpleJSONPreferences()
    {
    }


    /**
     * Creates a new SimpleJSONPreferences object using the specified file uri for persistent storage.
     *
     * @param uri The path to the preferences file.
     * @return A SimpleJSONPreferences object.
     */
    static SimpleJSONPreferences loadFile(String uri)
    {
        SimpleJSONPreferences prefs = new SimpleJSONPreferences();
        prefs.prefsfile = new File(uri);
        prefs.loadFromFile();
        return prefs;
    }

    /**
     * Loads preferences from file
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    /**
     * Flushes the modified preferences to the file.
     */
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

    /**
     * Put a new configuration key into the preferences.
     *
     * @param key   Configuration key.
     * @param value Configuration value.
     */
    void put(String key, String value)
    {
        this.jsonprefs.put(key, value);
        this.flushToFile();
        this.loadFromFile();
    }

    /**
     * Get the value of a configuration key. If it doesn't exists, returns the value passed as default.
     *
     * @param key The configuration key to lookup.
     * @param def Default value in case key is not found.
     * @return The value associated with the given key, or def if the key is not found.
     */
    String get(String key, String def)
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

    /**
     * Removes the value associated with the given key. Does nothing if the key is not found.
     *
     * @param key The configuration key to clear.
     */
    void remove(String key)
    {
        try
        {
            this.jsonprefs.remove(key);
        }
        catch (JSONException ignored)
        {
        }
    }

    /**
     * Clears the preference storage.
     */
    void clear()
    {
        this.jsonprefs = new JSONObject();
        this.flushToFile();
        this.loadFromFile();
    }
}
