import android.content.Context
import com.lynx.jsbridge.LynxModule

abstract class LynxpoModule(context: Context) : LynxModule(context) {
    private val propDefinitions = mutableListOf<PropDefinition<*>>()
    private val onCreateHandlers = mutableListOf<() -> Unit>()
    private val onDestroyHandlers = mutableListOf<() -> Unit>()
    private val onStartObservingHandlers = mutableListOf<() -> Unit>()
    private val onStopObservingHandlers = mutableListOf<() -> Unit>()
    private val activityLifecycleHandlers =
            mutableMapOf<ActivityLifecycleEvent, MutableList<() -> Unit>>()

    // DSL support
    protected class DslMarker

    // Class properties that act as DSL elements
    @JvmField protected val OnCreate = EventHandler(onCreateHandlers)
    @JvmField protected val OnDestroy = EventHandler(onDestroyHandlers)
    @JvmField protected val OnStartObserving = EventHandler(onStartObservingHandlers)
    @JvmField protected val OnStopObserving = EventHandler(onStopObservingHandlers)
    @JvmField
    protected val OnActivityEntersForeground =
            EventHandler(
                    activityLifecycleHandlers.getOrPut(ActivityLifecycleEvent.ENTERS_FOREGROUND) {
                        mutableListOf()
                    }
            )
    @JvmField
    protected val OnActivityEntersBackground =
            EventHandler(
                    activityLifecycleHandlers.getOrPut(ActivityLifecycleEvent.ENTERS_BACKGROUND) {
                        mutableListOf()
                    }
            )
    @JvmField
    protected val OnActivityDestroys =
            EventHandler(
                    activityLifecycleHandlers.getOrPut(ActivityLifecycleEvent.DESTROYS) {
                        mutableListOf()
                    }
            )

    // Event handler class that supports the DSL syntax
    protected class EventHandler(private val handlers: MutableList<() -> Unit>) {
        operator fun invoke(handler: () -> Unit) {
            handlers.add(handler)
        }
    }

    // Prop API
    fun <T> Prop(name: String, setter: (value: T) -> Unit) {
        propDefinitions.add(PropDefinition(name, setter))
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

    private inner class PropDefinition<T>(val name: String, val setter: (T) -> Unit)

    enum class ActivityLifecycleEvent {
        ENTERS_FOREGROUND,
        ENTERS_BACKGROUND,
        DESTROYS
    }
}
