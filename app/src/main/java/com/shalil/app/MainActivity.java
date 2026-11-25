package com.shalil.app;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private StoreDb helper;
    private LinearLayout content;
    private final LinkedHashMap<Long,Integer> cart = new LinkedHashMap<>();
    private boolean toman=false;
    private int paperWidth=58;
    private String pendingBarcode="";
    private String pendingPhotoUri="";
    private final ActivityResultLauncher<String[]> photoPicker=registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
        if(uri!=null){ try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){} pendingPhotoUri=uri.toString(); toast("تصویر انتخاب شد؛ با ذخیره کالا ثبت می‌شود"); }
    });
    private boolean scanForInvoice=false;
    private TextInputEditText activeBarcodeField;
    private final ActivityResultLauncher<ScanOptions> scanner=registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents()!=null){
            pendingBarcode=result.getContents();
            if(scanForInvoice){ scanForInvoice=false; Cursor p=db().rawQuery("SELECT id FROM products WHERE code=? LIMIT 1",new String[]{pendingBarcode}); if(p.moveToFirst()){long id=p.getLong(0);cart.put(id,cart.getOrDefault(id,0)+1);p.close();invoice();}else{p.close();new AlertDialog.Builder(this).setMessage("No product has this barcode. Add it?").setPositiveButton("Add",(d,w)->productDialog(-1,"",pendingBarcode,0,0,0)).setNegativeButton("Cancel",null).show();}}
            else if(activeBarcodeField!=null){activeBarcodeField.setText(pendingBarcode);activeBarcodeField.setSelection(activeBarcodeField.getText().length());activeBarcodeField=null;}
            else productDialog(-1,"",pendingBarcode,0,0,0);
        } else { activeBarcodeField=null; }
    });
    private final ActivityResultLauncher<String> exportPicker=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> { if(uri!=null) backupTo(uri); });
    private final ActivityResultLauncher<String[]> importPicker=registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if(uri!=null) restoreFrom(uri); });

    @Override public void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_main);
        helper=new StoreDb(); content=findViewById(R.id.content_area);
        toman=getPreferences(0).getBoolean("toman",false);
        paperWidth=getPreferences(0).getInt("paper",58);
        findViewById(R.id.nav_home).setOnClickListener(v->dashboard());
        findViewById(R.id.nav_products).setOnClickListener(v->products());
        findViewById(R.id.nav_invoice).setOnClickListener(v->invoice());
        findViewById(R.id.nav_reports).setOnClickListener(v->reports());
        findViewById(R.id.nav_settings).setOnClickListener(v->settings());
        dashboard();
    }
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(getColor(R.color.ink));t.setPadding(3,6,3,6);return t;}
    private void heading(String s){
        content.animate().cancel();content.setAlpha(0f);content.setTranslationY(14f);content.removeAllViews();
        TextView h=label(s,23);h.setTypeface(null,1);content.addView(h);
        content.animate().alpha(1f).translationY(0f).setDuration(240).start();
    }
    private MaterialButton button(String s,Runnable r){MaterialButton b=new MaterialButton(this);b.setText(s);b.setOnClickListener(v->r.run());return b;}
    private void card(String title,String detail,Runnable click){
        MaterialCardView c=new MaterialCardView(this);c.setRadius(22);c.setCardElevation(3);c.setStrokeWidth(1);c.setStrokeColor(getColor(R.color.teal_light));c.setCardBackgroundColor(getColor(R.color.surface));
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(16,12,16,12);box.addView(label(title,17));box.addView(label(detail,14));c.addView(box);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,7,0,9);content.addView(c,p);c.setAlpha(0f);c.setTranslationY(10f);c.animate().alpha(1f).translationY(0f).setDuration(260).start();if(click!=null)c.setOnClickListener(v->click.run());
    }
    private String money(long n){return NumberFormat.getInstance(new Locale("fa","IR")).format(toman?n/10:n)+(toman?" تومان":" ریال");}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private SQLiteDatabase db(){return helper.getWritableDatabase();}
    private void dashboard(){
        heading("داشبورد فروشگاه");
        card("فاکتور جدید","ثبت فروش، محاسبه مبلغ و کاهش خودکار موجودی",this::invoice);
        card("مدیریت کالا","افزودن، ویرایش، حذف و جست‌وجوی کالا",this::products);
        card("گزارش فروش","فاکتورهای امروز، هفته و ماه",this::reports);
        Cursor c=db().rawQuery("SELECT COUNT(*),COALESCE(SUM(stock),0) FROM products",null);c.moveToFirst();
        card("وضعیت انبار","تعداد کالا: "+c.getInt(0)+"   |   تعداد موجودی: "+c.getInt(1),null);c.close();
    }
    private TextInputEditText field(String hint,String value,int type){TextInputEditText e=new TextInputEditText(this);e.setHint(hint);e.setText(value);e.setInputType(type);e.setSingleLine(true);return e;}
    private long num(TextInputEditText e){try{return Math.max(0,Long.parseLong(e.getText().toString().trim()));}catch(Exception ex){return 0;}}
    private void products(){
        heading("مدیریت کالاها");
        content.addView(button("+ افزودن کالا",()->productDialog(-1,"","",0,0,0)));
        content.addView(button("اسکن بارکد با دوربین",this::scanBarcode));
        TextInputEditText search=field("جست‌وجوی نام یا بارکد","",InputType.TYPE_CLASS_TEXT);content.addView(search);
        LinearLayout results=new LinearLayout(this);results.setOrientation(1);content.addView(results);
        Runnable load=()->{
            results.removeAllViews();String q=search.getText()==null?"":search.getText().toString();
            Cursor c=db().rawQuery("SELECT id,name,code,stock,buy,price,photo FROM products WHERE name LIKE ? OR code LIKE ? ORDER BY name",new String[]{"%"+q+"%","%"+q+"%"});
            while(c.moveToNext()){
                long id=c.getLong(0),buy=c.getLong(4),price=c.getLong(5);String n=c.getString(1),code=c.getString(2),photo=c.getString(6);int stock=c.getInt(3);
                MaterialCardView card=new MaterialCardView(this);card.setRadius(14);LinearLayout box=new LinearLayout(this);box.setPadding(12,8,12,8);box.setOrientation(1);
                if(photo!=null&&!photo.isEmpty()){try{ImageView thumb=new ImageView(this);thumb.setImageURI(Uri.parse(photo));thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(92,92);ip.gravity=Gravity.CENTER_HORIZONTAL;box.addView(thumb,ip);}catch(Exception ignored){}}
                box.addView(label("🍑  "+n,17));box.addView(label("بارکد: "+(code==null?"—":code)+" | موجودی: "+stock,13));
                box.addView(label("خرید: "+money(buy)+" | فروش: "+money(price),13));
                LinearLayout actions=new LinearLayout(this);
                actions.addView(button("ویرایش",()->productDialog(id,n,code==null?"":code,stock,buy,price)),new LinearLayout.LayoutParams(0,-2,1));
                actions.addView(button("حذف",()->new AlertDialog.Builder(this).setMessage("کالا حذف شود؟").setNegativeButton("خیر",null).setPositiveButton("حذف",(d,w)->{db().delete("products","id=?",new String[]{""+id});products();}).show()),new LinearLayout.LayoutParams(0,-2,1));
                box.addView(actions);card.addView(box);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,5,0,5);results.addView(card,lp);
            }c.close();
        };
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){load.run();}public void afterTextChanged(android.text.Editable e){}});
        load.run();
    }
    private LinearLayout labeledRow(String caption, View input, int inputWeight){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label(caption,14);
        title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        title.setMinWidth(dp(92));
        row.addView(title,new LinearLayout.LayoutParams(dp(104),-2));
        row.addView(input,new LinearLayout.LayoutParams(0,-2,inputWeight));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
        rp.setMargins(0,dp(4),0,dp(4));
        return row;
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private void productDialog(long id,String name,String code,int stock,long buy,long price){
        pendingPhotoUri="";
        LinearLayout f=new LinearLayout(this);f.setPadding(dp(16),dp(8),dp(16),dp(8));f.setOrientation(LinearLayout.VERTICAL);
        f.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextInputEditText n=field("",name,InputType.TYPE_CLASS_TEXT);
        TextInputEditText bc=field("",code,InputType.TYPE_CLASS_TEXT);
        TextInputEditText st=field("",String.valueOf(stock),InputType.TYPE_CLASS_NUMBER);
        TextInputEditText bp=field("",String.valueOf(buy),InputType.TYPE_CLASS_NUMBER);
        TextInputEditText sp=field("",String.valueOf(price),InputType.TYPE_CLASS_NUMBER);
        n.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);bc.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        st.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);bp.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);sp.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        f.addView(labeledRow("نام محصول",n,1));
        LinearLayout barcodeRow=new LinearLayout(this);
        barcodeRow.setOrientation(LinearLayout.HORIZONTAL);barcodeRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        barcodeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView barcodeLabel=label("بارکد",14);barcodeLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        barcodeRow.addView(barcodeLabel,new LinearLayout.LayoutParams(dp(104),-2));
        MaterialButton scan=button("▣",()->{activeBarcodeField=bc;ScanOptions o=new ScanOptions();o.setPrompt("بارکد را روبه‌روی دوربین بگیرید");o.setBeepEnabled(false);o.setOrientationLocked(true);scanner.launch(o);});
        scan.setContentDescription("اسکن بارکد با دوربین");scan.setMinWidth(dp(48));
        barcodeRow.addView(scan,new LinearLayout.LayoutParams(dp(52),dp(48)));
        barcodeRow.addView(bc,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams brp=new LinearLayout.LayoutParams(-1,-2);brp.setMargins(0,dp(4),0,dp(4));f.addView(barcodeRow,brp);
        f.addView(labeledRow("موجودی",st,1));
        f.addView(labeledRow("قیمت محصول (خرید)",bp,1));
        f.addView(labeledRow("قیمت فروش",sp,1));
        MaterialButton photo=button("🖼 انتخاب تصویر کالا (اختیاری)",()->photoPicker.launch(new String[]{"image/*"}));f.addView(photo);
        new AlertDialog.Builder(this).setTitle(id<0?"افزودن کالا":"ویرایش کالا").setView(f).setNegativeButton("لغو",(d,w)->activeBarcodeField=null).setPositiveButton("ذخیره",(d,w)->{
            activeBarcodeField=null;
            if(n.getText()==null||n.getText().toString().trim().isEmpty()){toast("نام محصول الزامی است");return;}
            ContentValues v=new ContentValues();v.put("name",n.getText().toString().trim());v.put("code",bc.getText().toString());v.put("stock",num(st));v.put("buy",num(bp));v.put("price",num(sp));if(!pendingPhotoUri.isEmpty())v.put("photo",pendingPhotoUri);
            if(id<0)db().insert("products",null,v);else db().update("products",v,"id=?",new String[]{""+id});products();
        }).show();
    }
    private void scanBarcode(){
        ScanOptions o=new ScanOptions();o.setPrompt("بارکد کالا را روبه‌روی دوربین بگیرید");o.setBeepEnabled(false);o.setOrientationLocked(true);scanner.launch(o);
    }
    private void invoice(){
        heading("فاکتور فروش جدید");
        content.addView(button("اسکن بارکد",()->{
            ScanOptions o=new ScanOptions();o.setPrompt("بارکد محصول را اسکن کنید");o.setBeepEnabled(false);
            scanForInvoice=true; scanner.launch(o);
        }));
        Cursor c=db().rawQuery("SELECT id,name,stock,price FROM products WHERE stock>0 ORDER BY name",null);
        while(c.moveToNext()){
            long id=c.getLong(0),price=c.getLong(3);String name=c.getString(1);int stock=c.getInt(2);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(label(name+"\nموجودی "+stock+" | "+money(price),14),new LinearLayout.LayoutParams(0,-2,1));
            row.addView(button("افزودن",()->{cart.put(id,cart.getOrDefault(id,0)+1);invoice();}));content.addView(row);
        }c.close();
        content.addView(label("اقلام فاکتور",18));long total=0;
        for(Map.Entry<Long,Integer> e:cart.entrySet()){
            Cursor p=db().rawQuery("SELECT name,price FROM products WHERE id=?",new String[]{""+e.getKey()});
            if(p.moveToFirst()){long line=p.getLong(1)*e.getValue();total+=line;card(p.getString(0)+" × "+e.getValue(),money(line),null);}p.close();
        }
        card("جمع کل",money(total),null);
        content.addView(button("ثبت و بستن فاکتور",this::finishInvoice));
        content.addView(button("پاک کردن فاکتور",()->{cart.clear();invoice();}));
    }
    private void finishInvoice(){
        if(cart.isEmpty()){toast("فاکتور خالی است");return;}SQLiteDatabase w=db();w.beginTransaction();
        try{
            long total=0;for(Map.Entry<Long,Integer> e:cart.entrySet()){
                Cursor p=w.rawQuery("SELECT price,stock FROM products WHERE id=?",new String[]{""+e.getKey()});
                if(!p.moveToFirst()||p.getInt(1)<e.getValue()){p.close();throw new Exception("موجودی کالا کافی نیست");}
                total+=p.getLong(0)*e.getValue();p.close();
            }
            ContentValues s=new ContentValues();s.put("total",total);s.put("created",System.currentTimeMillis());long sid=w.insertOrThrow("sales",null,s);
            for(Map.Entry<Long,Integer> e:cart.entrySet()){
                Cursor p=w.rawQuery("SELECT price FROM products WHERE id=?",new String[]{""+e.getKey()});p.moveToFirst();long price=p.getLong(0);p.close();
                ContentValues l=new ContentValues();l.put("sale_id",sid);l.put("product_id",e.getKey());l.put("qty",e.getValue());l.put("price",price);w.insert("sale_lines",null,l);
                w.execSQL("UPDATE products SET stock=stock-? WHERE id=?",new Object[]{e.getValue(),e.getKey()});
            }
            w.setTransactionSuccessful();cart.clear();toast("فاکتور ثبت شد");dashboard();
        }catch(Exception ex){toast(ex.getMessage());}finally{w.endTransaction();}
    }
    private void reports(){
        heading("گزارش فروش");
        long now=System.currentTimeMillis(),day=86400000L;
        reportRange("امروز",now-day);reportRange("۷ روز اخیر",now-7*day);reportRange("۳۰ روز اخیر",now-30*day);
        Cursor c=db().rawQuery("SELECT id,total,created FROM sales ORDER BY created DESC",null);
        while(c.moveToNext()){final long saleId=c.getLong(0);String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(c.getLong(2)));card("فاکتور "+saleId,dt+" | "+money(c.getLong(1)),()->showSale(saleId));}
        c.close();
    }
    private void reportRange(String title,long from){
        Cursor c=db().rawQuery("SELECT COUNT(*),COALESCE(SUM(total),0) FROM sales WHERE created>=?",new String[]{""+from});c.moveToFirst();
        card(title,"تعداد فاکتور: "+c.getInt(0)+" | فروش: "+money(c.getLong(1)),null);c.close();
    }
    private void showSale(long id){
        Cursor c=db().rawQuery("SELECT p.name,l.qty,l.price FROM sale_lines l LEFT JOIN products p ON p.id=l.product_id WHERE l.sale_id=?",new String[]{""+id});
        StringBuilder b=new StringBuilder();while(c.moveToNext())b.append(c.getString(0)).append(" × ").append(c.getInt(1)).append("  ").append(money(c.getLong(2)*c.getInt(1))).append("\n");c.close();
        new AlertDialog.Builder(this).setTitle("جزئیات فاکتور "+id).setMessage(b.toString()).setPositiveButton("چاپ", (d,w)->printReceipt(id)).setNegativeButton("بستن",null).show();
    }
    private void settings(){
        heading("تنظیمات");
        content.addView(label("واحد پول",17));content.addView(button(toman?"تومان (تغییر به ریال)":"ریال (تغییر به تومان)",()->{toman=!toman;getPreferences(0).edit().putBoolean("toman",toman).apply();settings();}));
        content.addView(label("عرض کاغذ چاپگر",17));content.addView(button(paperWidth+" میلی‌متر (تغییر ۵۸ / ۸۰)",()->{paperWidth=paperWidth==58?80:58;getPreferences(0).edit().putInt("paper",paperWidth).apply();settings();}));
        content.addView(button("پشتیبان‌گیری از همه اطلاعات",()->exportPicker.launch("shalil-backup.db")));
        content.addView(button("بازیابی پشتیبان",()->importPicker.launch(new String[]{"application/octet-stream","application/x-sqlite3","*/*"})));
        content.addView(button("چاپگر بلوتوث",this::choosePrinter));
        card("ذخیره‌سازی","اطلاعات فروشگاه روی همین دستگاه و بدون نیاز به اینترنت نگهداری می‌شود.",null);
    }
    private void backupTo(Uri uri){
        try{helper.close();try(InputStream in=new FileInputStream(getDatabasePath("shalil.db"));OutputStream out=getContentResolver().openOutputStream(uri)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}helper=new StoreDb();toast("فایل پشتیبان ذخیره شد");}
        catch(Exception e){helper=new StoreDb();toast("خطا در پشتیبان‌گیری: "+e.getMessage());}
    }
    private void restoreFrom(Uri uri){
        try{helper.close();File target=getDatabasePath("shalil.db");try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(target)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}helper=new StoreDb();dashboard();toast("پشتیبان بازیابی شد");}
        catch(Exception e){helper=new StoreDb();toast("فایل پشتیبان معتبر نیست: "+e.getMessage());}
    }
    private void choosePrinter(){
        BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();if(a==null){toast("بلوتوث در این دستگاه موجود نیست");return;}
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},81);return;}
        try{
            Set<BluetoothDevice> devices=a.getBondedDevices();if(devices.isEmpty()){toast("ابتدا چاپگر را از تنظیمات بلوتوث گوشی جفت کنید");return;}
            List<BluetoothDevice> list=new ArrayList<>(devices);String[] names=new String[list.size()];for(int i=0;i<list.size();i++)names[i]=list.get(i).getName()+"\n"+list.get(i).getAddress();
            new AlertDialog.Builder(this).setTitle("انتخاب چاپگر").setItems(names,(d,w)->sendPrint(list.get(w))).show();
        }catch(SecurityException e){toast("مجوز بلوتوث لازم است");}
    }
    private void printReceipt(long saleId){
        BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();if(a==null){toast("بلوتوث موجود نیست");return;}
        try{if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},81);return;}
            Set<BluetoothDevice> ds=a.getBondedDevices();List<BluetoothDevice> list=new ArrayList<>(ds);if(list.isEmpty()){toast("چاپگر جفت‌شده پیدا نشد");return;}
            String[] names=new String[list.size()];for(int i=0;i<list.size();i++)names[i]=list.get(i).getName();
            new AlertDialog.Builder(this).setTitle("چاپ فاکتور").setItems(names,(d,w)->sendReceipt(list.get(w),saleId)).show();
        }catch(Exception e){toast("خطای بلوتوث: "+e.getMessage());}
    }
    private void sendPrint(BluetoothDevice device){toast("چاپگر انتخاب شد: "+device.getName()+"؛ برای چاپ فاکتور از گزارش اقدام کنید");}
    private void sendReceipt(BluetoothDevice device,long saleId){
        new Thread(()->{BluetoothSocket socket=null;try{
            socket=device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();
            StringBuilder out=new StringBuilder("\n             SHALIL / شلیل\n--------------------------------\n");
            Cursor c=db().rawQuery("SELECT p.name,l.qty,l.price FROM sale_lines l LEFT JOIN products p ON p.id=l.product_id WHERE l.sale_id=?",new String[]{""+saleId});
            while(c.moveToNext())out.append(c.getString(0)).append(" x").append(c.getInt(1)).append("  ").append(c.getLong(2)*c.getInt(1)).append("\n");c.close();
            Cursor s=db().rawQuery("SELECT total FROM sales WHERE id=?",new String[]{""+saleId});if(s.moveToFirst())out.append("--------------------------------\nTOTAL: ").append(s.getLong(0)).append(" RIAL\n");s.close();
            out.append("\n\n\n");socket.getOutputStream().write(out.toString().getBytes("UTF-8"));socket.getOutputStream().flush();
            runOnUiThread(()->toast("فاکتور به چاپگر ارسال شد"));
        }catch(Exception e){runOnUiThread(()->toast("چاپ انجام نشد: "+e.getMessage()));}finally{try{if(socket!=null)socket.close();}catch(Exception ignored){}}
        }).start();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);}
    private class StoreDb extends SQLiteOpenHelper{
        StoreDb(){super(MainActivity.this,"shalil.db",null,2);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,code TEXT,stock INTEGER NOT NULL,buy INTEGER NOT NULL,price INTEGER NOT NULL,photo TEXT)");
            d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,total INTEGER NOT NULL,created INTEGER NOT NULL)");
            d.execSQL("CREATE TABLE sale_lines(id INTEGER PRIMARY KEY AUTOINCREMENT,sale_id INTEGER,product_id INTEGER,qty INTEGER,price INTEGER)");
        }
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV){if(oldV<2)d.execSQL("ALTER TABLE products ADD COLUMN photo TEXT");}
    }
}