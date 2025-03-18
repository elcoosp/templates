
interface CodedThrowable {
  fun getCode(): String
  fun getMessage(): String
}



/**
 * Base class that can be extended to create coded errors that promise.reject
 * can handle.
 */
public abstract class CodedException : Exception {
    constructor(message: String) : super(message)

    constructor(cause: Throwable?) : super(cause)
    constructor(message: String, cause: Throwable?) : super(message, cause)

    open val code: String
        get() = "ERR_UNSPECIFIED_ANDROID_EXCEPTION"
}

