/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins.loader {
    import org.mangui.hls.HLS;
    import org.mangui.hls.event.HLSEvent;
	import org.mangui.hls.model.Level;
    import org.mangui.hls.constant.HLSTypes;
    import org.mangui.osmf.plugins.HLSMediaElement;
    import org.mangui.osmf.plugins.utils.ErrorManager;
    import org.osmf.elements.proxyClasses.LoadFromDocumentLoadTrait;
    import org.osmf.events.MediaError;
    import org.osmf.events.MediaErrorEvent;
    import org.osmf.media.MediaElement;
    import org.osmf.media.MediaResourceBase;
    import org.osmf.media.URLResource;
    import org.osmf.net.DynamicStreamingItem;
    import org.osmf.net.DynamicStreamingResource;
    import org.osmf.net.StreamType;
    import org.osmf.net.StreamingURLResource;
    import org.osmf.traits.LoadState;
    import org.osmf.traits.LoadTrait;
    import org.osmf.traits.LoaderBase;

    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    /**
     * Loader for .m3u8 playlist file.
     * Works like a F4MLoader
     */
    public class HLSLoaderBase extends LoaderBase {
        private var _loadTrait : LoadTrait;
        /** Reference to the framework. **/
        private var _hls : HLS = null;

        public function HLSLoaderBase() {
            super();
        }

        public static function canHandle(resource : MediaResourceBase) : Boolean {
            if (resource !== null && resource is URLResource) {
                var urlResource : URLResource = URLResource(resource);
                //  check for m3u/m3u8
                if (urlResource.url.search(/(https?|file)\:\/\/.*?\m3u(\?.*)?/i) !== -1) {
                    return true;
                }

                var contentType : Object = urlResource.getMetadataValue("content-type");
                if (contentType && contentType is String) {
                    // If the filename doesn't include a .m3u or m3u8 extension, but
                    // explicit content-type metadata is found on the
                    // URLResource, we can handle it.  Must be either of:
                    // - "application/x-mpegURL"
                    // - "vnd.apple.mpegURL"
                    if ((contentType as String).search(/(application\/x-mpegURL|vnd.apple.mpegURL)/i) !== -1) {
                        return true;
                    }
                }
            }
            return false;
        }

        override public function canHandleResource(resource : MediaResourceBase) : Boolean {
            return canHandle(resource);
        }

        override protected function executeLoad(loadTrait : LoadTrait) : void {
            _loadTrait = loadTrait;
            updateLoadTrait(loadTrait, LoadState.LOADING);

            if (_hls != null) {
                _hls.removeEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
                _hls.removeEventListener(HLSEvent.ERROR, _errorHandler);
                _hls.dispose();
                _hls = null;
            }
            _hls = new HLS();
            _hls.addEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.addEventListener(HLSEvent.ERROR, _errorHandler);
            /* load playlist */
            _hls.load(URLResource(loadTrait.resource).url);
        }

        override protected function executeUnload(loadTrait : LoadTrait) : void {
            updateLoadTrait(loadTrait, LoadState.UNINITIALIZED);
        }

        /** Update video A/R on manifest load. **/
        private function _manifestLoadedHandler(event : HLSEvent) : void {
            var resource : MediaResourceBase = URLResource(_loadTrait.resource);

            // retrieve stream type
            var streamType : String = (resource as StreamingURLResource).streamType;
            if (streamType == null || streamType == StreamType.LIVE_OR_RECORDED) {
                if (_hls.type == HLSTypes.LIVE) {
                    streamType = StreamType.LIVE;
                } else {
                    streamType = StreamType.RECORDED;
                }
            }

            var levels : Vector.<Level> = _hls.levels;
            var nbLevel : int = levels.length;
            var urlRes : URLResource = resource as URLResource;
            var dynamicRes : DynamicStreamingResource = new DynamicStreamingResource(urlRes.url);
            var streamItems : Vector.<DynamicStreamingItem> = new Vector.<DynamicStreamingItem>();

            for (var i : int = 0; i < nbLevel; i++) {
                if (levels[i].width) {
                    streamItems.push(new DynamicStreamingItem(level2label(levels[i]), levels[i].bitrate / 1024, levels[i].width, levels[i].height));
                } else {
                    streamItems.push(new DynamicStreamingItem(level2label(levels[i]), levels[i].bitrate / 1024));
                }
            }
            dynamicRes.streamItems = streamItems;
            dynamicRes.initialIndex = _hls.startLevel;
            resource = dynamicRes;
            // set Stream Type
            var streamUrlRes : StreamingURLResource = resource as StreamingURLResource;
            streamUrlRes.streamType = streamType;
            try {
                var loadedElem : MediaElement = new HLSMediaElement(resource, _hls, event.levels[_hls.startLevel].duration);
                LoadFromDocumentLoadTrait(_loadTrait).mediaElement = loadedElem;
                updateLoadTrait(_loadTrait, LoadState.READY);
            } catch(e : Error) {
                updateLoadTrait(_loadTrait, LoadState.LOAD_ERROR);
                _loadTrait.dispatchEvent(new MediaErrorEvent(MediaErrorEvent.MEDIA_ERROR, false, false, new MediaError(e.errorID, e.message)));
            }
        };

        private function level2label(level : Level) : String {
            if (level.name) {
                return level.name;
            } else {
                if (level.height) {
                    return(level.height + 'p / ' + Math.round(level.bitrate / 1024) + 'kb');
                } else {
                    return(Math.round(level.bitrate / 1024) + 'kb');
                }
            }
        }

        private function _errorHandler(event : HLSEvent) : void {
            var errorCode : int = ErrorManager.getMediaErrorCode(event);
            var errorMsg : String = ErrorManager.getMediaErrorMessage(event);
            CONFIG::LOGGING {
            Log.warn("HLS Error event received, dispatching MediaError " + errorCode + "," + errorMsg);
            }
            updateLoadTrait(_loadTrait, LoadState.LOAD_ERROR);
            _loadTrait.dispatchEvent(new MediaErrorEvent(MediaErrorEvent.MEDIA_ERROR, false, false, new MediaError(errorCode, errorMsg)));
        }
    }
}
