/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.event {
    /** Error Identifier **/
    public class HLSError {
        public static const OTHER_ERROR : int = 0;
        public static const MANIFEST_LOADING_CROSSDOMAIN_ERROR : int = 1;
        public static const MANIFEST_LOADING_IO_ERROR : int = 2;
        public static const MANIFEST_PARSING_ERROR : int = 3;
        public static const FRAGMENT_LOADING_CROSSDOMAIN_ERROR : int = 4;
        public static const FRAGMENT_LOADING_ERROR : int = 5;
        public static const FRAGMENT_PARSING_ERROR : int = 6;
        public static const KEY_LOADING_CROSSDOMAIN_ERROR : int = 7;
        public static const KEY_LOADING_ERROR : int = 8;
        public static const KEY_PARSING_ERROR : int = 9;
        public static const TAG_APPENDING_ERROR : int = 10;

        private var _code : int;
        private var _url : String;
        private var _msg : String;

        public function HLSError(code : int, url : String, msg : String) {
            _code = code;
            _url = url;
            _msg = msg;
        }

        public function get code() : int {
            return _code;
        }

        public function get msg() : String {
            return _msg;
        }

        public function get url() : String {
            return _url;
        }

        public function toString() : String {
            return "HLSError(code/url/msg)=" + _code + "/" + _url + "/" + _msg;
        }
    }
}