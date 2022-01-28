/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.cardreader;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.os.SystemClock;
import android.widget.Toast;

import com.example.android.common.AESHelper;
import com.example.android.common.logger.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
 * Reader mode can be invoked by calling NfcAdapter
 */
public class LoyaltyCardReader implements NfcAdapter.ReaderCallback {
    private static final String TAG = "LoyaltyCardReader";
    // AID for our loyalty card service.
    private static final String SAMPLE_LOYALTY_CARD_AID = "D276000085010100";//00A4040007D276000085010100
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String GET_DATA_APDU_HEADER = "00CA0000";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

    String gotData = "", finalGotData = "";

    long timeTaken = 0;

    // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<AccountCallback> mAccountCallback;

    public interface AccountCallback {
        public void onAccountReceived(String account);
    }

    public LoyaltyCardReader(AccountCallback accountCallback) {
        mAccountCallback = new WeakReference<AccountCallback>(accountCallback);
    }

    private NdefRecord createTextRecord(String text, byte[] id){
        byte[] textBytes;
        try {
            textBytes = text.getBytes("UTF-8");
            byte[] recordPayload = new byte[textBytes.length+1];
            System.arraycopy(
                    textBytes,
                    0,
                    recordPayload,
                    1,
                    textBytes.length
            );
            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, "U".getBytes(StandardCharsets.UTF_8), id, recordPayload);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Callback when a new tag is discovered by the system.
     *
     * <p>Communication with the card should take place here.
     *
     * @param tag Discovered tag
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "New tag discovered");
        // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
        // protocol.
        //
        // In order to communicate with a device using HCE, the discovered tag should be processed
        // using the IsoDep class.


        try {
            Ndef ndef = Ndef.get(tag);
            if(ndef != null) {
//                NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{NdefRecord.createApplicationRecord("com.wisdom.testnfc3_cardemulation")});
                NdefMessage ndefMessage = new NdefMessage(createTextRecord("aaaaaaaaaaa", "Wisdom".getBytes(StandardCharsets.UTF_8)));

                //获得写入大小
                int size = ndefMessage.toByteArray().length;

                //说明是NDEF标签,开始连接
                ndef.connect();
                //判断是否可写
                if (!ndef.isWritable()) {
                    Log.d("xmg", "当前设备不支持写入");
//                    Toast.makeText(this, "当前设备不支持写入", Toast.LENGTH_LONG).show();
                    return;
                }
                //判断大小
                if (ndef.getMaxSize() < size) {
                    Log.d("xmg", "容量太小了");
//                    Toast.makeText(this, "容量太小了", Toast.LENGTH_LONG).show();
                    return;
                }
                //写入
                try {
                    ndef.writeNdefMessage(ndefMessage);
                    Log.d("xmg", "写入成功");
//                    Toast.makeText(this, "写入成功", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


//        IsoDep isoDep = IsoDep.get(tag);
//        if (isoDep != null) {
//            try {
//                // Connect to the remote NFC device
//                isoDep.connect();
//                Log.i(TAG, "Timeout = " + isoDep.getTimeout());
//                isoDep.setTimeout(3600);
//                Log.i(TAG, "Timeout = " + isoDep.getTimeout());
//                Log.i(TAG, "MaxTransceiveLength = " + isoDep.getMaxTransceiveLength());
//
//                // Build SELECT AID command for our loyalty card service.
//                // This command tells the remote device which service we wish to communicate with.
//                Log.i(TAG, "Requesting remote AID: " + SAMPLE_LOYALTY_CARD_AID);
//                byte[] selCommand = BuildSelectApdu(SAMPLE_LOYALTY_CARD_AID);
//                // Send command to remote device
//                Log.i(TAG, "Sending: " + ByteArrayToHexString(selCommand));
//                byte[] result = isoDep.transceive(selCommand);
//                // If AID is successfully selected, 0x9000 is returned as the status word (last 2
//                // bytes of the result) by convention. Everything before the status word is
//                // optional payload, which is used here to hold the account number.
//                int resultLength = result.length;
//                byte[] statusWord = {result[resultLength-2], result[resultLength-1]};
//                byte[] payload = Arrays.copyOf(result, resultLength-2);
//                Log.i(TAG, "statusWord: " + ByteArrayToHexString(statusWord));
//                if (Arrays.equals(SELECT_OK_SW, statusWord)) {
//                    // The remote NFC device will immediately respond with its stored account number
//                    String accountNumber = new String(payload, "UTF-8");
//                    Log.i(TAG, "Received: " + accountNumber);
//                    // Inform CardReaderFragment of received account number
//                    /*if (true) {
//                        timeTaken = System.currentTimeMillis();
//                        while (!(gotData.contains("END"))) {
//                            byte[] getCommand = BuildGetDataApdu();
//                            Log.i(TAG, "Sending: " + ByteArrayToHexString(getCommand));
//                            result = isoDep.transceive(getCommand);
//                            resultLength = result.length;
//                            Log.i(TAG, "Received length : " + resultLength);
//                            byte[] statusWordNew = {result[resultLength - 2], result[resultLength - 1]};
//                            payload = Arrays.copyOf(result, resultLength - 2);
//                            if (Arrays.equals(SELECT_OK_SW, statusWordNew)) {
//                                gotData = new String(payload, "UTF-8");
//                                Log.i(TAG, "Received: " + gotData);
//                                finalGotData = finalGotData + gotData;
//                                Log.i(TAG, "Data transferred : " + finalGotData.length());
//                                Log.i(TAG, "Time taken: " + (System.currentTimeMillis() - timeTaken));
//
//                            }
//                        }
//                        mAccountCallback.get().onAccountReceived(gotData);
//
//                    }*/
//                    //mAccountCallback.get().onAccountReceived(accountNumber);
//
//                    /*String seedVal = "PRESHAREDKEY";
//                    String decodedString = null;
//                    try {
//                        decodedString = AESHelper.decrypt(seedVal, accountNumber);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Log.e("ARNAV", "failed to decrypt");
//                        decodedString = accountNumber;
//                    }*/
//                }else{
//                    logBytes("0 received: ", result);
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "Error communicating with card: " + e.toString());
//            }
//        }
    }

    private void logBytes(String tag, byte[] result){
        try {
            String received = new String(result, "UTF-8");
            Log.d(TAG, tag + received);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    /**
     * Build APDU for GET_DATA command. See ISO 7816-4.
     *
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildGetDataApdu() {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(GET_DATA_APDU_HEADER + "0FFF");
    }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
