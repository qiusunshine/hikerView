/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.ext.ffmpeg;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.util.LibraryLoader;
import com.google.android.exoplayer2.util.Log;

/** Configures and queries the underlying native library. */
public final class FfmpegLibrary {

  static {
    ExoPlayerLibraryInfo.registerModule("goog.exo.ffmpeg");
  }

  private static final String TAG = "FfmpegLibrary";

  private static final LibraryLoader LOADER =
          new LibraryLoader("avutil", "swresample", "avcodec", "ffmpeg");

  private static String version;
  private static int inputBufferPaddingSize = C.LENGTH_UNSET;

  private FfmpegLibrary() {}

  /**
   * Override the names of the FFmpeg native libraries. If an application wishes to call this
   * method, it must do so before calling any other method defined by this class, and before
   * instantiating a {@link FfmpegAudioRenderer} instance.
   *
   * @param libraries The names of the FFmpeg native libraries.
   */
  public static void setLibraries(String... libraries) {
    LOADER.setLibraries(libraries);
  }

  /** Returns whether the underlying library is available, loading it if necessary. */
  public static boolean isAvailable() {
    return LOADER.isAvailable();
  }

  /** Returns the version of the underlying library if available, or null otherwise. */
  @Nullable
  public static String getVersion() {
    if (!isAvailable()) {
      return null;
    }
    if (version == null) {
      version = ffmpegGetVersion();
    }
    return version;
  }

  /**
   * Returns the required amount of padding for input buffers in bytes, or {@link C#LENGTH_UNSET} if
   * the underlying library is not available.
   */
  public static int getInputBufferPaddingSize() {
    if (!isAvailable()) {
      return -1;
    } else {
      if (inputBufferPaddingSize == -1) {
        inputBufferPaddingSize = ffmpegGetInputBufferPaddingSize();
      }

      return inputBufferPaddingSize;
    }
  }

  /**
   * Returns whether the underlying library supports the specified MIME type.
   *
   * @param mimeType The MIME type to check.
   */
  public static boolean supportsFormat(String mimeType) {
    if (!isAvailable()) {
      return false;
    }
    @Nullable String codecName = getCodecName(mimeType);
    if (codecName == null) {
      return false;
    }
    if (!ffmpegHasDecoder(codecName)) {
      Log.w(TAG, "No " + codecName + " decoder available. Check the FFmpeg build configuration.");
      return false;
    }
    return true;
  }

  /**
   * Returns the name of the FFmpeg decoder that could be used to decode the format, or {@code null}
   * if it's unsupported.
   */
  @Nullable
  static String getCodecName(String mimeType) {
    byte var2 = -1;
    switch(mimeType.hashCode()) {
      case -2123537834:
        if (mimeType.equals("audio/eac3-joc")) {
          var2 = 6;
        }
        break;
      case -1606874997:
        if (mimeType.equals("audio/amr-wb")) {
          var2 = 13;
        }
        break;
      case -1095064472:
        if (mimeType.equals("audio/vnd.dts")) {
          var2 = 8;
        }
        break;
      case -1003765268:
        if (mimeType.equals("audio/vorbis")) {
          var2 = 10;
        }
        break;
      case -432837260:
        if (mimeType.equals("audio/mpeg-L1")) {
          var2 = 2;
        }
        break;
      case -432837259:
        if (mimeType.equals("audio/mpeg-L2")) {
          var2 = 3;
        }
        break;
      case -53558318:
        if (mimeType.equals("audio/mp4a-latm")) {
          var2 = 0;
        }
        break;
      case 187078296:
        if (mimeType.equals("audio/ac3")) {
          var2 = 4;
        }
        break;
      case 1503095341:
        if (mimeType.equals("audio/3gpp")) {
          var2 = 12;
        }
        break;
      case 1504470054:
        if (mimeType.equals("audio/alac")) {
          var2 = 15;
        }
        break;
      case 1504578661:
        if (mimeType.equals("audio/eac3")) {
          var2 = 5;
        }
        break;
      case 1504619009:
        if (mimeType.equals("audio/flac")) {
          var2 = 14;
        }
        break;
      case 1504831518:
        if (mimeType.equals("audio/mpeg")) {
          var2 = 1;
        }
        break;
      case 1504891608:
        if (mimeType.equals("audio/opus")) {
          var2 = 11;
        }
        break;
      case 1505942594:
        if (mimeType.equals("audio/vnd.dts.hd")) {
          var2 = 9;
        }
        break;
      case 1556697186:
        if (mimeType.equals("audio/true-hd")) {
          var2 = 7;
        }
        break;
      case 1903231877:
        if (mimeType.equals("audio/g711-alaw")) {
          var2 = 17;
        }
        break;
      case 1903589369:
        if (mimeType.equals("audio/g711-mlaw")) {
          var2 = 16;
        }
    }

    switch(var2) {
      case 0:
        return "aac";
      case 1:
      case 2:
      case 3:
        return "mp3";
      case 4:
        return "ac3";
      case 5:
      case 6:
        return "eac3";
      case 7:
        return "truehd";
      case 8:
      case 9:
        return "dca";
      case 10:
        return "vorbis";
      case 11:
        return "opus";
      case 12:
        return "amrnb";
      case 13:
        return "amrwb";
      case 14:
        return "flac";
      case 15:
        return "alac";
      case 16:
        return "pcm_mulaw";
      case 17:
        return "pcm_alaw";
      default:
        return null;
    }
  }


  private static native String ffmpegGetVersion();

  private static native int ffmpegGetInputBufferPaddingSize();

  private static native boolean ffmpegHasDecoder(String var0);
}
