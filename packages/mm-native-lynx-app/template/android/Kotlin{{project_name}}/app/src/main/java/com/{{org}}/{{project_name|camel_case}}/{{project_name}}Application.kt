package com.{{org}}.{{project_name|camel_case}}

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.memory.PoolConfig
import com.facebook.imagepipeline.memory.PoolFactory
import com.lynx.service.http.LynxHttpService
import com.lynx.service.image.LynxImageService
import com.lynx.service.log.LynxLogService
import com.lynx.tasm.service.LynxServiceCenter
import com.lynx.tasm.LynxEnv
import com.{{org}}.{{project_name|camel_case}}.modules.device.DeviceModule
import com.{{org}}.{{project_name|camel_case}}.modules.clipboard.ClipboardModule
class {{project_name}}Application : Application() {

    override fun onCreate() {
        super.onCreate()
        initLynxService()
        initLynxEnv()
    }

    private fun initLynxService() {
        // Init Fresco which is needed by LynxImageService
        val factory = PoolFactory(PoolConfig.newBuilder().build())
        val builder = ImagePipelineConfig.newBuilder(applicationContext).setPoolFactory(factory)
        Fresco.initialize(applicationContext, builder.build())

        LynxServiceCenter.inst().registerService(LynxImageService.getInstance())
        LynxServiceCenter.inst().registerService(LynxLogService)
        LynxServiceCenter.inst().registerService(LynxHttpService)
    }
    private fun initLynxModules() {
        LynxEnv.inst().registerModule("DeviceModule", DeviceModule::class.java)
        LynxEnv.inst().registerModule("ClipboardModule", ClipboardModule::class.java)
    }

    private fun initLynxEnv() {
         LynxEnv.inst().init(
             this,
             null,
             null,
             null
         )
         initLynxModules()
     }
}
