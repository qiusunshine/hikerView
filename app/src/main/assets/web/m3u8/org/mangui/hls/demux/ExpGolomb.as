/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {
    import flash.utils.ByteArray;
    public class ExpGolomb {
        private var _data : ByteArray;
        private var _bit : int;
        private var _curByte : uint;

        public function ExpGolomb(data : ByteArray) {
            _data = data;
            _bit = -1;
        }

        private function _readBit() : uint {
            var res : uint;
            if (_bit == -1) {
                // read next
                _curByte = _data.readByte();
                _bit = 7;
            }
            res = _curByte & (1 << _bit) ? 1 : 0;
            _bit--;
            return res;
        }

        public function readBoolean() : Boolean {
            return (_readBit() == 1);
        }

        public function readBits(nbBits : uint) : int {
            var val : int = 0;
            for (var i : uint = 0; i < nbBits; ++i)
                val = (val << 1) + _readBit();
            return val;
        }

        public function readUE() : uint {
            var nbZero : uint = 0;
            while (_readBit() == 0)
                ++nbZero;
            var x : uint = readBits(nbZero);
            return x + (1 << nbZero) - 1;
        }

        public function readSE() : uint {
            var value : int = readUE();
            // the number is odd if the low order bit is set
            if (0x01 & value) {
                // add 1 to make it even, and divide by 2
                return (1 + value) >> 1;
            } else {
                // divide by two then make it negative
                return -1 * (value >> 1);
            }
        }
    }
}
