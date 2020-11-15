/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.utils {
    import flash.utils.getTimer;
    import flash.display.DisplayObject;
    import flash.utils.ByteArray;
    import flash.events.Event;

    /**
     * Contains Utility functions for AES-128 CBC Decryption
     */
    public class AES {
        private var _key : FastAESKey;
        //private var _keyArray : ByteArray;
        private var iv0 : uint;
        private var iv1 : uint;
        private var iv2 : uint;
        private var iv3 : uint;
        /* callback function upon decrypt progress */
        private var _progress : Function;
        /* callback function upon decrypt complete */
        private var _complete : Function;
        /** Byte data to be decrypt **/
        private var _data : ByteArray;
        /** read position **/
        private var _readPosition : uint;
        /** write position **/
        private var _writePosition : uint;
        /** chunk size to avoid blocking **/
        private static const CHUNK_SIZE : uint = 2048;
        /** is bytearray full ? **/
        private var _dataComplete : Boolean;
        /** display object used for ENTER_FRAME listener */
        private var _displayObject : DisplayObject;

        public function AES(displayObject : DisplayObject, key : ByteArray, iv : ByteArray, notifyprogress : Function, notifycomplete : Function) {
            // _keyArray = key;
            _key = new FastAESKey(key);
            iv.position = 0;
            iv0 = iv.readUnsignedInt();
            iv1 = iv.readUnsignedInt();
            iv2 = iv.readUnsignedInt();
            iv3 = iv.readUnsignedInt();
            _data = new ByteArray();
            _dataComplete = false;
            _progress = notifyprogress;
            _complete = notifycomplete;
            _readPosition = 0;
            _writePosition = 0;
            _displayObject = displayObject;
        }

        public function append(data : ByteArray) : void {
            // CONFIG::LOGGING {
            // Log.info("notify append");
            // }
            _data.position = _writePosition;
            _data.writeBytes(data);
            if (_writePosition == 0) {
                _displayObject.addEventListener(Event.ENTER_FRAME, _decryptTimer);
            }
            _writePosition += data.length;
        }

        public function notifycomplete() : void {
            // CONFIG::LOGGING {
            // Log.info("notify complete");
            // }
            _dataComplete = true;
        }

        public function cancel() : void {
            _displayObject.removeEventListener(Event.ENTER_FRAME, _decryptTimer);
        }

        private function _decryptTimer(e : Event) : void {
            var start_time : int = getTimer();
            var decrypted : Boolean;
            do {
                decrypted = _decryptChunk();
            // dont spend more than 10ms in the decrypt timer to avoid blocking/freezing video
            // if frame rate is 60fps, we have 1000/60 = 16.6ms budget total per frame
            } while (decrypted && (getTimer() - start_time) < 10);
        }

        /** decrypt a small chunk of packets each time to avoid blocking **/
        private function _decryptChunk() : Boolean {
            _data.position = _readPosition;
            var decryptdata : ByteArray;
            if (_data.bytesAvailable) {
                if (_data.bytesAvailable <= CHUNK_SIZE) {
                    if (_dataComplete) {
                        // CONFIG::LOGGING {
                        // Log.info("data complete, last chunk");
                        // }
                        _readPosition += _data.bytesAvailable;
                        decryptdata = _decryptCBC(_data, _data.bytesAvailable);
                        unpad(decryptdata);
                    } else {
                        // data not complete, and available data less than chunk size, return
                        return false;
                    }
                } else {
                    _readPosition += CHUNK_SIZE;
                    decryptdata = _decryptCBC(_data, CHUNK_SIZE);
                }
                _progress(decryptdata);
                return true;
            } else {
                if (_dataComplete) {
                    CONFIG::LOGGING {
                        Log.debug("AES:data+decrypt completed, callback");
                    }
                    // callback
                    _complete();
                    _displayObject.removeEventListener(Event.ENTER_FRAME, _decryptTimer);
                }
                return false;
            }
        }

        /* Cypher Block Chaining Decryption, refer to
         * http://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Cipher-block_chaining_
         * for algorithm description
         */
        private function _decryptCBC(crypt : ByteArray, len : uint) : ByteArray {
            var src : Vector.<uint> = new Vector.<uint>(4);
            var dst : Vector.<uint> = new Vector.<uint>(4);
            var decrypt : ByteArray = new ByteArray();
            decrypt.length = len;

            for (var i : uint = 0; i < len / 16; i++) {
                // read src byte array
                src[0] = crypt.readUnsignedInt();
                src[1] = crypt.readUnsignedInt();
                src[2] = crypt.readUnsignedInt();
                src[3] = crypt.readUnsignedInt();

                // AES decrypt src vector into dst vector
                _key.decrypt128(src, dst);

                // CBC : write output = XOR(decrypted,IV)
                decrypt.writeUnsignedInt(dst[0] ^ iv0);
                decrypt.writeUnsignedInt(dst[1] ^ iv1);
                decrypt.writeUnsignedInt(dst[2] ^ iv2);
                decrypt.writeUnsignedInt(dst[3] ^ iv3);

                // CBC : next IV = (input)
                iv0 = src[0];
                iv1 = src[1];
                iv2 = src[2];
                iv3 = src[3];
            }
            decrypt.position = 0;
            return decrypt;
        }

        public function unpad(a : ByteArray) : void {
            var c : uint = a.length % 16;
            if (c != 0) throw new Error("PKCS#5::unpad: ByteArray.length isn't a multiple of the blockSize");
            c = a[a.length - 1];
            for (var i : uint = c; i > 0; i--) {
                var v : uint = a[a.length - 1];
                a.length--;
                if (c != v) throw new Error("PKCS#5:unpad: Invalid padding value. expected [" + c + "], found [" + v + "]");
            }
        }

        public function destroy() : void {
            _key.dispose();
            // _key = null;
        }
    }
}
