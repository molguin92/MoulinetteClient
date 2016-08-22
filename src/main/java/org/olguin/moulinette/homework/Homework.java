package org.olguin.moulinette.homework;

import java.util.List;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-21.
 * Part of org.olguin.moulinette.homework.
 */
public class Homework {
    private String id;
    private String name;
    private String description;
    private List<HomeworkItem> items;

    public Homework(String id, String name, String description, List<HomeworkItem> items)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<HomeworkItem> getItems() {
        return items;
    }

    public String getName() {
        return name;
    }

    public String toString() { return name; }
}
