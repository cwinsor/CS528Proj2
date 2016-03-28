package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bignerdranch.android.criminalintent.database.MyStatic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String TAG = "CrimeFragment";

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO= 2;

    private Crime mCrime;
    private List<File> mPhotoFileList;

    private Integer currentPhotoNum = 0;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckbox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;
    private final Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    private Button mDeleteImagesButton;

    private List<ImageView> mPhotoViewList;

    public static CrimeFragment newInstance(UUID crimeId) {

        Log.v(TAG, "newInstance");

        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate");

        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);

        mPhotoFileList = new ArrayList<File>();
        mPhotoFileList.add(0, CrimeLab.get(getActivity()).getPhotoFile(mCrime, 0));
        mPhotoFileList.add(1, CrimeLab.get(getActivity()).getPhotoFile(mCrime, 1));
        mPhotoFileList.add(2, CrimeLab.get(getActivity()).getPhotoFile(mCrime, 2));
        mPhotoFileList.add(3, CrimeLab.get(getActivity()).getPhotoFile(mCrime, 3));
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.v(TAG, "onPause");

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.v(TAG, "--> onCreateView");

        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment
                        .newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckbox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckbox.setChecked(mCrime.isSolved());
        mSolvedCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mReportButton = (Button)v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button)v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);


        boolean canTakePhoto =
              /*  (mPhotoFileList.get(0) != null) &&
                        (mPhotoFileList.get(1) != null) &&
                        (mPhotoFileList.get(2) != null) &&
                        (mPhotoFileList.get(3) != null) && */
                imageIntent.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if (canTakePhoto) {
            targetThePhotoIntent();
        }

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(imageIntent, REQUEST_PHOTO);
            }
        });

        mPhotoViewList = new ArrayList<ImageView>();
        mPhotoViewList.add(0, (ImageView) v.findViewById(R.id.crime_photo_0));
        mPhotoViewList.add(1, (ImageView) v.findViewById(R.id.crime_photo_1));
        mPhotoViewList.add(2, (ImageView) v.findViewById(R.id.crime_photo_2));
        mPhotoViewList.add(3, (ImageView) v.findViewById(R.id.crime_photo_3));

        mDeleteImagesButton = (Button)v.findViewById(R.id.delete_button);
        mDeleteImagesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                for (int i = 0; i < getActivity().getResources().getInteger(R.integer.num_pics); i++) {
                    boolean deleted = mPhotoFileList.get(i).delete();
                    MyStatic.Log(TAG, "...deleted returns " + deleted);
                }
                currentPhotoNum = 0;
                initialPhotoView();
            }
        });

        initialPhotoView();
        return v;
    }

    private void targetThePhotoIntent() {
        Uri uri = Uri.fromFile(mPhotoFileList.get(currentPhotoNum));
        imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        MyStatic.Log(TAG,"here");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.v(TAG, "onActivityResult");

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            // values for.
            String[] queryFields = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME,
            };
            // Perform your query - the contactUri is like a "where"
            // clause here
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor c = resolver
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();

                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            updatePhotoView(currentPhotoNum);

            // point intent to the next photo
            currentPhotoNum = (currentPhotoNum + 1) % getActivity().getResources().getInteger(R.integer.num_pics);
            targetThePhotoIntent();

        }
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDate().toString());
    }

    private String getCrimeReport() {

        Log.v(TAG, "getCrimeReport");

        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void initialPhotoView() {
        for (int i=0; i<getActivity().getResources().getInteger(R.integer.num_pics); i++) {
            updatePhotoView(i);
        }
    }

    private void updatePhotoView(Integer i) {
        if (mPhotoFileList.get(i) == null) {
            MyStatic.Fatal(TAG, "error 1234 mPhotoFileList has a null");
        }
        if (!mPhotoFileList.get(i).exists()) {
            MyStatic.Log(TAG, "note - file does NOT exist");
            mPhotoViewList.get(i).setImageBitmap(null);
        }
        else {
            MyStatic.Log(TAG, "note - file exists");
            String path =  mPhotoFileList.get(i).getPath();
            FragmentActivity fragmentActivity = getActivity();
            Bitmap bitmap = PictureUtils.getScaledBitmap(path, fragmentActivity);

            int orientation = -1;
            try {
                ExifInterface ei = new ExifInterface(path);
                 orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException ioException) {
                MyStatic.Fatal(TAG,ioException.toString());
            }
          MyStatic.Log(TAG,"orientation = " + orientation);
            float angle = exifToAngle(orientation);
            Bitmap bitmap2 = rotateImage(bitmap, angle);



            mPhotoViewList.get(i).setImageBitmap(bitmap2);
        }
    }


    // http://www.programcreek.com/java-api-examples/index.php?api=android.media.ExifInterface
    // http://stackoverflow.com/questions/14066038/why-image-captured-using-camera-intent-gets-rotated-on-some-devices-in-android
    public float exifToAngle(int o) {

        if (o == ExifInterface.ORIENTATION_NORMAL) {
            return 0;
        } else if (o == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (o == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (o == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        } else {
            return 0;
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Bitmap retVal;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        return retVal;
    }


}
