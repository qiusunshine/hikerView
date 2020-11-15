/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.osmf.plugins.traits {
    import org.mangui.hls.HLS;
    import org.mangui.hls.model.AudioTrack;
    import org.mangui.hls.event.HLSEvent;
    import org.osmf.events.AlternativeAudioEvent;
    import org.osmf.media.MediaElement;
    import org.osmf.net.StreamingItem;
    import org.osmf.traits.AlternativeAudioTrait;
    import org.osmf.utils.OSMFStrings;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    public class HLSAlternativeAudioTrait extends AlternativeAudioTrait {
        private var _hls : HLS;
        private var _media : MediaElement;
        private var _audioTrackList : Vector.<AudioTrack>;
        private var _numAlternativeAudioStreams : int;
        private var _activeTransitionIndex : int = DEFAULT_TRANSITION_INDEX;
        private var _lastTransitionIndex : int = INVALID_TRANSITION_INDEX;

        public function HLSAlternativeAudioTrait(hls : HLS, media : MediaElement) {
            CONFIG::LOGGING {
            Log.debug("HLSAlternativeAudioTrait()");
            }
            _hls = hls;
            _audioTrackList = _hls.audioTracks;
            _numAlternativeAudioStreams = _audioTrackList.length - 1;
            super(_numAlternativeAudioStreams);
            _media = media;
            _hls.addEventListener(HLSEvent.AUDIO_TRACK_SWITCH, _audioTrackChangedHandler);
            _hls.addEventListener(HLSEvent.AUDIO_TRACKS_LIST_CHANGE, _audioTrackListChangedHandler);
        }

        override public function dispose() : void {
            CONFIG::LOGGING {
            Log.debug("HLSAlternativeAudioTrait:dispose");
            }
            _hls.removeEventListener(HLSEvent.AUDIO_TRACK_SWITCH, _audioTrackChangedHandler);
            _hls.removeEventListener(HLSEvent.AUDIO_TRACKS_LIST_CHANGE, _audioTrackListChangedHandler);
            super.dispose();
        }

        override public function get numAlternativeAudioStreams() : int {
            CONFIG::LOGGING {
            Log.debug("HLSAlternativeAudioTrait:numAlternativeAudioStreams:" + _numAlternativeAudioStreams);
            }
            return _numAlternativeAudioStreams;
        }

        override public function getItemForIndex(index : int) : StreamingItem {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:getItemForIndex(" + index + ")");
            }
            if (index <= INVALID_TRANSITION_INDEX || index >= numAlternativeAudioStreams) {
                throw new RangeError(OSMFStrings.getString(OSMFStrings.ALTERNATIVEAUDIO_INVALID_INDEX));
            }

            if (index == DEFAULT_TRANSITION_INDEX) {
                return null;
            }
            var name : String = _audioTrackList[index + 1].title;
            var streamItem : StreamingItem = new StreamingItem("AUDIO", name);
            streamItem.info.label = name;
            return streamItem;
        }

        override protected function endSwitching(index : int) : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:endSwitching(" + index + ")");
            }
            if (switching) {
                executeSwitching(_indexToSwitchTo);
            }
            super.endSwitching(index);
        }

        protected function executeSwitching(indexToSwitchTo : int) : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:executeSwitching(" + indexToSwitchTo + ")");
            }
            if (_lastTransitionIndex != indexToSwitchTo) {
                _activeTransitionIndex = indexToSwitchTo;
                _hls.audioTrack = indexToSwitchTo + 1;
            }
        }

        private function _audioTrackChangedHandler(event : HLSEvent) : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:_audioTrackChangedHandler");
            }
            setSwitching(false, _activeTransitionIndex);
            _lastTransitionIndex = _activeTransitionIndex;
        }

        private function _audioTrackListChangedHandler(event : HLSEvent) : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:_audioTrackListChangedHandler");
            }
            _audioTrackList = _hls.audioTracks;
            if (_audioTrackList.length > 0) {
                // try to change default Audio Track Title for GrindPlayer ...
                if (_audioTrackList[0].title.indexOf("TS/") == -1) {
                    CONFIG::LOGGING {
                    Log.debug("default audio track title:" + _audioTrackList[0].title);
                    }
                    _media.resource.addMetadataValue("defaultAudioLabel", _audioTrackList[0].title);
                }
            }
            _numAlternativeAudioStreams = _audioTrackList.length - 1;
            if (_numAlternativeAudioStreams > 0) {
                dispatchEvent(new AlternativeAudioEvent(AlternativeAudioEvent.NUM_ALTERNATIVE_AUDIO_STREAMS_CHANGE, false, false, false));
            }
        }
    }
}
