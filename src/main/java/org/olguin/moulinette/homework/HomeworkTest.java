package org.olguin.moulinette.homework;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-25.
 * Part of org.olguin.moulinette.homework.
 */
public class HomeworkTest
{
    public String id;
    public String description;
    public String input;
    public int timeout;

    public HomeworkTest(String id, String description, String input, int timeout)
    {
        this.id = id;
        this.description = description;
        this.input = input;
        this.timeout = timeout;
    }
}
