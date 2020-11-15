/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.playlist {
    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    /**
     * Facilitates extracting information from a data URI.
     */
    public class DataUri {

        private static const DATA_PROTOCOL : String = "data:";
        private static const BASE_64 : String = "base64";

        private var _dataUri : String;

        public function DataUri(dataUri : String) {
            _dataUri = dataUri;
        }

        /**
         * @return Returns the data portion of the data URI if it is able extract the information,
         * null otherwise.
         */
        public function extractData() : String {
            if (_dataUri == null) {
                return null;
            }

            var base64Index : int = _dataUri.indexOf(BASE_64 + ',');
            var dataIndex : int = _dataUri.indexOf(',') + 1;

            if (dataIndex > _dataUri.length) {
                return null;
            }

            var data : String = _dataUri.substr(dataIndex);
            return (base64Index === -1) ? _extractPlainData(data) : _extractBase64Data(data);
        }

        /**
         * Data URIs support base 64 encoding the data section.
         * This is not typically used for plain text files, which includes HLS manifests.
         * As such, decoded base 64 data sections is not currently (6/18/14) supported.
         * @param data
         * @return
         */
        private function _extractBase64Data(data : String) : String {
            CONFIG::LOGGING {
            Log.warn("Base 64 encoded Data URIs are not supported.");
            }
            return null;
        }

        /**
         * @param data
         * @return The URL decoded data section from the data URI.
         */
        private function _extractPlainData(data : String) : String {
            var decodedData : String = decodeURIComponent(data);
            CONFIG::LOGGING {
            Log.debug2("Decoded data from data URI into: " + decodedData);
            }
            return decodedData;
        }

        /**
         * @param dataUri
         * @return True if the provided string is a data URI, false otherwise.
         */
        public static function isDataUri(dataUri : String) : Boolean {
            return dataUri != null && dataUri.indexOf(DATA_PROTOCOL) === 0;
        }
    }
}
