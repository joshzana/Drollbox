package com.joshzana.drollbox;

import android.util.Log;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileSystem;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Created by joshzana on 12/30/14.
 */
@Module(injects = MainActivity.class, complete = false)
public class DropboxModule
{
    private final DrollboxApplication mApplication;

    public DropboxModule(DrollboxApplication application)
    {
        mApplication = application;
    }

    @Provides @Singleton
    DbxAccountManager provideDbxAccountManager()
    {
        return DbxAccountManager.getInstance(mApplication, mApplication.getString(R.string.dropbox_api_key), mApplication.getString(R.string.dropbox_api_secret));
    }

    @Provides @Singleton
    DbxFileSystem provideDbxFileSystem()
    {
        try
        {
            DbxAccountManager accountManager = provideDbxAccountManager();
            if (accountManager.hasLinkedAccount())
            {
                return DbxFileSystem.forAccount(accountManager.getLinkedAccount());
            }
        }
        catch (DbxException.Unauthorized e)
        {
            Log.e("DropBoxModule", "Fail", e);
        }

        return null;
    }

    @Provides @Singleton
    DbxDatastoreManager provideDbxDatastoreManager()
    {
        DbxAccountManager accountManager = provideDbxAccountManager();
        DbxDatastoreManager datastoreManager = null;
        if (accountManager.hasLinkedAccount())
        {
            try
            {
                datastoreManager = DbxDatastoreManager.forAccount(accountManager.getLinkedAccount());
            }
            catch (DbxException.Unauthorized e)
            {
                Log.e("DropBoxModule", "Fail", e);
            }
        }

        if (datastoreManager == null)
        {
            datastoreManager = DbxDatastoreManager.localManager(accountManager);
        }

        return datastoreManager;
    }
}
