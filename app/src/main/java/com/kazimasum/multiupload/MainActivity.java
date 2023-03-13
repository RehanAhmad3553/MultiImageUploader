package com.kazimasum.multiupload;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{

    StorageReference ref;
    List<String> files,status;
    RecyclerView recview;
    ImageView btn_upload;
    myadapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ref=FirebaseStorage.getInstance().getReference();

        files=new ArrayList<>();
        status=new ArrayList<>();

        recview=(RecyclerView)findViewById(R.id.recview);
        recview.setLayoutManager(new LinearLayoutManager(this));
        adapter=new myadapter(files,status);
        recview.setAdapter(adapter);

        btn_upload=(ImageView)findViewById(R.id.btn_upload);


                btn_upload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                        Dexter.withContext(getApplicationContext())
                                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                .withListener(new PermissionListener() {
                                    @Override
                                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                       Intent intent=new Intent();
                                       intent.setType("image/*");
                                       intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                                       intent.setAction(Intent.ACTION_GET_CONTENT);
                                       startActivityForResult(Intent.createChooser(intent,"Please Select Multiple Files"),101);
                                    }

                                    @Override
                                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                                    }

                                    @Override
                                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                     permissionToken.continuePermissionRequest();
                                    }
                                }).check();

                    }
                });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==101 && resultCode==RESULT_OK)
        {
            if(data.getClipData()!=null)
            {
                 for(int i=0; i<data.getClipData().getItemCount();i++)
                 {
                     Uri fileuri=data.getClipData().getItemAt(i).getUri();
                     String filename=getfilenamefromuri(fileuri);
                     files.add(filename);
                     status.add("loading");
                     adapter.notifyDataSetChanged();

                     final int index=i;
                     StorageReference uploader=ref.child("/multiuploads").child(filename);
                     uploader.putFile(fileuri)
                             .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                 @Override
                                 public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    status.remove(index);
                                    status.add(index,"done");
                                    adapter.notifyDataSetChanged();


                                     AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                     builder.setCancelable(false).
                                             setMessage("Do you want to upload more pics");
                                     builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {
                                             finish();
                                             startActivity(getIntent());
                                         }
                                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {

                                         }
                                     });
                                     builder.show();


                                 }
                             });

                 }

            }

        }
    }

    @SuppressLint("Range")
    public String getfilenamefromuri(Uri filepath)
    {
        String result = null;
        if (filepath.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(filepath, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = filepath.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}