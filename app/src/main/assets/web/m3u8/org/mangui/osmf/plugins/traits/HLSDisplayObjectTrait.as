/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins.traits {

    import flash.display.DisplayObject;
    import flash.display.Stage;
    import flash.events.Event;
    
    import org.mangui.hls.HLS;
    import org.osmf.media.videoClasses.VideoSurface;
    import org.osmf.traits.DisplayObjectTrait;
    
    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    public class HLSDisplayObjectTrait extends DisplayObjectTrait {
        private var videoSurface : VideoSurface;
        private var _hls : HLS;

        public function HLSDisplayObjectTrait(hls : HLS, videoSurface : DisplayObject, mediaWidth : int = 0, mediaHeight : int = 0) {
            CONFIG::LOGGING {
            Log.debug("HLSDisplayObjectTrait()");
            }
            _hls = hls;
            super(videoSurface, mediaWidth, mediaHeight);
            this.videoSurface = videoSurface as VideoSurface;

            if (this.videoSurface is VideoSurface)
                this.videoSurface.addEventListener(Event.ADDED_TO_STAGE, onStage);
        }

        override public function dispose() : void {
            CONFIG::LOGGING {
            Log.debug("HLSDisplayObjectTrait:dispose");
            }
            videoSurface.removeEventListener(Event.ENTER_FRAME, onFrame);
            super.dispose();
        }

        private function onStage(event : Event) : void {
            _hls.stage = event.target.stage as Stage;
            videoSurface.removeEventListener(Event.ADDED_TO_STAGE, onStage);
            videoSurface.addEventListener(Event.ENTER_FRAME, onFrame);
        }

        private function onFrame(event : Event) : void {
            var newWidth : int = videoSurface.videoWidth;
            var newHeight : int = videoSurface.videoHeight;
            if (newWidth != 0 && newHeight != 0 && (newWidth != mediaWidth || newHeight != mediaHeight)) {
                // If there is no layout, set as no scale.
                if (videoSurface.width == 0 && videoSurface.height == 0) {
                    videoSurface.width = newWidth;
                    videoSurface.height = newHeight;
                }
                CONFIG::LOGGING {
                Log.info("HLSDisplayObjectTrait:setMediaSize(" + newWidth + "," + newHeight + ")");
                }
                setMediaSize(newWidth, newHeight);
            }
            // videoSurface.removeEventListener(Event.ENTER_FRAME, onFrame);
        }
    }
}