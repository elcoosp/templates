package com.{{org}}.{{project_name|camel_case}}

import lynxpo.core.LynxpoApp
import lynxpo.core.modules.clipboard.ClipboardModule
import lynxpo.core.modules.device.DeviceModule
import lynxpo.core.modules.statusbar.StatusBarModule

class MyProjApplication : LynxpoApp() {
    override val lynxpoModules
        get() = arrayOf(
            Pair("ClipboardModule", ClipboardModule::class.java),
            Pair("DeviceModule", DeviceModule::class.java),
            Pair("StatusBarModule", StatusBarModule::class.java)
            
        )
}
