package org.olguin.moulinette.homework;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-21.
 * Part of org.olguin.moulinette.homework.
 */
public class HomeworkItem {
    private String id;
    private String name;
    private String description;
    private Map<String, String> tests;

    public HomeworkItem(String id, String name, String description)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tests = new HashMap<>(3);
    }

    public void addTest(String id, String input)
    {
        tests.put(id, input);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getTests() {
        return tests;
    }

    public String getName() {
        return name;
    }

    public String toString() { return name; }
}
