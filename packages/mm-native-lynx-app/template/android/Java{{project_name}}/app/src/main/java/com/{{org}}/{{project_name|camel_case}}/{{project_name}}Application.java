package com.{{org}}.{{project_name|camel_case}};

import android.app.Application;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.lynx.service.http.LynxHttpService;
import com.lynx.service.image.LynxImageService;
import com.lynx.service.log.LynxLogService;
import com.lynx.tasm.service.LynxServiceCenter;
import com.lynx.tasm.LynxEnv;
public class {{project_name}}Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initLynxService();
        initLynxEnv();
    }

    private void initLynxService() {
        // init Fresco which is needed by LynxImageService
        final PoolFactory factory = new PoolFactory(PoolConfig.newBuilder().build());
        ImagePipelineConfig.Builder builder =
                ImagePipelineConfig.newBuilder(getApplicationContext()).setPoolFactory(factory);
        Fresco.initialize(getApplicationContext(), builder.build());

        LynxServiceCenter.inst().registerService(LynxImageService.getInstance());
        LynxServiceCenter.inst().registerService(LynxLogService.INSTANCE);
        LynxServiceCenter.inst().registerService(LynxHttpService.INSTANCE);
    }

    private void initLynxEnv() {
         LynxEnv.inst().init(
             this,
             null,
             null,
             null
         );
     }
}
