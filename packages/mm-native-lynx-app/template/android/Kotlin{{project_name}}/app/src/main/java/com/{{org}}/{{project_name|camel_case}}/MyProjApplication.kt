package com.myOrg.myProj

import lynxpo.core.modules.clipboard.ClipboardModule
import lynxpo.core.modules.device.DeviceModule
import lynxpo.core.modules.statusbar.StatusBarModule
import lynxpo.core.LynxpoApp

class MyProjApplication : LynxpoApp() {
    override val lynxpoModules
        get() = arrayOf(
            Pair("ClipboardModule", ClipboardModule::class.java),
            Pair("DeviceModule", DeviceModule::class.java),
            Pair("StatusBarModule", StatusBarModule::class.java)
            
        )
}
