/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.event {
    import org.mangui.hls.model.Level;

    import flash.events.Event;

    /** Event fired when an error prevents playback. **/
    public class HLSEvent extends Event {
        /** Identifier for a manifest loading event, triggered after a call to hls.load(url) **/
        public static const MANIFEST_LOADING : String = "hlsEventManifestLoading";
        /** Identifier for a manifest parsed event,
         * triggered after main manifest has been retrieved and parsed.
         * hls playlist may not be playable yet, in case of adaptive streaming, start level playlist is not downloaded yet at that stage */
        public static const MANIFEST_PARSED : String = "hlsEventManifestParsed";
        /** Identifier for a manifest loaded event, when this event is received, main manifest and start level has been retrieved */
        public static const MANIFEST_LOADED : String = "hlsEventManifestLoaded";
        /** Identifier for a level loading event  **/
        public static const LEVEL_LOADING : String = "hlsEventLevelLoading";
        /** Identifier for a level loading aborted event  **/
        public static const LEVEL_LOADING_ABORTED : String = "hlsEventLevelLoadingAborted";
        /** Identifier for a level loaded event  **/
        public static const LEVEL_LOADED : String = "hlsEventLevelLoaded";
        /** Identifier for a level switch event. **/
        public static const LEVEL_SWITCH : String = "hlsEventLevelSwitch";
        /** Identifier for a level ENDLIST event. **/
        public static const LEVEL_ENDLIST : String = "hlsEventLevelEndList";
        /** Identifier for a fragment loading event. **/
        public static const FRAGMENT_LOADING : String = "hlsEventFragmentLoading";
        /** Identifier for a fragment loaded event. **/
        public static const FRAGMENT_LOADED : String = "hlsEventFragmentLoaded";
        /* Identifier for fragment load aborting for emergency switch down */
        public static const FRAGMENT_LOAD_EMERGENCY_ABORTED : String = "hlsEventFragmentLoadEmergencyAborted";
        /** Identifier for a fragment playing event. **/
        public static const FRAGMENT_PLAYING : String = "hlsEventFragmentPlaying";
        /** Identifier for a fragment skipping event. **/
        public static const FRAGMENT_SKIPPED : String = "hlsEventFragmentSkipped";
        /** Identifier for a audio tracks list change **/
        public static const AUDIO_TRACKS_LIST_CHANGE : String = "audioTracksListChange";
        /** Identifier for a audio track switch **/
        public static const AUDIO_TRACK_SWITCH : String = "audioTrackSwitch";
        /** Identifier for a audio level loading event  **/
        public static const AUDIO_LEVEL_LOADING : String = "hlsEventAudioLevelLoading";
        /** Identifier for a audio level loaded event  **/
        public static const AUDIO_LEVEL_LOADED : String = "hlsEventAudioLevelLoaded";
        /** Identifier for audio/video TAGS loaded event. **/
        public static const TAGS_LOADED : String = "hlsEventTagsLoaded";
        /** Identifier when last fragment of playlist has been loaded **/
        public static const LAST_VOD_FRAGMENT_LOADED : String = "hlsEventLastFragmentLoaded";
        /** Identifier for a playback warning event. **/
        public static const WARNING : String = "hlsEventWarning";
        /** Identifier for a playback error event. **/
        public static const ERROR : String = "hlsEventError";
        /** Identifier for a playback media time change event. **/
        public static const MEDIA_TIME : String = "hlsEventMediaTime";
        /** Identifier for a playback state switch event. **/
        public static const PLAYBACK_STATE : String = "hlsPlaybackState";
        /** Identifier for a seek state switch event. **/
        public static const SEEK_STATE : String = "hlsSeekState";
        /** Identifier for stream type changes: VoD or Live, type will be stored in 'streamType' field **/
        public static const STREAM_TYPE_DID_CHANGE:String = "hlsEventStreamTypeDidChange";
        /** Identifier for a playback complete event. **/
        public static const PLAYBACK_COMPLETE : String = "hlsEventPlayBackComplete";
        /** Identifier for a Playlist Duration updated event **/
        public static const PLAYLIST_DURATION_UPDATED : String = "hlsPlayListDurationUpdated";
        /** Identifier for a ID3 updated event **/
        public static const ID3_UPDATED : String = "hlsID3Updated";
        /** Identifier for a fps drop event **/
        public static const FPS_DROP : String = "hlsFPSDrop";
        /** Identifier for a fps drop level capping event **/
        public static const FPS_DROP_LEVEL_CAPPING : String = "hlsFPSDropLevelCapping";
        /** Identifier for a fps drop smooth level switch event **/
        public static const FPS_DROP_SMOOTH_LEVEL_SWITCH : String = "hlsFPSDropSmoothLevelSwitch";
        /** Identifier for a live loading stalled event **/
        public static const LIVE_LOADING_STALLED : String = "hlsLiveLoadingStalled";
        /** Identifier for a Stage set event **/
        public static const STAGE_SET : String = "hlsStageSet";
		/**ckplayer-m3u8 about m3u8 list**/
		public static const M3U8_LIST : String = "hlsM3u8List";
		
        /** The current url **/
        public var url : String;
        /** The current quality level. **/
        public var level : int;
        /** The current playlist duration. **/
        public var duration : Number;
        /** The list with quality levels. **/
        public var levels : Vector.<Level>;
        /** The error message. **/
        public var error : HLSError;
        /** Load Metrics. **/
        public var loadMetrics : HLSLoadMetrics;
        /** Play Metrics. **/
        public var playMetrics : HLSPlayMetrics;
        /** The time position. **/
        public var mediatime : HLSMediatime;
        /** The new playback state. **/
        public var state : String;
        /** The new stream type value **/
        public var streamType: String;
        /** The current audio track **/
        public var audioTrack : int;
        /** a complete ID3 payload from PES, as a hex dump **/
        public var ID3Data : String;
		/**ckplayer-m3u8 about m3u8 list**/
		public var m3u8list :Array;
		
        /** Assign event parameter and dispatch. **/
        public function HLSEvent(type : String, parameter : *=null, parameter2 : *=null) {
            switch(type) {
                case MANIFEST_LOADING:
                case FRAGMENT_LOADING:
                    url = parameter as String;
                    break;
                case ERROR:
                case WARNING:
                    error = parameter as HLSError;
                    break;
                case TAGS_LOADED:
                case FRAGMENT_LOADED:
                case FRAGMENT_LOAD_EMERGENCY_ABORTED:
                case LEVEL_LOADED:
                case AUDIO_LEVEL_LOADED:
                    loadMetrics = parameter as HLSLoadMetrics;
                    break;
                case MANIFEST_PARSED:
                case MANIFEST_LOADED:
                    levels = parameter as Vector.<Level>;
                    if(parameter2) {
                        loadMetrics = parameter2 as HLSLoadMetrics;
                    }
                    break;
                case MEDIA_TIME:
                    mediatime = parameter as HLSMediatime;
                    break;
                case PLAYBACK_STATE:
                case SEEK_STATE:
                    state = parameter as String;
                    break;
                case LEVEL_LOADING:
                case LEVEL_LOADING_ABORTED:
                case LEVEL_SWITCH:
                case LEVEL_ENDLIST:
                case AUDIO_LEVEL_LOADING:
                case FPS_DROP:
                case FPS_DROP_LEVEL_CAPPING:
                    level = parameter as int;
                    break;
                case PLAYLIST_DURATION_UPDATED:
                case FRAGMENT_SKIPPED:
                    duration = parameter as Number;
                    break;
                case ID3_UPDATED:
                    ID3Data = parameter as String;
                    break;
                case FRAGMENT_PLAYING:
                    playMetrics = parameter as HLSPlayMetrics;
                    break;
                case HLSEvent.STREAM_TYPE_DID_CHANGE:
                    // Stream Type is required
                    streamType = String(parameter);
                    break;
				case M3U8_LIST:
                    m3u8list = parameter as Array;
                    break;
                default:
                    break;
            }
            super(type, false, false);
        }
    }
}
