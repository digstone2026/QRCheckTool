package com.example.qrchecktool;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    EditText etInput;
    TextView tvStatus, tvExtracted;
    LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // ✅ 顶部状态（超大+居中）
        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(42);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setPadding(30,50,30,50);
        tvStatus.setBackgroundColor(Color.GRAY);

        TextView close = new TextView(this);
        close.setText("❌");
        close.setTextSize(24);
        close.setPadding(20,20,20,20);
        close.setOnClickListener(v -> showExit());

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.addView(tvStatus,new LinearLayout.LayoutParams(0,-2,1));
        topBar.addView(close);

        etInput = new EditText(this);
        etInput.setHint("扫码输入");

        tvExtracted = new TextView(this);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        root.addView(topBar);
        root.addView(etInput);
        root.addView(tvExtracted);
        root.addView(container);

        scroll.addView(root);
        setContentView(scroll);

        etInput.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER &&
               event.getAction() == KeyEvent.ACTION_DOWN){
                process(etInput.getText().toString());
                return true;
            }
            return false;
        });
    }

    // ✅ 主流程
    private void process(String input){

        container.removeAllViews();

        if(input == null || input.isEmpty()){
            setStatus(false);
            return;
        }

        String extracted = extract(input);
        tvExtracted.setText("提取后内容：\n" + extracted);

        String[] parts = extracted.split("#");
        if(parts.length != 3){
            setStatus(false);
            addBlock("格式错误", extracted, "-", "必须3段",
                    false, "分隔错误");
            return;
        }

        String prefix = parts[0];

        String id = prefix.substring(0,1);
        String sku = prefix.substring(1,10);
        String batch = prefix.substring(10);

        String pdStr = parts[1];
        String ddStr = parts[2];

        Date today = new Date();

        // ✅ ID
        boolean idOK = id.equals("0");

        // ✅ SKU
        boolean skuOK = sku.matches("\\d{9}");

        // ✅ Batch
        boolean batchOK = batch.matches("[A-Za-z0-9]{1,15}");

        // ✅ Production Date
        Date pd = strictDate(pdStr);
        boolean pdOK = false;
        String pdErr = "";

        if(!pdStr.matches("^\\d{8}$")){
            pdErr = "必须8位数字";
        }else if(pd == null){
            pdErr = "非法日期";
        }else if(pd.after(today)){
            pdErr = "不能大于今天";
        }else{
            pdOK = true;
        }

        // ✅ ✅ ✅ Due Date（核心增强版）
        Date dd = null;
        boolean ddOK = false;
        String ddErr = "";

        if(!ddStr.matches("^\\d{8}$")){
            ddErr = "必须8位纯数字（无空格/无字符）";
        }else{
            dd = strictDate(ddStr);

            if(dd == null){
                ddErr = "非法日期";
            }else if(!dd.after(today)){
                ddErr = "必须大于今天";
            }else{
                ddOK = true;
            }
        }

        // ✅ 关系
        boolean relOK = pd!=null && dd!=null && pd.before(dd);

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK && relOK;

        setStatus(allOK);

        String line = "----------------------------------------";

        addBlock("Identification Number",id,"1","必须为0",
                idOK,idOK?"":"必须为0");

        addDivider(line);

        addBlock("SKU Number",sku,
                String.valueOf(sku.length()),
                "9位数字",
                skuOK,
                skuOK?"":"错误");

        addDivider(line);

        addBlock("Batch Number",batch,
                String.valueOf(batch.length()),
                "≤15位字母数字",
                batchOK,
                batchOK?"":"非法");

        addDivider(line);

        addBlock("Production Date",pdStr,
                String.valueOf(pdStr.length()),
                "YYYYMMDD ≤ today",
                pdOK,
                pdOK?"":pdErr);

        addDivider(line);

        addBlock("Due Date",ddStr,
                String.valueOf(ddStr.length()),
                "YYYYMMDD > today（且无多余字符）",
                ddOK,
                ddOK?"":ddErr);

        addDivider(line);

        addBlock("Date Relation","-","-",
                "PD < DD",
                relOK,
                relOK?"":"生产日期必须小于到期");
    }

    // ✅ UI块
    private void addBlock(String name,String value,String len,
                          String rule,boolean pass,String err){

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20,20,20,20);

        box.setBackgroundColor(pass ?
                Color.parseColor("#C8E6C9") :   // ✅ 绿色
                Color.parseColor("#FFCDD2"));   // ❌ 红色

        addText(box,"["+name+"]");
        addText(box,"二维码内容："+value);
        addText(box,"长度："+len);
        addText(box,"校验规则："+rule);
        addText(box,"结果："+(pass?"✅ PASS":"❌ FAIL"));

        if(!pass && err!=null && !err.isEmpty()){
            addText(box,"错误："+err);
        }

        container.addView(box);
    }

    private void addText(LinearLayout box,String txt){
        TextView tv = new TextView(this);
        tv.setText(txt);
        box.addView(tv);
    }

    private void addDivider(String line){
        TextView tv = new TextView(this);
        tv.setText(line);
        tv.setGravity(Gravity.CENTER);
        container.addView(tv);
    }

    private void setStatus(boolean pass){
        ToneGenerator t=new ToneGenerator(AudioManager.STREAM_MUSIC,100);

        if(pass){
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.parseColor("#00C853"));
            t.startTone(ToneGenerator.TONE_PROP_BEEP);
        }else{
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.parseColor("#D50000"));
            t.startTone(ToneGenerator.TONE_SUP_ERROR);
        }
    }

    private void showExit(){
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("是否退出？")
                .setPositiveButton("确认",(d,w)->finish())
                .setNegativeButton("取消",null)
                .show();
    }

    private String extract(String input){
        if(input.contains("cii1/")){
            String raw=input.substring(input.indexOf("cii1/")+5);
            return raw.replace("&","#");   // ✅ 不要 trim
        }
        return input.replace("&","#").trim();
    }

    // ✅ 严格日期
    private Date strictDate(String s){
        if(!s.matches("\\d{8}")) return null;
        try{
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
            sdf.setLenient(false);
            return sdf.parse(s);
        }catch(Exception e){
            return null;
        }
    }
}
