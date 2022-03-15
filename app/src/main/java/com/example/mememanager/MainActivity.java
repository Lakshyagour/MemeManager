package com.example.mememanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PICKFILE_REQUEST_CODE = 102;
    private TextView textView;
    private String mCurrentPhotoPath;
    private Uri mCurrentPhotoUri;
    private InputImage mImage;
    private ProgressDialog mProgressDialog;
    private ImageView imageView;
    private int SELECT_IMAGE=100;
    private int SELECT_CAMERA=101;
    private TextRecognizer recognizer;
    private ArrayList<String> f = new ArrayList<String>();// list of file paths
    private File[] mFile;
    private List<String> ImageExtensions = Arrays.asList( ".JPG", ".JPE", ".BMP", ".GIF", ".PNG");
    private Handler hdlr = new Handler();
    private DatabaseHandler db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text1);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        db = new DatabaseHandler(this);
        checkPermission();
        Log.e(TAG, String.valueOf(Environment.getExternalStorageDirectory()));
        this.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
                //dispatchTakePictureIntent();
                // imageChooser();
                selectFolder();
            }
        });
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 122);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG,ex.getMessage());
            }
            if (photoFile != null) {
                mCurrentPhotoUri = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
                startActivityForResult(takePictureIntent, SELECT_CAMERA);
            }
        }
    }

    public String getImageFilePath(Uri uri) {
        File file = new File(uri.getPath());
        String[] filePath = file.getPath().split(":");
        String image_id = filePath[filePath.length - 1];

        Cursor cursor = getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[]{image_id}, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
            return imagePath;
        }
        return null;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    void imageChooser() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Picture"),SELECT_IMAGE );
    }

    private void selectFolder(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), PICKFILE_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                startOCR();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Result canceled.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Activity result failed.", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode==SELECT_IMAGE){
            if(resultCode == Activity.RESULT_OK){
                mCurrentPhotoUri = data.getData();
                //imageView.setImageURI(uri);
                mCurrentPhotoPath = getImageFilePath(mCurrentPhotoUri);
                Log.e(TAG,mCurrentPhotoPath);
                startOCR();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Result canceled.", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getApplicationContext(), "Activity result failed.", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == PICKFILE_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                Uri uri = data.getData();
                File file = new File(uri.getPath());//create path from uri
                final String[] split = file.getPath().split(":");//split the path.
                String folderPath = split[1];
                Log.e(TAG,folderPath);
                Toast.makeText(getApplicationContext(), folderPath, Toast.LENGTH_SHORT).show();
                String path = Environment.getExternalStorageDirectory().toString()+"/"+folderPath;
                Log.e("Files", "Path: " + path);
                File directory = new File(path);
                mFile = directory.listFiles();
                Log.e("Files", "Size: "+ mFile.length);
                startOCR();

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Result canceled.", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(getApplicationContext(), "Activity result failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void startOCR(){
        Log.e(TAG,"START OCR >>>>>>>>>>>>>>>");
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle("Processing Images");
                mProgressDialog.setMessage("Doing OCR");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setProgress(0);
                mProgressDialog.show();
            } else {
                mProgressDialog.show();
            }
            new Thread(new Runnable() {
                public void run() {
                    InputImage image;
                    Log.e(TAG,"START RUNNING >>>>>>>>>>>");
                    int counter = 0;
                    int totalImageFiles = 0;
                    for(File f : mFile) {
                        if(f.getAbsolutePath().endsWith(".png") || f.getAbsolutePath().endsWith(".webp") || f.getAbsolutePath().endsWith(".jpg") || f.getAbsolutePath().endsWith(".jpeg"))
                            totalImageFiles+=1;
                    }
                    Log.e(TAG,"Total Image Files ======== " + totalImageFiles);
                    mProgressDialog.setMax(totalImageFiles);
                    for(File f : mFile) {
                        if(f.getAbsolutePath().endsWith(".png") || f.getAbsolutePath().endsWith(".webp") || f.getAbsolutePath().endsWith(".jpg") || f.getAbsolutePath().endsWith(".jpeg")) {
                            counter+=1;
                            Log.e(TAG, "FILE" + String.valueOf(counter));
                            Uri uri = Uri.fromFile(new File(f.getAbsolutePath()));
                            try {
                                image = InputImage.fromFilePath(getApplicationContext(), uri);
                                final int finalCounter = counter;
                                final int finalTotalImageFiles = totalImageFiles;
                                final String finalpath = f.getAbsolutePath();
                                Task<Text> result =
                                        recognizer.process(image)
                                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                                    @Override
                                                    public void onSuccess(Text visionText) {
                                                        Log.e(TAG, "FILE" + String.valueOf(finalCounter) + "SUCCESS");
                                                        textView.append(visionText.getText() +"\n");
                                                        mProgressDialog.setProgress(finalCounter);
                                                        db.addText(new ImageTextData(finalpath, visionText.getText()));
                                                        if(finalCounter == finalTotalImageFiles) {
                                                            mProgressDialog.dismiss();
                                                            List<ImageTextData> data = db.getAllData();
                                                            for (ImageTextData cn : data) {
                                                                String log = "File Path: " + cn.getImgPath() + " ,File Text: " + cn.getContainsText() ;
                                                                Log.e(TAG, log);
                                                            }
                                                        }
                                                    }
                                                })
                                                .addOnFailureListener(
                                                        new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.e(TAG, e.getMessage());
                                                                Log.e(TAG, "FILE" + String.valueOf(finalCounter) + "FAILED");
                                                                mProgressDialog.setProgress(finalCounter);
                                                                if(finalCounter == finalTotalImageFiles)
                                                                    mProgressDialog.dismiss();
                                                            }
                                                        });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
}