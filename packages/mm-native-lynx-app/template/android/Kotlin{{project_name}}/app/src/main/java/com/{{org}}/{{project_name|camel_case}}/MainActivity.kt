package com.{{org}}.{{project_name|camel_case}}


import com.{{org}}.{{project_name|camel_case}}.ui.theme.Kotlin{{project_name}}Theme
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.lynx.tasm.LynxView
import com.lynx.tasm.LynxViewBuilder
import lynxpo.core.LynxpoTemplateProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lynxView = buildLynxView()
        setContentView(lynxView)
        val uri = "main.lynx.bundle";
        lynxView.renderTemplateUrl(uri, "")
    }

    private fun buildLynxView(): LynxView {
        val viewBuilder = LynxViewBuilder()
        viewBuilder.setTemplateProvider(LynxpoTemplateProvider(this))
        return viewBuilder.build(this)
    }
}
