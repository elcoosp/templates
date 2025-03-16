import android.content.Context
import com.lynx.jsbridge.LynxModule

@DslMarker
annotation class LynxpoModuleDsl

abstract class LynxpoModule(context: Context) : LynxModule(context) {
    val propDefinitions = mutableListOf<PropDefinition<*>>()
    val onCreateHandlers = mutableListOf<() -> Unit>()
    val onDestroyHandlers = mutableListOf<() -> Unit>()
    val onStartObservingHandlers = mutableListOf<() -> Unit>()
    val onStopObservingHandlers = mutableListOf<() -> Unit>()
    val activityLifecycleHandlers =
            mutableMapOf<ActivityLifecycleEvent, MutableList<() -> Unit>>()

    // Prop API - Using inline function with reified type parameter
    @LynxpoModuleDsl
    inline fun <reified T> Prop(name: String, noinline setter: (value: T) -> Unit) {
        propDefinitions.add(PropDefinition(name, setter))
    }

    // Lifecycle APIs - Using inline functions with crossinline lambdas
    @LynxpoModuleDsl
    inline fun OnCreate(crossinline handler: () -> Unit) {
        onCreateHandlers.add { handler() }
    }

    @LynxpoModuleDsl
    inline fun OnDestroy(crossinline handler: () -> Unit) {
        onDestroyHandlers.add { handler() }
    }

    @LynxpoModuleDsl
    inline fun OnStartObserving(crossinline handler: () -> Unit) {
        onStartObservingHandlers.add { handler() }
    }

    @LynxpoModuleDsl
    inline fun OnStopObserving(crossinline handler: () -> Unit) {
        onStopObservingHandlers.add { handler() }
    }

    // Activity Lifecycle APIs - Using inline functions with crossinline lambdas
    @LynxpoModuleDsl
    inline fun OnActivityEntersForeground(crossinline handler: () -> Unit) {
        activityLifecycleHandlers
                .getOrPut(ActivityLifecycleEvent.ENTERS_FOREGROUND) { mutableListOf() }
                .add { handler() }
    }

    @LynxpoModuleDsl
    inline fun OnActivityEntersBackground(crossinline handler: () -> Unit) {
        activityLifecycleHandlers
                .getOrPut(ActivityLifecycleEvent.ENTERS_BACKGROUND) { mutableListOf() }
                .add { handler() }
    }

    @LynxpoModuleDsl
    inline fun OnActivityDestroys(crossinline handler: () -> Unit) {
        activityLifecycleHandlers
                .getOrPut(ActivityLifecycleEvent.DESTROYS) { mutableListOf() }
                .add { handler() }
    }

    fun initialize() {
        // // Register props with LynxModule system
        // propDefinitions.forEach { prop ->
        //     registerLynxProp(prop.name, prop.setter)
        // }

        // // Register lifecycle listeners
        // registerLynxLifecycle(object : LynxLifecycleObserver {
        //     override fun onCreate() {
        //         onCreateHandlers.forEach { it() }
        //     }

        //     override fun onDestroy() {
        //         onDestroyHandlers.forEach { it() }
        //     }
        // })

        // // Register observation listeners
        // registerLynxObservation(object : LynxObservationListener {
        //     override fun onStartObserving() {
        //         onStartObservingHandlers.forEach { it() }
        //     }

        //     override fun onStopObserving() {
        //         onStopObservingHandlers.forEach { it() }
        //     }
        // })

        // // Register activity lifecycle
        // registerLynxActivityLifecycle(object : LynxActivityLifecycleObserver {
        //     override fun onEvent(event: ActivityLifecycleEvent) {
        //         activityLifecycleHandlers[event]?.forEach { it() }
        //     }
        // })
    }

    inner class PropDefinition<T>(val name: String, val setter: (T) -> Unit)

    enum class ActivityLifecycleEvent {
        ENTERS_FOREGROUND,
        ENTERS_BACKGROUND,
        DESTROYS
    }
}