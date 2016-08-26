package org.olguin.moulinette.homework;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-21.
 * Part of org.olguin.moulinette.homework.
 */
public class HomeworkItem {
    private String id;
    private String name;
    private String description;
    private List<HomeworkTest> tests;

    public HomeworkItem(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tests = new ArrayList<>(3);
    }

    public void addTest(String id, String description, String input) {
        tests.add(new HomeworkTest(id, description, input));
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<HomeworkTest> getTests() {
        return tests;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
