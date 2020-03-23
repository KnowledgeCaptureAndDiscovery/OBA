package edu.isi.oba.config;

import java.util.HashMap;
import java.util.Map;


public enum Provider
{
    FIREBASE("firebase");

    private String name;

    Provider(String envUrl) {
        this.name = envUrl;
    }

    public String getName() {
        return name;
    }

    //****** Reverse Lookup Implementation************//

    //Lookup table
    private static final Map<String, Provider> lookup = new HashMap<>();

    //Populate the lookup table on loading time
    static
    {
        for(Provider env : Provider.values())
        {
            lookup.put(env.getName(), env);
        }
    }

    //This method can be used for reverse lookup purpose
    public static Provider get(String url)
    {
        return lookup.get(url);
    }
}
