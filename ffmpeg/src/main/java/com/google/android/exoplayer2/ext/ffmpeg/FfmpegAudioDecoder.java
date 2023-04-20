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
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleDecoder;
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;

import java.nio.ByteBuffer;
import java.util.List;

/** FFmpeg audio decoder. */
/* package */ final class FfmpegAudioDecoder
        extends SimpleDecoder<DecoderInputBuffer, SimpleOutputBuffer, FfmpegDecoderException> {

  // Output buffer sizes when decoding PCM mu-law streams, which is the maximum FFmpeg outputs.
  private static final int OUTPUT_BUFFER_SIZE_16BIT = 65536;
  private static final int OUTPUT_BUFFER_SIZE_32BIT = OUTPUT_BUFFER_SIZE_16BIT * 2;

  private static final int AUDIO_DECODER_ERROR_INVALID_DATA = -1;
  private static final int AUDIO_DECODER_ERROR_OTHER = -2;

  private final String codecName;
  @Nullable private final byte[] extraData;
  @C.Encoding private final int encoding;
  private final int outputBufferSize;

  private long nativeContext; // May be reassigned on resetting the codec.
  private boolean hasOutputFormat;
  private volatile int channelCount;
  private volatile int sampleRate;

  public FfmpegAudioDecoder(
          Format format,
          int numInputBuffers,
          int numOutputBuffers,
          int initialInputBufferSize,
          boolean outputFloat)
          throws FfmpegDecoderException {
    super(new DecoderInputBuffer[numInputBuffers], new SimpleOutputBuffer[numOutputBuffers]);
    if (!FfmpegLibrary.isAvailable()) {
      throw new FfmpegDecoderException("Failed to load decoder native libraries.");
    }
    Assertions.checkNotNull(format.sampleMimeType);
    this.codecName = (String)Assertions.checkNotNull(FfmpegLibrary.getCodecName(format.sampleMimeType));
    this.extraData = getExtraData(format.sampleMimeType, format.initializationData);
    this.encoding = outputFloat ? 4 : 2;
    this.outputBufferSize = outputFloat ? 131072 : 65536;
    this.nativeContext = this.ffmpegInitialize(this.codecName, this.extraData, outputFloat, format.sampleRate, format.channelCount);
    if (this.nativeContext == 0L) {
      throw new FfmpegDecoderException("Initialization failed.");
    } else {
      this.setInitialInputBufferSize(initialInputBufferSize);
    }
  }

  @Override
  public String getName() {
    return "ffmpeg" + FfmpegLibrary.getVersion() + "-" + codecName;
  }

  @Override
  protected DecoderInputBuffer createInputBuffer() {
    return new DecoderInputBuffer(
            DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT,
            FfmpegLibrary.getInputBufferPaddingSize());
  }

  @Override
  protected SimpleOutputBuffer createOutputBuffer() {
    return new SimpleOutputBuffer(this::releaseOutputBuffer);
  }

  @Override
  protected FfmpegDecoderException createUnexpectedDecodeException(Throwable error) {
    return new FfmpegDecoderException("Unexpected decode error", error);
  }

  @Override
  @Nullable
  protected FfmpegDecoderException decode(
          DecoderInputBuffer inputBuffer, SimpleOutputBuffer outputBuffer, boolean reset) {
    if (reset) {
      nativeContext = ffmpegReset(nativeContext, extraData);
      if (nativeContext == 0) {
        return new FfmpegDecoderException("Error resetting (see logcat).");
      }
    }
    ByteBuffer inputData = Util.castNonNull(inputBuffer.data);
    int inputSize = inputData.limit();
    ByteBuffer outputData = outputBuffer.init(inputBuffer.timeUs, outputBufferSize);
    int result = ffmpegDecode(nativeContext, inputData, inputSize, outputData, outputBufferSize);
    if (result == AUDIO_DECODER_ERROR_OTHER) {
      return new FfmpegDecoderException("Error decoding (see logcat).");
    } else if (result == AUDIO_DECODER_ERROR_INVALID_DATA) {
      // Treat invalid data errors as non-fatal to match the behavior of MediaCodec. No output will
      // be produced for this buffer, so mark it as decode-only to ensure that the audio sink's
      // position is reset when more audio is produced.
      outputBuffer.setFlags(C.BUFFER_FLAG_DECODE_ONLY);
      return null;
    } else if (result == 0) {
      // There's no need to output empty buffers.
      outputBuffer.setFlags(C.BUFFER_FLAG_DECODE_ONLY);
      return null;
    }
    if (!hasOutputFormat) {
      channelCount = ffmpegGetChannelCount(nativeContext);
      sampleRate = ffmpegGetSampleRate(nativeContext);
      if (sampleRate == 0 && "alac".equals(codecName)) {
        Assertions.checkNotNull(extraData);
        // ALAC decoder did not set the sample rate in earlier versions of FFmpeg. See
        // https://trac.ffmpeg.org/ticket/6096.
        ParsableByteArray parsableExtraData = new ParsableByteArray(extraData);
        parsableExtraData.setPosition(extraData.length - 4);
        sampleRate = parsableExtraData.readUnsignedIntToInt();
      }
      hasOutputFormat = true;
    }
    outputData.position(0);
    outputData.limit(result);
    return null;
  }

  @Override
  public void release() {
    super.release();
    ffmpegRelease(nativeContext);
    nativeContext = 0;
  }

  /** Returns the channel count of output audio. */
  public int getChannelCount() {
    return channelCount;
  }

  /** Returns the sample rate of output audio. */
  public int getSampleRate() {
    return sampleRate;
  }

  /** Returns the encoding of output audio. */
  @C.Encoding
  public int getEncoding() {
    return encoding;
  }

  @Nullable
  private static byte[] getExtraData(String mimeType, List<byte[]> initializationData) {
    byte var3 = -1;
    switch(mimeType.hashCode()) {
      case -1003765268:
        if (mimeType.equals("audio/vorbis")) {
          var3 = 3;
        }
        break;
      case -53558318:
        if (mimeType.equals("audio/mp4a-latm")) {
          var3 = 0;
        }
        break;
      case 1504470054:
        if (mimeType.equals("audio/alac")) {
          var3 = 2;
        }
        break;
      case 1504891608:
        if (mimeType.equals("audio/opus")) {
          var3 = 1;
        }
    }

    switch(var3) {
      case 0:
      case 1:
        return (byte[])initializationData.get(0);
      case 2:
        return getAlacExtraData(initializationData);
      case 3:
        return getVorbisExtraData(initializationData);
      default:
        return null;
    }
  }

  private static byte[] getAlacExtraData(List<byte[]> initializationData) {
    byte[] magicCookie = (byte[])initializationData.get(0);
    int alacAtomLength = 12 + magicCookie.length;
    ByteBuffer alacAtom = ByteBuffer.allocate(alacAtomLength);
    alacAtom.putInt(alacAtomLength);
    alacAtom.putInt(1634492771);
    alacAtom.putInt(0);
    alacAtom.put(magicCookie, 0, magicCookie.length);
    return alacAtom.array();
  }

  private static byte[] getVorbisExtraData(List<byte[]> initializationData) {
    byte[] header0 = (byte[])initializationData.get(0);
    byte[] header1 = (byte[])initializationData.get(1);
    byte[] extraData = new byte[header0.length + header1.length + 6];
    extraData[0] = (byte)(header0.length >> 8);
    extraData[1] = (byte)(header0.length & 255);
    System.arraycopy(header0, 0, extraData, 2, header0.length);
    extraData[header0.length + 2] = 0;
    extraData[header0.length + 3] = 0;
    extraData[header0.length + 4] = (byte)(header1.length >> 8);
    extraData[header0.length + 5] = (byte)(header1.length & 255);
    System.arraycopy(header1, 0, extraData, header0.length + 6, header1.length);
    return extraData;
  }

  private native long ffmpegInitialize(String var1, @Nullable byte[] var2, boolean var3, int var4, int var5);

  private native int ffmpegDecode(long var1, ByteBuffer var3, int var4, ByteBuffer var5, int var6);

  private native int ffmpegGetChannelCount(long var1);

  private native int ffmpegGetSampleRate(long var1);

  private native long ffmpegReset(long var1, @Nullable byte[] var3);

  private native void ffmpegRelease(long var1);

}
