/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins {
    import flash.media.Video;
    import flash.net.NetStream;

    import org.mangui.hls.HLS;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.osmf.plugins.loader.HLSNetLoader;
    import org.mangui.osmf.plugins.traits.*;
    import org.mangui.osmf.plugins.utils.ErrorManager;

    import org.osmf.media.LoadableElementBase;
    import org.osmf.media.MediaElement;
    import org.osmf.media.MediaResourceBase;
    import org.osmf.media.videoClasses.VideoSurface;
    import org.osmf.net.NetStreamAudioTrait;
    import org.osmf.net.StreamType;
    import org.osmf.net.StreamingURLResource;
    import org.osmf.traits.AudioTrait;
    import org.osmf.traits.BufferTrait;
    import org.osmf.traits.DVRTrait;
    import org.osmf.traits.LoadTrait;
    import org.osmf.traits.LoaderBase;
    import org.osmf.traits.MediaTraitType;
    import org.osmf.traits.PlayTrait;
    import org.osmf.traits.SeekTrait;
    import org.osmf.traits.TimeTrait;
    import org.osmf.utils.OSMFSettings;
    import org.osmf.events.MediaError;
    import org.osmf.events.MediaErrorEvent;

    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    public class HLSMediaElement extends LoadableElementBase {
        private var _hls : HLS;
        private var _stream : NetStream;
        private var _defaultduration : Number;
        private var videoSurface : VideoSurface;
        private var _smoothing : Boolean;

        public function HLSMediaElement(resource : MediaResourceBase, hls : HLS, duration : Number) {
            _hls = hls;
            _defaultduration = duration;
            super(resource, new HLSNetLoader(hls));
            _hls.addEventListener(HLSEvent.ERROR, _errorHandler);
        }

        protected function createVideo() : Video {
            return new Video();
        }

        override protected function createLoadTrait(resource : MediaResourceBase, loader : LoaderBase) : LoadTrait {
            return new HLSNetStreamLoadTrait(_hls, _defaultduration, loader, resource);
        }

        override protected function processLoadingState() : void {
            CONFIG::LOGGING {
            Log.debug("HLSMediaElement:processLoadingState");
            }
        }

        override protected function processReadyState() : void {
            CONFIG::LOGGING {
            Log.debug("HLSMediaElement:processReadyState");
            }
            initTraits();
        }

        override protected function processUnloadingState() : void {
            CONFIG::LOGGING {
            Log.debug("HLSMediaElement:processUnloadingState");
            }
            removeTrait(MediaTraitType.AUDIO);
            removeTrait(MediaTraitType.BUFFER);
            removeTrait(MediaTraitType.TIME);
            removeTrait(MediaTraitType.DISPLAY_OBJECT);
            removeTrait(MediaTraitType.PLAY);
            removeTrait(MediaTraitType.SEEK);
            removeTrait(MediaTraitType.DYNAMIC_STREAM);
            removeTrait(MediaTraitType.DVR);
            removeTrait(MediaTraitType.ALTERNATIVE_AUDIO);
            if (videoSurface != null) {
                videoSurface.attachNetStream(null);
                videoSurface = null;
            }
        }

        /**
         * Specifies whether the video should be smoothed (interpolated) when it is scaled.
         * For smoothing to work, the runtime must be in high-quality mode (the default).
         * The default value is false (no smoothing).  Set this property to true to take
         * advantage of mipmapping image optimization.
         *
         * @see flash.media.Video
         *
         *  @langversion 3.0
         *  @playerversion Flash 10
         *  @playerversion AIR 1.5
         *  @productversion OSMF 1.0
         **/
        public function get smoothing() : Boolean {
            return _smoothing;
        }

        public function set smoothing(value : Boolean) : void {
            _smoothing = value;
            if (videoSurface != null) {
                videoSurface.smoothing = value;
            }
        }

        private function initTraits() : void {
            _stream = _hls.stream;

            // Set the video's dimensions so that it doesn't appear at the wrong size.
            // We'll set the correct dimensions once the metadata is loaded.  (FM-206)
            videoSurface = new VideoSurface(OSMFSettings.enableStageVideo && OSMFSettings.supportsStageVideo, createVideo);
            videoSurface.smoothing = true;
            videoSurface.deblocking = 1;
            videoSurface.width = videoSurface.height = 0;
            videoSurface.attachNetStream(_stream);

            var audioTrait : AudioTrait = new NetStreamAudioTrait(_stream);
            addTrait(MediaTraitType.AUDIO, audioTrait);

            var ccTrait : HLSClosedCaptionsTrait = new HLSClosedCaptionsTrait(_hls);
            addTrait(HLSMediaTraitType.CLOSED_CAPTIONS, ccTrait);

            var bufferTrait : BufferTrait = new HLSBufferTrait(_hls);
            addTrait(MediaTraitType.BUFFER, bufferTrait);

            var timeTrait : TimeTrait = new HLSTimeTrait(_hls, _defaultduration);
            addTrait(MediaTraitType.TIME, timeTrait);

            var displayObjectTrait : HLSDisplayObjectTrait = new HLSDisplayObjectTrait(_hls,videoSurface, NaN, NaN);
            addTrait(MediaTraitType.DISPLAY_OBJECT, displayObjectTrait);

            // setup seek trait
            var seekTrait : SeekTrait = new HLSSeekTrait(_hls, timeTrait);
            addTrait(MediaTraitType.SEEK, seekTrait);

            if (_hls.levels.length > 1) {
                // setup dynamic stream trait
                var dsTrait : HLSDynamicStreamTrait = new HLSDynamicStreamTrait(_hls);
                addTrait(MediaTraitType.DYNAMIC_STREAM, dsTrait);
            }

            // retrieve stream type
            var streamType : String = (resource as StreamingURLResource).streamType;
            if (streamType == StreamType.DVR) {
                // add DvrTrait
                var dvrTrait : DVRTrait = new DVRTrait(true);
                addTrait(MediaTraitType.DVR, dvrTrait);
            }

            // setup drm trait
            // addTrait(MediaTraitType.DRM, drmTrait);

            // setup alternative audio trait
            var alternateAudioTrait : HLSAlternativeAudioTrait = new HLSAlternativeAudioTrait(_hls, this as MediaElement);
            addTrait(MediaTraitType.ALTERNATIVE_AUDIO, alternateAudioTrait);

            var playTrait : PlayTrait = new HLSPlayTrait(_hls);
            addTrait(MediaTraitType.PLAY, playTrait);
        }

        private function _errorHandler(event : HLSEvent) : void {
            var errorCode : int = ErrorManager.getMediaErrorCode(event);
            var errorMsg : String = ErrorManager.getMediaErrorMessage(event);
            CONFIG::LOGGING {
            Log.warn("HLS Error event received, dispatching MediaError " + errorCode + "," + errorMsg);
            }
            dispatchEvent(new MediaErrorEvent(MediaErrorEvent.MEDIA_ERROR, false, false, new MediaError(errorCode, errorMsg)));
        }
    }
}
