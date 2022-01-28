package com.example.android.newParser;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mayong on 2022/1/26
 */
public class NdefMessageParser {
    public List<ParsedNdefRecord> parse(NdefMessage message) {
        return getRecords(message.getRecords());
    }

    public List<ParsedNdefRecord> getRecords(NdefRecord[] records) {
        List<ParsedNdefRecord> elements = new ArrayList<>();


        for (NdefRecord record : records) {
            if (TextRecord.isText(record)) {
                elements.add(TextRecord.parse(record));
            } else {
                elements.add(new ParsedNdefRecord() {
                    @Override
                    public String str() {
                        return new String(record.getPayload());
                    }
                });
            }
        }

        return elements;
    }

}
