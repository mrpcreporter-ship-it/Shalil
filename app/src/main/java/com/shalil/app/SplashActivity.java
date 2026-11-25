package com.shalil.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(7, 94, 89));
        getWindow().setNavigationBarColor(Color.rgb(7, 94, 89));
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(7,94,89), Color.rgb(15,139,131), Color.rgb(7,94,89)});
        root.setBackground(bg);

        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams stackParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        stackParams.setMargins(dp(24), 0, dp(24), 0);
        root.addView(stack, stackParams);

        ImageView fruit = new ImageView(this);
        fruit.setImageResource(R.drawable.nectarine_mark);
        fruit.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams fruitParams = new LinearLayout.LayoutParams(dp(150), dp(150));
        fruitParams.gravity = Gravity.CENTER;
        stack.addView(fruit, fruitParams);
        fruit.setTranslationY(dp(280));
        fruit.setRotation(-75f);
        fruit.setScaleX(0.72f); fruit.setScaleY(0.72f);

        TextView brand = text("Shalil", 34, Color.WHITE, true);
        brand.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(-2, -2);
        brandParams.topMargin = dp(18);
        stack.addView(brand, brandParams);
        TextView persian = text("نرم افزار شلیل", 20, Color.rgb(226,248,244), true);
        LinearLayout.LayoutParams persianParams = new LinearLayout.LayoutParams(-2, -2);
        persianParams.topMargin = dp(4);
        stack.addView(persian, persianParams);

        View line = new View(this);
        GradientDrawable lineBg = new GradientDrawable();
        lineBg.setColor(0x66FFFFFF); lineBg.setCornerRadius(dp(2));
        line.setBackground(lineBg);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(92), dp(2));
        lineParams.topMargin = dp(30); lineParams.bottomMargin = dp(18);
        stack.addView(line, lineParams);
        stack.addView(text("Created by Mohammad Rasooli", 13, Color.WHITE, false));
        TextView powered = text("powered by ChatGPT", 12, Color.rgb(190,231,224), false);
        LinearLayout.LayoutParams poweredParams = new LinearLayout.LayoutParams(-2, -2);
        poweredParams.topMargin = dp(5);
        stack.addView(powered, poweredParams);

        setContentView(root);
        ObjectAnimator fruitMotion = ObjectAnimator.ofPropertyValuesHolder(fruit,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, dp(280), 0f),
                PropertyValuesHolder.ofFloat(View.ROTATION, -75f, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.72f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.72f, 1f));
        fruitMotion.setDuration(1250);
        fruitMotion.setInterpolator(new DecelerateInterpolator(1.7f));
        brand.setAlpha(0f); persian.setAlpha(0f); line.setAlpha(0f);
        stack.getChildAt(4).setAlpha(0f); powered.setAlpha(0f);
        fruitMotion.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                brand.animate().alpha(1f).setDuration(350).start();
                persian.animate().alpha(1f).setDuration(350).setStartDelay(90).start();
                line.animate().alpha(1f).setDuration(350).setStartDelay(180).start();
                stack.getChildAt(4).animate().alpha(1f).setDuration(350).setStartDelay(250).start();
                powered.animate().alpha(1f).setDuration(350).setStartDelay(320).start();
            }
        });
        fruitMotion.start();
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3200);
    }
}