package com.{{org}}.{{project_name|camel_case}}

import android.view.View
import lynxpo.core.LynxpoApp
import lynxpo.core.LynxpoUI
import lynxpo.core.modules.clipboard.ClipboardModule
import lynxpo.core.modules.device.DeviceModule
import lynxpo.core.modules.statusbar.StatusBarModule
import lynxpo.core.modules.ui.input.InputUI

class MyProjApplication : LynxpoApp() {
    override val lynxpoModules
        get() = arrayOf(
            Pair("ClipboardModule", ClipboardModule::class.java),
            Pair("DeviceModule", DeviceModule::class.java),
            Pair("StatusBarModule", StatusBarModule::class.java)
        )
    override val lynxpoUiModules: Array<Pair<String, Class<out LynxpoUI<out View>>>>
        get() = arrayOf(
            Pair("input", InputUI::class.java)
        )
}
