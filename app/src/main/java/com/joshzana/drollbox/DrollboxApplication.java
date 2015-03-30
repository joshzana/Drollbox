package com.joshzana.drollbox;

import android.app.Application;
import dagger.ObjectGraph;

import java.util.Arrays;
import java.util.List;

/**
 * Created by joshzana on 12/30/14.
 */
public class DrollboxApplication extends Application
{
    private ObjectGraph mGraph;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mGraph = ObjectGraph.create(getModules().toArray());
    }

    private List<DropboxModule> getModules()
    {
        return Arrays.asList(
                new DropboxModule(this));
    }

    public void inject(Object object)
    {
        mGraph.inject(object);
    }
}
