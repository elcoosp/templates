package com.{{org}}.{{project_name|camel_case}}.modules.clipboard
import LynxpoModule
import CodedException

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.text.Spanned
import android.text.TextUtils
import android.util.Log
import androidx.core.os.bundleOf
import com.lynx.jsbridge.LynxMethod
import com.lynx.jsbridge.Promise
import com.lynx.react.bridge.*
import com.lynx.tasm.behavior.LynxContext
import java.io.File
import kotlinx.coroutines.*

inline fun <T> T?.ifNull(block: () -> T): T = this ?: block()

inline fun <reified T> Any?.takeIfInstanceOf(): T? = this as? T

private const val moduleName = "ExpoClipboard"
private val TAG = ClipboardModule::class.java.simpleName

// this must match the one from `res/xml/clipboard_provider_paths.xml`
const val CLIPBOARD_DIRECTORY_NAME = ".clipboard"
const val CLIPBOARD_CHANGED_EVENT_NAME = "onClipboardChanged"

private enum class ContentType(val jsName: String) {
  PLAIN_TEXT("plain-text"),
  HTML("html"),
  IMAGE("image")
}

class ClipboardModule(private val context: Context) : LynxpoModule(context) {

  init {

    OnCreate {
      clipboardEventEmitter = ClipboardEventEmitter()
      clipboardEventEmitter.attachListener()
    }

    OnDestroy { clipboardEventEmitter.detachListener() }

    OnActivityEntersBackground { clipboardEventEmitter.pauseListening() }

    OnActivityEntersForeground { clipboardEventEmitter.resumeListening() }
  }
  
  @LynxMethod
  fun getStringAsync(options: GetStringOptions, promise: Promise) {
    val item = clipboardManager.firstItem
    val result =
            when (options.preferredFormat) {
              StringFormat.PLAIN -> item?.coerceToPlainText(context)
              StringFormat.HTML -> item?.coerceToHtmlText(context)
            }
    return promise.resolve(result)
  }

  
  @LynxMethod
  fun setStringAsync(content: String, options: SetStringOptions, promise: Promise) {
    val clip =
            when (options.inputFormat) {
              StringFormat.PLAIN -> ClipData.newPlainText(null, content)
              StringFormat.HTML -> {
                // HTML clip requires complementary plain text content
                val plainText = plainTextFromHtml(content)
                ClipData.newHtmlText(null, plainText, content)
              }
            }
    clipboardManager.setPrimaryClip(clip)
    return promise.resolve(true)
  }

  @LynxMethod
  fun hasStringAsync(promise: Promise): Boolean = clipboardManager.primaryClipDescription?.hasTextContent ?: false

  // endregion

  // region Images
  @LynxMethod
  fun getImageAsync(options: GetImageOptions, promise: Promise) {
    runBlocking {
      launch {
        val imageUri =
                clipboardManager
                        .takeIf { clipboardHasItemWithType("image/*") }
                        ?.firstItem
                        ?.uri
                        .ifNull {
                          return@launch promise.resolve(null)
                        }

        try {
          val imageResult = imageFromContentUri(context, imageUri, options)
          return@launch promise.resolve(imageResult.toBundle())
        } catch (err: Throwable) {
          err.printStackTrace()
          return@launch promise.reject(
                  when (err) {
                    is CodedException -> err
                    is SecurityException -> NoPermissionException(err)
                    else -> PasteFailureException(err, kind = "image")
                  }.toString()
          )
        }
      }
    }
  }

  @LynxMethod
  fun setImageAsync(imageData: String, promise: Promise) {
    runBlocking {
      launch {
        try {
          val clip = clipDataFromBase64Image(context, imageData, clipboardCacheDir)
          clipboardManager.setPrimaryClip(clip)
        } catch (err: Throwable) {
          err.printStackTrace()
          return@launch promise.reject(
                  when (err) {
                    is CodedException -> err
                    else -> CopyFailureException(err, kind = "image")
                  }.toString()
          )
        }
      }
    }
  }

  @LynxMethod
  fun hasImageAsync(promise: Promise): Boolean = clipboardManager.primaryClipDescription?.hasMimeType("image/*") == true


  private fun getContext(): Context {
    val lynxContext = mContext as LynxContext
    return lynxContext.getContext()
  }

  private val clipboardManager: ClipboardManager
    get() =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: throw ClipboardUnavailableException()

  private val clipboardCacheDir: File by lazy {
    File(context.cacheDir, CLIPBOARD_DIRECTORY_NAME).also { it.mkdirs() }
  }

  // region Clipboard event emitter
  private lateinit var clipboardEventEmitter: ClipboardEventEmitter

  private inner class ClipboardEventEmitter {
    private var isListening = true
    private var timestamp = -1L
    fun resumeListening() {
      isListening = true
    }

    fun pauseListening() {
      isListening = false
    }

    fun attachListener() =
            maybeClipboardManager?.addPrimaryClipChangedListener(listener).ifNull {
              Log.e(TAG, "'CLIPBOARD_SERVICE' unavailable. Events won't be received")
            }

    fun detachListener() = maybeClipboardManager?.removePrimaryClipChangedListener(listener)

    private val listener =
            ClipboardManager.OnPrimaryClipChangedListener {
              // if (!appContext.hasActiveReactInstance) {
                // return@OnPrimaryClipChangedListener
              // }

              maybeClipboardManager.takeIf { isListening }?.primaryClipDescription?.let { clip ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  if (timestamp == clip.timestamp) {
                    return@OnPrimaryClipChangedListener
                  }
                  timestamp = clip.timestamp
                }
                val contentTypes = listOfNotNull(
                  ContentType.PLAIN_TEXT.takeIf { clip.hasTextContent },
                  ContentType.HTML.takeIf { clip.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) },
                  ContentType.IMAGE.takeIf { clip.hasMimeType("image/*") }
              ).map { it.jsName }
              
              // Create JavaOnlyArray for contentTypes
              val contentTypesArray = JavaOnlyArray()
              contentTypes.forEach { contentTypesArray.pushString(it) }
                (mContext as LynxContext)
                        .sendGlobalEvent(
                                CLIPBOARD_CHANGED_EVENT_NAME,
                                contentTypesArray
                        )
              }
            }

    private val maybeClipboardManager = runCatching { clipboardManager }.getOrNull()
  }
  // endregion

  // region Utilities

  /**
   * Check whether the clipboard contains the given MIME type.
   *
   * Does NOT trigger the "Pasted from clipboard" toast on Android 12+
   *
   * @param mimeType The desired MIME type. May be a pattern, accepts wildcards.
   * @return Returns `true` if one of items in the clipboard matches the desired MIME type,
   * otherwise returns `false`.
   */
  private fun clipboardHasItemWithType(mimeType: String) =
          clipboardManager.primaryClipDescription?.hasMimeType(mimeType) ?: false

  /** Gets first item from the clipboard or null if empty */
  private val ClipboardManager.firstItem: ClipData.Item?
    get() = primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)

  // endregion
}

private fun plainTextFromHtml(htmlContent: String): String {
  val styledText: Spanned = Html.fromHtml(htmlContent, FROM_HTML_MODE_LEGACY)
  val chars = CharArray(styledText.length)
  TextUtils.getChars(styledText, 0, styledText.length, chars, 0)
  return String(chars)
}

/**
 * Turn this item into text, regardless of the type of data it actually contains. It is the same as
 * [ClipData.Item.coerceToText] but this also supports HTML.
 *
 * The algorithm for deciding what text to return is:
 * - If [ClipData.Item.getHtmlText] is non-null, strip HTML tags and return that. See
 * [plainTextFromHtml] for implementation details
 * - Otherwise, return the result of [ClipData.Item.coerceToText]
 *
 * @param context The caller's Context, from which its ContentResolver and other things can be
 * retrieved.
 * @return Returns the item's textual representation.
 */
private fun ClipData.Item.coerceToPlainText(context: Context): String =
        if (text == null && htmlText != null) {
          plainTextFromHtml(htmlText)
        } else {
          coerceToText(context).toString()
        }

/** True if clipboard contains plain text or HTML content */
private val ClipDescription.hasTextContent: Boolean
  get() =
          hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                  hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
