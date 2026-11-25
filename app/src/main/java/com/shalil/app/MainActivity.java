package com.shalil.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private StoreDb db;
    private LinearLayout content;
    private final Map<Long,Integer> cart = new LinkedHashMap<>();
    private boolean toman = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        db = new StoreDb();
        content = findViewById(R.id.content_area);
        toman = getPreferences(0).getBoolean("toman", false);
        findViewById(R.id.nav_home).setOnClickListener(v -> dashboard());
        findViewById(R.id.nav_products).setOnClickListener(v -> products());
        findViewById(R.id.nav_invoice).setOnClickListener(v -> invoice());
        findViewById(R.id.nav_reports).setOnClickListener(v -> reports());
        findViewById(R.id.nav_settings).setOnClickListener(v -> settings());
        dashboard();
    }

    private void heading(String title) {
        content.removeAllViews();
        TextView h = new TextView(this); h.setText(title); h.setTextSize(22);
        h.setTextColor(getColor(R.color.ink)); h.setTypeface(null, 1);
        h.setPadding(4,8,4,16); content.addView(h);
    }
    private TextView text(String value, int size) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size);
        t.setTextColor(getColor(R.color.ink)); t.setPadding(2,5,2,5); return t;
    }
    private MaterialButton button(String label, Runnable action) {
        MaterialButton b = new MaterialButton(this); b.setText(label);
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void addCard(String title, String detail, View.OnClickListener click) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.surface));
        card.setRadius(18); card.setCardElevation(2);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16,12,16,12); box.addView(text(title,17)); box.addView(text(detail,14));
        card.addView(box); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,4,0,10); content.addView(card,lp);
        if(click!=null) card.setOnClickListener(click);
    }
    private String money(long n) {
        long shown = toman ? n/10 : n;
        return NumberFormat.getInstance(new Locale("fa","IR")).format(shown) + (toman ? " تومان" : " ریال");
    }
    private void dashboard() {
        heading("نرم افزار شلیل");
        addCard("فاکتور جدید","ثبت فروش و محاسبه خودکار مبلغ",v -> invoice());
        addCard("مدیریت کالا","افزودن، ویرایش و حذف کالاها",v -> products());
        addCard("گزارش فروش","مشاهده فاکتورهای ثبت‌شده",v -> reports());
        Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*), COALESCE(SUM(stock),0) FROM products",null);
        c.moveToFirst(); String stats="تعداد کالا: "+c.getInt(0)+"   |   موجودی کل: "+c.getInt(1); c.close();
        addCard("وضعیت انبار",stats,null);
    }
    private void products() {
        heading("مدیریت کالاها");
        content.addView(button("+ افزودن کالای جدید",this::productDialog));
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name,code,stock,buy,price FROM products ORDER BY name",null);
        if(c.getCount()==0) content.addView(text("هنوز کالایی ثبت نشده است.",15));
        while(c.moveToNext()) {
            long id=c.getLong(0); String name=c.getString(1), code=c.getString(2);
            int stock=c.getInt(3); long buy=c.getLong(4), price=c.getLong(5);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
            addCard(name,"بارکد: "+(code==null?"—":code)+" | موجودی: "+stock+"\nخرید: "+money(buy)+" | فروش: "+money(price),null);
            LinearLayout actions=new LinearLayout(this);
            MaterialButton edit=button("ویرایش",()->productDialog(id,name,code,stock,buy,price));
            MaterialButton del=button("حذف",()->new AlertDialog.Builder(this).setMessage("این کالا حذف شود؟")
                .setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{db.getWritableDatabase().delete("products","id=?",new String[]{String.valueOf(id)});products();}).show());
            actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(del,new LinearLayout.LayoutParams(0,-2,1));
            content.addView(actions);
        }
        c.close();
    }
    private void productDialog(){ productDialog(-1,"","","",0,0,0); }
    private void productDialog(long id,String name,String code,int stock,long buy,long price) {
        LinearLayout form=new LinearLayout(this); form.setPadding(20,8,20,8); form.setOrientation(LinearLayout.VERTICAL);
        TextInputEditText n=field("نام کالا",name,InputType.TYPE_CLASS_TEXT);
        TextInputEditText bc=field("بارکد (اختیاری)",code,InputType.TYPE_CLASS_TEXT);
        TextInputEditText st=field("تعداد موجودی",String.valueOf(stock),InputType.TYPE_CLASS_NUMBER);
        TextInputEditText bp=field("قیمت خرید (ریال)",String.valueOf(buy),InputType.TYPE_CLASS_NUMBER);
        TextInputEditText sp=field("قیمت فروش (ریال)",String.valueOf(price),InputType.TYPE_CLASS_NUMBER);
        form.addView(n);form.addView(bc);form.addView(st);form.addView(bp);form.addView(sp);
        new AlertDialog.Builder(this).setTitle(id<0?"افزودن کالا":"ویرایش کالا").setView(form)
          .setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
            if(n.getText()==null||n.getText().toString().trim().isEmpty()){toast("نام کالا را وارد کنید");return;}
            ContentValues v=new ContentValues();v.put("name",n.getText().toString().trim());v.put("code",bc.getText().toString());
            v.put("stock",number(st));v.put("buy",numberLong(bp));v.put("price",numberLong(sp));
            if(id<0)db.getWritableDatabase().insert("products",null,v);else db.getWritableDatabase().update("products",v,"id=?",new String[]{String.valueOf(id)});
            products();
          }).show();
    }
    private TextInputEditText field(String hint,String value,int type){
        TextInputEditText e=new TextInputEditText(this);e.setHint(hint);e.setText(value);e.setInputType(type);
        e.setSingleLine(true);return e;
    }
    private int number(TextInputEditText e){return (int)Math.max(0,numberLong(e));}
    private long numberLong(TextInputEditText e){try{return Long.parseLong(e.getText().toString().trim());}catch(Exception ex){return 0;}}
    private void invoice() {
        heading("فاکتور فروش جدید");
        content.addView(text("برای افزودن کالا، دکمه «افزودن» را بزنید.",14));
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name,stock,price FROM products WHERE stock>0 ORDER BY name",null);
        while(c.moveToNext()){
            long id=c.getLong(0);String name=c.getString(1);int stock=c.getInt(2);long price=c.getLong(3);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=text(name+"\nموجودی: "+stock+" | "+money(price),14);
            row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(button("افزودن",()->{cart.put(id,cart.getOrDefault(id,0)+1);invoice();}));
            content.addView(row);
        } c.close();
        content.addView(text("اقلام فاکتور",18));
        long total=0;
        for(Map.Entry<Long,Integer> item:cart.entrySet()){
            Cursor p=db.getReadableDatabase().rawQuery("SELECT name,price FROM products WHERE id=?",new String[]{String.valueOf(item.getKey())});
            if(p.moveToFirst()){long line=p.getLong(1)*item.getValue();total+=line;addCard(p.getString(0)+" × "+item.getValue(),money(line),null);}p.close();
        }
        addCard("جمع کل فاکتور",money(total),null);
        content.addView(button("بستن و ثبت فاکتور",()->finishInvoice()));
        content.addView(button("پاک کردن فاکتور",()->{cart.clear();invoice();}));
    }
    private void finishInvoice(){
        if(cart.isEmpty()){toast("فاکتور خالی است");return;}
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            long total=0;for(Map.Entry<Long,Integer> e:cart.entrySet()){
                Cursor p=w.rawQuery("SELECT price,stock FROM products WHERE id=?",new String[]{String.valueOf(e.getKey())});
                if(!p.moveToFirst()||p.getInt(1)<e.getValue()){p.close();throw new IllegalStateException("موجودی یکی از کالاها کافی نیست");}
                total+=p.getLong(0)*e.getValue();p.close();
            }
            ContentValues sale=new ContentValues();sale.put("total",total);sale.put("created",System.currentTimeMillis());
            long sid=w.insertOrThrow("sales",null,sale);
            for(Map.Entry<Long,Integer> e:cart.entrySet()){
                Cursor p=w.rawQuery("SELECT price FROM products WHERE id=?",new String[]{String.valueOf(e.getKey())});p.moveToFirst();long price=p.getLong(0);p.close();
                ContentValues line=new ContentValues();line.put("sale_id",sid);line.put("product_id",e.getKey());line.put("qty",e.getValue());line.put("price",price);w.insert("sale_lines",null,line);
                w.execSQL("UPDATE products SET stock=stock-? WHERE id=?",new Object[]{e.getValue(),e.getKey()});
            }
            w.setTransactionSuccessful();cart.clear();toast("فاکتور با موفقیت ثبت شد");dashboard();
        }catch(Exception ex){toast(ex.getMessage()==null?"ثبت فاکتور انجام نشد":ex.getMessage());}
        finally{w.endTransaction();}
    }
    private void reports(){
        heading("گزارش فاکتورها");
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,total,created FROM sales ORDER BY created DESC",null);
        if(c.getCount()==0)content.addView(text("هنوز فروشی ثبت نشده است.",15));
        while(c.moveToNext()){
            String date=new SimpleDateFormat("yyyy/MM/dd - HH:mm",Locale.getDefault()).format(new Date(c.getLong(2)));
            addCard("فاکتور شماره "+c.getLong(0),date+"\nمبلغ: "+money(c.getLong(1)),null);
        }c.close();
    }
    private void settings(){
        heading("تنظیمات");
        content.addView(text("واحد نمایش قیمت",16));
        content.addView(button(toman?"واحد فعلی: تومان (تغییر به ریال)":"واحد فعلی: ریال (تغییر به تومان)",()->{
            toman=!toman;getPreferences(0).edit().putBoolean("toman",toman).apply();settings();
        }));
        addCard("ذخیره‌سازی","اطلاعات کالاها و فاکتورها به‌صورت محلی روی همین دستگاه ذخیره می‌شوند.",null);
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    private class StoreDb extends SQLiteOpenHelper{
        StoreDb(){super(MainActivity.this,"shalil.db",null,1);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,code TEXT,stock INTEGER NOT NULL,buy INTEGER NOT NULL,price INTEGER NOT NULL)");
            d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,total INTEGER NOT NULL,created INTEGER NOT NULL)");
            d.execSQL("CREATE TABLE sale_lines(id INTEGER PRIMARY KEY AUTOINCREMENT,sale_id INTEGER,product_id INTEGER,qty INTEGER,price INTEGER)");
        }
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV){}
    }
}
