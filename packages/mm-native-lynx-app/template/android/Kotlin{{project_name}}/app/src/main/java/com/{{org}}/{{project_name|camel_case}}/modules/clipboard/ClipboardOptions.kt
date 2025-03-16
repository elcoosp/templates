package com.{{org}}.{{project_name|camel_case}}.modules.clipboard

import com.lynx.jsbridge.LynxMethod
import com.lynx.jsbridge.LynxModule
import com.lynx.jsbridge.Promise
import com.lynx.tasm.behavior.LynxContext
// TODO move in dedicated package
import LynxpoModule


import android.graphics.Bitmap

import com.lynx.react.bridge.ReadableMap
import kotlinx.serialization.*


@Serializable
abstract class GetImageOptions : ReadableMap {
  var imageFormat: ImageFormat = ImageFormat.JPG


var jpegQuality: Double = 1.0

}

@Serializable
abstract class GetStringOptions : ReadableMap {
  var preferredFormat: StringFormat = StringFormat.PLAIN

}

@Serializable
abstract class SetStringOptions : ReadableMap {
  var inputFormat: StringFormat = StringFormat.PLAIN

}

@Serializable
enum class ImageFormat(val jsName: String) {
  JPG("jpeg"),
  PNG("png");

  val compressFormat: Bitmap.CompressFormat
    get() = when (this) {
      JPG -> Bitmap.CompressFormat.JPEG
      PNG -> Bitmap.CompressFormat.PNG
    }

  val mimeType: String
    get() = when (this) {
      JPG -> "image/jpeg"
      PNG -> "image/png"
    }
}


@Serializable
enum class StringFormat(val jsValue: String) {
  PLAIN("plainText"),
  HTML("html")
}
