/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/




package com.example.android.cardreader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;

import androidx.fragment.app.FragmentTransaction;

import com.example.android.common.activities.SampleActivityBase;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;
import com.example.android.newParser.NdefMessageParser;
import com.example.android.newParser.ParsedNdefRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link androidx.fragment.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase {

    public static final String TAG = "xmg";
    public static Context pContext;

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;


    private PendingIntent mPendingIntent = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0,intent, 0);

//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        CardReaderFragment fragment = new CardReaderFragment();
//        transaction.replace(R.id.sample_content_fragment, fragment);
//        transaction.commit();
        pContext = getApplicationContext();
//
//        Button btn_menu_log = findViewById(R.id.btn_menu_log);
//        ViewAnimator output = (ViewAnimator) findViewById(R.id.sample_output);
//        output.setDisplayedChild(1);
//        btn_menu_log.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                btn_menu_log.setText(mLogShown ? "hide_log" : "show_log");
//
//                mLogShown = !mLogShown;
//
//                if (mLogShown) {
//                    output.setDisplayedChild(1);
//                } else {
//                    output.setDisplayedChild(0);
//                }
//            }
//        });
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.d("xmg","=== onResume ===");
//        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
//        Log.d("xmg","=== onResume 1 ===");
//        if (nfc != null) {
//            Log.d("xmg","=== onResume 2 ===");
//            nfc.enableForegroundDispatch(this, mPendingIntent, null, null);
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
//        if (nfc != null) {
//            nfc.disableForegroundDispatch(this);
//        }
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        setIntent(intent);
//        Log.d("xmg","=== onNewIntent ===");
////        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
////            Parcelable[] parcelableArrayExtra = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
////            Log.d("xmg","=== onNewIntent 1 ===  Extra.length="+parcelableArrayExtra.length);
////            if(parcelableArrayExtra != null){
////                Log.d("xmg","=== onNewIntent 2 ===");
////                // Process the messages array.
////                parserNDEFMessage((NdefMessage)parcelableArrayExtra[0]);
////            }
////        }
//
//        handlePossibleNfcMessages(intent);
//    }


    @Override
    protected void onResume() {
        super.onResume();
        if (selectedVcardString != null) {
            setUpForgroundNdefPush();
        }
        enableForegroundNfcDispatch();
        handlePossibleNfcMessages(getIntent());
    }

    private void setUpForgroundNdefPush() {
//        if (!ApiAccessor.hasNfcSupport())
//            return;
        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter != null) {
            adapter.enableForegroundNdefPush(this, createNdefWithPhotoUrlForSelectedContact());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handlePossibleNfcMessages(intent);
    }

    @Override
    protected void onPause() {
//        if (ApiAccessor.hasNfcSupport()) {
            NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
            NfcAdapter adapter = manager.getDefaultAdapter();
            if (adapter != null) {
                adapter.disableForegroundDispatch(this);
            }
//        }
        super.onPause();
    }

    private void enableForegroundNfcDispatch() {

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (adapter == null)
            return;

        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Unable to speciy */* Mime Type", e);
        }
        IntentFilter[] intentFiltersArray = new IntentFilter[] {
                ndef
        };

        String[][] techListsArray = new String[][] {
                new String[] {
                        Ndef.class.getName()
                }, new String[] {
                NfcF.class.getName()
                // to read felica
        }
        };

        adapter.enableForegroundDispatch(this, intent, intentFiltersArray, techListsArray);
    }
    private String selectedVcardString = "123456789000";
    private NdefMessage createNdefWithPhotoUrlForSelectedContact() {
        return createNdefVCard(selectedVcardString);
    }
    public static NdefMessage createNdefVCard(String vcard) {
        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, MIME_TYPE_VCARD.getBytes(),
                new byte[] {}, vcard.getBytes());
        return new NdefMessage(new NdefRecord[] {
                record
        });
    }
    public static final String MIME_TYPE_VCARD = "text/x-vCard";
    private void handlePossibleNfcMessages(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] msgs = getNdefMessages(intent);
            if (msgs != null) {
                NdefMessage msg = msgs[0];
                final String vcardPayload = new String(msg.getRecords()[0].getPayload());
//                handleReceivedVCard(vcardPayload, EVENT_LABEL_NFC);
                Log.w(TAG, "vcardPayload:" + vcardPayload);
            }
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            // TODO refactor
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            try {
                Ndef ndef = Ndef.get(tagFromIntent);
                ndef.connect();
                NdefMessage msg = ndef.getNdefMessage();
                for (int i = 0; i < msg.getRecords().length; i++) {
                    NdefRecord record = msg.getRecords()[i];
                    String mime = new String(record.getType());
                    String payload = new String(record.getPayload());
                    if (MIME_TYPE_VCARD.toLowerCase().equals(mime.toLowerCase())) {
//                        handleReceivedVCard(payload, EVENT_LABEL_NFC_TAG);
                        Log.w(TAG, "payload:" + payload);
                    }
                    Log.d(TAG, record.toString());
                }
            } catch (IOException e) {
            } catch (FormatException e) {
            }
            Log.d(TAG, "tag:" + tagFromIntent);
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Log.d(TAG, "tech:");
            Parcelable tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Tag t = (Tag) tag;
            readTagUltralight(t);

                // http://ap.pitsquare.jp/pc/developers/

            // http://code.google.com/p/nfc-felica/source/browse/nfc-felica/branches/nfc-felica-2.3.3/src/net/kazzz/NFCFeliCaReader.java?r=34
            // intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            // intent.getParcelableExtra("android.nfc.extra.TAG");
            // ft.readWithoutEncryption((byte)0)
            // http://code.google.com/p/nfc-felica/source/browse/nfc-felica/trunk/nfc-felica-lib/src/net/kazzz/felica/FeliCaTag.java?r=21
        }
    }
    public String readTagUltralight(Tag tag) {
        MifareUltralight mifare = MifareUltralight.get(tag);
        try {
            mifare.connect();
            int size=mifare.PAGE_SIZE;
            byte[] payload = mifare.readPages(0);
            String result="page1："+ByteArrayToHexString(payload)+"\n"+"总容量："+String.valueOf(size)+"\n";
            //这里只读取了其中几个page
            byte[] payload1 = mifare.readPages(4);
            byte[] payload2 = mifare.readPages(8);
            byte[] payload3 = mifare.readPages(12);
            result+="page4:"+ByteArrayToHexString(payload1)+"\npage8:"+ByteArrayToHexString(payload2)+"\npage12："+ByteArrayToHexString(payload3)+"\n";
            //byte[] payload4 = mifare.readPages(16);
            //byte[] payload5 = mifare.readPages(20);
            return result;
            //+ new String(payload4, Charset.forName("US-ASCII"));
            //+ new String(payload5, Charset.forName("US-ASCII"));
        } catch (IOException e) {
            Log.e(TAG, "IOException while writing MifareUltralight message...",
                    e);
            return "读取失败！";
        } catch (Exception ee) {
            Log.e(TAG, "IOException while writing MifareUltralight message...",
                    ee);
            return "读取失败！";
        } finally {
            if (mifare != null) {
                try {
                    mifare.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing tag...", e);
                }
            }
        }
    }
    private String ByteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A",
                "B", "C", "D", "E", "F" };
        String out = "";
        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }
    NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {
                        record
                });
                msgs = new NdefMessage[] {
                        msg
                };
            }
        }
        return msgs;
    }
//    private void handleReceivedVCard(String vcardPayload, String receivedSourceLabel) {
//        tracker.trackEvent(EVENT_CATEGORY_CONTACTS, EVENT_ACTION_RECEIVE, receivedSourceLabel, 1);
//
//        // Consume this intent, so it won't do it again on next resume
//        setIntent(new Intent());
//
//        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//        vibrator.vibrate(100);
//
//        saveToHistory(vcardPayload);
//    }





    private void parserNDEFMessage(NdefMessage message) {
        Log.d("xmg","=== parserNDEFMessage ===");
        try {
            StringBuilder builder = new StringBuilder();
            List<ParsedNdefRecord> records = new NdefMessageParser().parse(message);
            int size = records.size();
            Log.d("xmg","size："+size);
            for (int i=0;i<size;i++) {
                ParsedNdefRecord record = records.get(i);
                String str = record.str();
                builder.append(str).append("\n");
            }

            Log.d("xmg","Message："+builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

//        mTvView.text = builder.toString();
    }

    /** Create a chain of targets that will receive log data */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");
    }
}
