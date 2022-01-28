package com.embedsky.administrator.mycardemulation;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity  {
    public static Handler dataHandler;
    private TextView data;
    private EditText dataedit;
    public static StringBuilder sb=new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        data= (TextView) findViewById(R.id.data_tv);
        dataedit= (EditText) findViewById(R.id.data_edt);
        dataedit.setText(AccountStorage.GetAccount(this));
//        dataedit.addTextChangedListener(new AccountUpdater());
        dataHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==1){
                    data.setText(sb.toString());
                }
                super.handleMessage(msg);
            }
        };
        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = dataedit.getText().toString();
                AccountStorage.SetAccount(MainActivity.this, account);
            }
        });
    }


//    private class AccountUpdater implements TextWatcher {
//        @Override
//        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            // Not implemented.
//        }
//
//        @Override
//        public void onTextChanged(CharSequence s, int start, int before, int count) {
//            // Not implemented.
//        }
//
//        @Override
//        public void afterTextChanged(Editable s) {
//            String account = s.toString();
//            AccountStorage.SetAccount(MainActivity.this, account);
//        }
//    }

}
