package com.{{org}}.{{project_name|camel_case}};

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.lynx.tasm.LynxView;
import com.lynx.tasm.LynxViewBuilder;
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LynxView lynxView = buildLynxView();
        setContentView(lynxView);

        String url = "main.lynx.bundle";
        lynxView.renderTemplateUrl(url, "");
    }

    private LynxView buildLynxView() {
         LynxViewBuilder viewBuilder = new LynxViewBuilder();
         viewBuilder.setTemplateProvider(new TemplateProvider(this));
         return viewBuilder.build(this);
     }
}
