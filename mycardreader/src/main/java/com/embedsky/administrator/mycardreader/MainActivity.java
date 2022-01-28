package com.embedsky.administrator.mycardreader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity implements LoyaltyCardReader.AccountCallback {

    public static final String TAG = "MyCardReader";
    // Recommend NfcAdapter flags for reading from other Android devices. Indicates that this
    // activity is interested in NFC-A devices (including other Android devices), and that the
    // system should not check for the presence of NDEF-formatted data (e.g. Android Beam).
    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
    public LoyaltyCardReader mLoyaltyCardReader;
    private TextView mAccountField;
    public static StringBuilder sb=new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAccountField= (TextView) findViewById(R.id.data_tv);
        mLoyaltyCardReader = new LoyaltyCardReader(this);

        enableReaderMode();
    }


    @Override
    public void onPause() {
        super.onPause();
        disableReaderMode();
    }

    @Override
    public void onResume() {
        super.onResume();
        enableReaderMode();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void enableReaderMode() {
        Log.i(TAG, "Enabling reader mode");
        Activity activity = this;
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.enableReaderMode(activity, mLoyaltyCardReader, READER_FLAGS, null);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void disableReaderMode() {
        Log.i(TAG, "Disabling reader mode");
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc != null) {
            nfc.disableReaderMode(this);
        }
    }

    @Override
    public void onAccountReceived() {
        // This callback is run on a background thread, but updates to UI elements must be performed
        // on the UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAccountField.setText(sb.toString());
            }
        });
    }
}
