/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.osmf.plugins.utils {
    import org.mangui.hls.event.HLSError;
    import org.mangui.hls.event.HLSEvent;
    import org.osmf.events.MediaErrorCodes;

    public class ErrorManager {

        public static function getMediaErrorCode(event : HLSEvent) : int {
            var errorCode : int = MediaErrorCodes.NETSTREAM_PLAY_FAILED;
            if (event && event.error) {
                switch (event.error.code) {
                    case HLSError.FRAGMENT_LOADING_ERROR:
                    case HLSError.KEY_LOADING_ERROR:
                    case HLSError.MANIFEST_LOADING_IO_ERROR:
                        errorCode = MediaErrorCodes.IO_ERROR;
                        break;
                    case HLSError.FRAGMENT_LOADING_CROSSDOMAIN_ERROR:
                    case HLSError.KEY_LOADING_CROSSDOMAIN_ERROR:
                    case HLSError.MANIFEST_LOADING_CROSSDOMAIN_ERROR:
                        errorCode = MediaErrorCodes.SECURITY_ERROR
                        break;
                    case org.mangui.hls.event.HLSError.FRAGMENT_PARSING_ERROR:
                    case org.mangui.hls.event.HLSError.KEY_PARSING_ERROR:
                    case org.mangui.hls.event.HLSError.MANIFEST_PARSING_ERROR:
                        errorCode = MediaErrorCodes.NETSTREAM_FILE_STRUCTURE_INVALID;
                        break;
                    case org.mangui.hls.event.HLSError.TAG_APPENDING_ERROR:
                        errorCode = MediaErrorCodes.ARGUMENT_ERROR;
                        break;
                }
            }
            return errorCode;
        };

        public static function getMediaErrorMessage(event : HLSEvent) : String {
            return (event && event.error) ? event.error.msg : "Unknown error";
        };
    };
}