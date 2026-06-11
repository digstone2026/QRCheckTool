package com.example.qrchecktool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    TextView tvStatus, tvVersion, tvExtracted;
    LinearLayout container;

    private String getScanData(Intent intent){
        String[] keys = {"barcode_string","data","barcode_data","scan_data"};
        for(String key : keys){
            String val = intent.getStringExtra(key);
            if(val != null && !val.isEmpty()){
                return val;
            }
        }
        return "";
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = getScanData(intent);
            if(data != null && !data.isEmpty()){
                process(data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(40);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTextColor(Color.WHITE);

        tvVersion = new TextView(this);
        tvVersion.setText("v1.0 | 20260610 | jazhao");
        tvVersion.setGravity(Gravity.CENTER);
        tvVersion.setTextColor(Color.WHITE);

        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.VERTICAL);
        statusBox.setPadding(20,40,20,40);
        statusBox.setBackgroundColor(Color.GRAY);
        statusBox.addView(tvStatus);
        statusBox.addView(tvVersion);

        TextView btnClose = new TextView(this);
        btnClose.setText("X");
        btnClose.setTextSize(24);
        btnClose.setPadding(20,20,20,20);
        btnClose.setOnClickListener(v -> showExit());

        LinearLayout topBar = new LinearLayout(this);
        topBar.addView(statusBox,new LinearLayout.LayoutParams(0,-2,1));
        topBar.addView(btnClose);

        tvExtracted = new TextView(this);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        root.addView(topBar);
        root.addView(tvExtracted);
        root.addView(container);

        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.ACTION_DECODE_DATA");
        filter.addAction("com.android.server.scannerservice.broadcast");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void process(String input){

        container.removeAllViews();

        String extracted = extract(input);
        tvExtracted.setText("Extracted:\n" + extracted);

        String[] parts = extracted.split("#");

        if(parts.length != 3){
            setStatus(false);
            addBlock("Format Error", extracted, "-", "3 parts required",
                    false, "Separator error");
            return;
        }

        String prefix = parts[0];

        String id = prefix.substring(0,1);
        String sku = prefix.substring(1,10);
        String batch = prefix.substring(10);

        String pdStr = parts[1];
        String ddStr = parts[2];

        Date today = new Date();

        boolean idOK = id.equals("0");
        String idErr = idOK ? "" : "Must be 0";

        boolean skuOK = sku.matches("\\d{9}");
        String skuErr = skuOK ? "" : "Must be 9 digits";

        boolean batchOK = batch.matches("[A-Za-z0-9]{1,15}");
        String batchErr = batchOK ? "" : "Invalid";

        boolean pdOK = false;
        String pdErr = "";
        Date pd = null;

        if(!pdStr.matches("^\\d{8}$")){
            pdErr = "Must be 8 digits";
        } else {
            pd = strictDate(pdStr);
            if(pd == null){
                pdErr = "Invalid date";
            } else if(pd.after(today)){
                pdErr = "Future not allowed";
            } else {
                pdOK = true;
            }
        }

        Date dd = null;
        boolean ddOK = true;
        StringBuilder ddErr = new StringBuilder();

        if(!ddStr.matches("^\\d{8}$")){
            ddOK = false;
            ddErr.append("- Must be 8 digits\n");
        } else {
            dd = strictDate(ddStr);
            if(dd == null){
                ddOK = false;
                ddErr.append("- Invalid date\n");
            } else if(!dd.after(today)){
                ddOK = false;
                ddErr.append("- Must be future\n");
            }
        }

        if(pd != null && dd != null && !pd.before(dd)){
            ddOK = false;
            ddErr.append("- PD must < DD\n");
        }

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK;

        setStatus(allOK);

        addBlock("ID",id,"1","Must 0",idOK,idErr);
        addDivider();

        addBlock("SKU",sku,String.valueOf(sku.length()),"9 digits",skuOK,skuErr);
        addDivider();

        addBlock("Batch",batch,String.valueOf(batch.length()),"<=15",batchOK,batchErr);
        addDivider();

        addBlock("Production Date",pdStr,String.valueOf(pdStr.length()),
                "<= Today",pdOK,pdErr);
        addDivider();

        addBlock("Due Date",ddStr,String.valueOf(ddStr.length()),
                "> Today AND PD<DD",ddOK,ddErr.toString());
    }

    private void setStatus(boolean pass){
        ToneGenerator t=new ToneGenerator(AudioManager.STREAM_MUSIC,100);
        LinearLayout parent=(LinearLayout) tvStatus.getParent();

        if(pass){
            tvStatus.setText("PASS");
            parent.setBackgroundColor(Color.GREEN);
            t.startTone(ToneGenerator.TONE_PROP_BEEP);
        } else {
            tvStatus.setText("FAIL");
            parent.setBackgroundColor(Color.RED);
            t.startTone(ToneGenerator.TONE_SUP_ERROR);
        }
    }

    private void addBlock(String name,String value,String len,String rule,boolean pass,String err){
        TextView tv=new TextView(this);
        tv.setText("["+name+"]\nValue:"+value+"\nLen:"+len+"\nRule:"+rule+
                "\n"+(pass?"PASS":"FAIL")+"\n"+err);
        tv.setPadding(20,20,20,20);
        tv.setBackgroundColor(pass?Color.parseColor("#C8E6C9"):Color.parseColor("#FFCDD2"));
        container.addView(tv);
    }

    private void addDivider(){
        TextView tv=new TextView(this);
        tv.setText("----------------------------------------");
        tv.setGravity(Gravity.CENTER);
        container.addView(tv);
    }

    private void showExit(){
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Quit?")
                .setPositiveButton("Yes",(d,w)->finish())
                .setNegativeButton("No",null)
                .show();
    }

    private String extract(String input){
        if(input.contains("cii1/")){
            String raw=input.substring(input.indexOf("cii1/")+5);
            return raw.replace("&","#");
        }
        return input.replace("&","#");
    }

    private Date strictDate(String s){
        try{
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
            sdf.setLenient(false);
            return sdf.parse(s);
        }catch(Exception e){
            return null;
        }
    }
}
