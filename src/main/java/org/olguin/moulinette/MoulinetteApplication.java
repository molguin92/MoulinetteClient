package org.olguin.moulinette;

import com.github.kevinsawicki.http.HttpRequest;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-15.
 * Part of org.olguin.moulinette.
 */
public class MoulinetteApplication {

    public static void main(String [] args)
    {
        String response = HttpRequest.get("https://moulinetteweb.herokuapp.com/api/v1/homeworks").body();
        System.out.println("Response was: " + response);
    }
}
