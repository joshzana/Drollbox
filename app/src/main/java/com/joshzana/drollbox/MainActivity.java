package com.joshzana.drollbox;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toolbar;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity
{
    public static final int DROPBOX_REQUEST_CODE = 12345;
    private static final int TAKE_PHOTO_CODE = 5555;
    private static final int SELECT_PHOTO_CODE = 67890;
    @Inject DbxAccountManager mDbxAccountManager;
    @Inject DbxDatastoreManager mDbxDatastoreManager;
    @Inject DbxFileSystem mDbxFileSystem;

    @InjectView(R.id.textView) TextView mTextView;
    @InjectView(R.id.toolbar) Toolbar mToolbar;
    @InjectView(R.id.dropbox_button) Button mLinkButton;
    @InjectView(R.id.add_photo_button) Button mAddPhotoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("MainActivity", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((DrollboxApplication) getApplication()).inject(this);
        ButterKnife.inject(this);

        bind();
    }

    private void bind()
    {
        if (mDbxAccountManager.hasLinkedAccount())
        {
            mLinkButton.setVisibility(View.GONE);
            DbxAccountInfo info = mDbxAccountManager.getLinkedAccount().getAccountInfo();
            // info is null post-link for some reason
            mTextView.setText(info == null ? "Linked" : info.displayName);
        }
        else
        {
            mAddPhotoButton.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.dropbox_button) void handleLinkButtonClick()
    {
        mDbxAccountManager.startLink(this, DROPBOX_REQUEST_CODE);
    }
    @OnClick(R.id.add_photo_button) void handleAddPhotoButtonClick()
    {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO_CODE);
    }
    @OnClick(R.id.take_photo_button) void handleTakePhotoButtonClick()
    {
        // From http://developer.android.com/training/camera/photobasics.html
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d("MainActivity", "onActivityResult " + requestCode + " " + resultCode);

        if (requestCode == DROPBOX_REQUEST_CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                DbxAccount account = mDbxAccountManager.getLinkedAccount();
                try
                {
                    mDbxDatastoreManager.migrateToAccount(account);

                    mDbxDatastoreManager = DbxDatastoreManager.forAccount(account);

                    bind();
                }
                catch (DbxException e)
                {
                    Log.e("MainActivity", "Fail", e);
                }
            }
            else
            {
                Log.e("MainActivity", "Fail");
            }
        }
        else if (requestCode == SELECT_PHOTO_CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Uri selectedImage = data.getData();
                String[] queryColumns = {MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.TITLE};
                Cursor cursor = getContentResolver().query(selectedImage, queryColumns, null, null, null);
                cursor.moveToFirst();

                int date = cursor.getInt(cursor.getColumnIndex(queryColumns[0]));
                String title = cursor.getString(cursor.getColumnIndex(queryColumns[1]));

                InputStream stream = null;

                try
                {
                    stream = getContentResolver().openInputStream(selectedImage);
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }

                if (stream != null && title != null)
                {
                    String path = addFile(title, date, stream);
                    if (path != null)
                    {
                        addRow(path, title, date);
                    }
                }

            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String addFile(String title, int date, InputStream stream)
    {
        DbxFile newFile = null;
        try
        {
            DbxPath path = new DbxPath(DbxPath.ROOT, "images/"+ date + "/" + title);
            newFile = mDbxFileSystem.create(path);
            FileOutputStream writer = newFile.getWriteStream();
            byte[] buffer = new byte[4096];
            int read;
            while((read = stream.read(buffer, 0, 4096)) > 0)
            {
                writer.write(buffer, 0, read);
            }

            return path.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (newFile != null)
            {
                newFile.close();
            }

            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void addRow(String path, String title, int date)
    {
        try
        {
            DbxDatastore datastore = mDbxDatastoreManager.openDefaultDatastore();

            DbxTable table = datastore.getTable("boards");
            DbxRecord newRecord = table.insert().set("path", path).set("title", title).set("date", date);
            datastore.sync();
        }
        catch (DbxException e)
        {
            Log.e("MainActivity", "Fail", e);
        }
    }
}

