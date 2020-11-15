/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.utils {
    import flash.utils.ByteArray;

    /* word based AES encryption/decryption
     * inspired by
     * https://code.google.com/p/crypto-js/source/browse/tags/3.1.2/src/aes.js
     */
    public class FastAESKey {
        /* private data, specific to each key */
        private var keySize : uint;
        private var nRounds : uint;
        private var ksRows : uint;
        private var keySchedule : Vector.<uint>;
        private var invKeySchedule : Vector.<uint>;
        private var keyWords : Vector.<uint>;
        private var state : Vector.<uint>;
        // static Lookup tables
        private static var _SBOX : Vector.<uint>;
        private static var _INV_SBOX : Vector.<uint>;
        private static var _INV_SUB_MIX_0 : Vector.<uint>;
        private static var _INV_SUB_MIX_1 : Vector.<uint>;
        private static var _INV_SUB_MIX_2 : Vector.<uint>;
        private static var _INV_SUB_MIX_3 : Vector.<uint>;
        private static var _RCON : Vector.<uint>;
        // static initializer
        {
        _initTable();
        };
        private static function _initTable() : void {
            _SBOX = new Vector.<uint>(256);
            _INV_SBOX = new Vector.<uint>(256);
            _INV_SUB_MIX_0 = new Vector.<uint>(256);
            _INV_SUB_MIX_1 = new Vector.<uint>(256);
            _INV_SUB_MIX_2 = new Vector.<uint>(256);
            _INV_SUB_MIX_3 = new Vector.<uint>(256);
            _RCON = new <uint>[0x0, 0x1, 0x2, 0x4, 0x8, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36];

            // Compute double table
            var i : int;
            var d : Vector.<uint> = new Vector.<uint>(256);
            for (i = 0; i < 256; i++) {
                if (i < 128) {
                    d[i] = i << 1;
                } else {
                    d[i] = (i << 1) ^ 0x11b;
                }
            }
            // Walk GF(2^8)
            var x : uint = 0;
            var xi : uint = 0;
            for (i = 0; i < 256; i++) {
                // Compute sbox
                var sx : uint = xi ^ (xi << 1) ^ (xi << 2) ^ (xi << 3) ^ (xi << 4);
                sx = (sx >>> 8) ^ (sx & 0xff) ^ 0x63;
                _SBOX[x] = sx;
                _INV_SBOX[sx] = x;

                // Compute multiplication
                var x2 : uint = d[x];
                var x4 : uint = d[x2];
                var x8 : uint = d[x4];

                // Compute inv sub bytes, inv mix columns tables
                var t: uint  = (x8 * 0x1010101) ^ (x4 * 0x10001) ^ (x2 * 0x101) ^ (x * 0x1010100);
                _INV_SUB_MIX_0[sx] = (t << 24) | (t >>> 8);
                _INV_SUB_MIX_1[sx] = (t << 16) | (t >>> 16);
                _INV_SUB_MIX_2[sx] = (t << 8) | (t >>> 24);
                _INV_SUB_MIX_3[sx] = t;

                // Compute next counter
                if (!x) {
                    x = xi = 1;
                } else {
                    x = x2 ^ d[d[d[x8 ^ x2]]];
                    xi ^= d[d[xi]];
                }
            }
        }

        public function FastAESKey(key : ByteArray) {
            keySize = key.length / 4;
            // Compute number of rounds
            nRounds = keySize + 6;
            // Compute number of key schedule rows
            ksRows = (nRounds + 1) * 4;
            state = new Vector.<uint>(keySize);
            keyWords = new Vector.<uint>(keySize);
            key.position = 0;
            for (var i : int = 0; i < keySize; i++) {
                keyWords[i] = key.readUnsignedInt();
            }
            expandKey();
        }

        private function expandKey() : void {
            this.keySchedule = new Vector.<uint>(ksRows);
            for (var ksRow : int = 0; ksRow < ksRows; ksRow++) {
                if (ksRow < keySize) {
                    keySchedule[ksRow] = keyWords[ksRow];
                } else {
                    var t : uint = keySchedule[ksRow - 1];

                    if (!(ksRow % keySize)) {
                        // Rot word
                        t = (t << 8) | (t >>> 24);

                        // Sub word
                        t = (_SBOX[t >>> 24] << 24) | (_SBOX[(t >>> 16) & 0xff] << 16) | (_SBOX[(t >>> 8) & 0xff] << 8) | _SBOX[t & 0xff];

                        // Mix Rcon
                        t ^= _RCON[(ksRow / keySize) | 0] << 24;
                    } else if (keySize > 6 && ksRow % keySize == 4) {
                        // Sub word
                        t = (_SBOX[t >>> 24] << 24) | (_SBOX[(t >>> 16) & 0xff] << 16) | (_SBOX[(t >>> 8) & 0xff] << 8) | _SBOX[t & 0xff];
                    }

                    keySchedule[ksRow] = keySchedule[ksRow - keySize] ^ t;
                }
            }
            // Compute inv key schedule
            this.invKeySchedule = new Vector.<uint>(ksRows);
            for (var invKsRow : int = 0; invKsRow < ksRows; invKsRow++) {
                ksRow = ksRows - invKsRow;

                if (invKsRow % 4) {
                    t = keySchedule[ksRow];
                } else {
                    t = keySchedule[ksRow - 4];
                }

                if (invKsRow < 4 || ksRow <= 4) {
                    invKeySchedule[invKsRow] = t;
                } else {
                    invKeySchedule[invKsRow] = _INV_SUB_MIX_0[_SBOX[t >>> 24]] ^ _INV_SUB_MIX_1[_SBOX[(t >>> 16) & 0xff]] ^ _INV_SUB_MIX_2[_SBOX[(t >>> 8) & 0xff]] ^ _INV_SUB_MIX_3[_SBOX[t & 0xff]];
                }
            }
        }
        
        public function decrypt128(input : Vector.<uint>,output : Vector.<uint>) : void {
            // Swap 2nd and 4th rows
          state[0] = input[0];
          state[1] = input[3];
          state[2] = input[2];
          state[3] = input[1];          
          _doCryptBlock(invKeySchedule, _INV_SUB_MIX_0, _INV_SUB_MIX_1, _INV_SUB_MIX_2, _INV_SUB_MIX_3, _INV_SBOX);
          // Inv swap 2nd and 4th rows
          output[0] = state[0];
          output[1] = state[3];
          output[2] = state[2];
          output[3] = state[1];
        }

        private function  _doCryptBlock(keySchedule : Vector.<uint>, SUB_MIX_0 : Vector.<uint>, SUB_MIX_1 : Vector.<uint>, SUB_MIX_2 : Vector.<uint>, SUB_MIX_3 : Vector.<uint>, SBOX : Vector.<uint>) : void {
            // Shortcut
            // Get input, add round key
            var s0 : uint = state[0] ^ keySchedule[0];
            var s1 : uint = state[1] ^ keySchedule[1];
            var s2 : uint = state[2] ^ keySchedule[2];
            var s3 : uint = state[3] ^ keySchedule[3];

            // Key schedule row counter
            var ksRow : uint = 4;
            var t0 : uint;
            var t1 : uint;
            var t2 : uint;
            var t3 : uint;

            // Rounds
            for (var round : int = 1; round < nRounds; round++) {
                // Shift rows, sub bytes, mix columns, add round key
                t0 = SUB_MIX_0[s0 >>> 24] ^ SUB_MIX_1[(s1 >>> 16) & 0xff] ^ SUB_MIX_2[(s2 >>> 8) & 0xff] ^ SUB_MIX_3[s3 & 0xff] ^ keySchedule[ksRow++];
                t1 = SUB_MIX_0[s1 >>> 24] ^ SUB_MIX_1[(s2 >>> 16) & 0xff] ^ SUB_MIX_2[(s3 >>> 8) & 0xff] ^ SUB_MIX_3[s0 & 0xff] ^ keySchedule[ksRow++];
                t2 = SUB_MIX_0[s2 >>> 24] ^ SUB_MIX_1[(s3 >>> 16) & 0xff] ^ SUB_MIX_2[(s0 >>> 8) & 0xff] ^ SUB_MIX_3[s1 & 0xff] ^ keySchedule[ksRow++];
                t3 = SUB_MIX_0[s3 >>> 24] ^ SUB_MIX_1[(s0 >>> 16) & 0xff] ^ SUB_MIX_2[(s1 >>> 8) & 0xff] ^ SUB_MIX_3[s2 & 0xff] ^ keySchedule[ksRow++];
                // Update state
                s0 = t0;
                s1 = t1;
                s2 = t2;
                s3 = t3;
            }
            // Shift rows, sub bytes, add round key
            t0 = ((SBOX[s0 >>> 24] << 24) | (SBOX[(s1 >>> 16) & 0xff] << 16) | (SBOX[(s2 >>> 8) & 0xff] << 8) | SBOX[s3 & 0xff]) ^ keySchedule[ksRow++];
            t1 = ((SBOX[s1 >>> 24] << 24) | (SBOX[(s2 >>> 16) & 0xff] << 16) | (SBOX[(s3 >>> 8) & 0xff] << 8) | SBOX[s0 & 0xff]) ^ keySchedule[ksRow++];
            t2 = ((SBOX[s2 >>> 24] << 24) | (SBOX[(s3 >>> 16) & 0xff] << 16) | (SBOX[(s0 >>> 8) & 0xff] << 8) | SBOX[s1 & 0xff]) ^ keySchedule[ksRow++];
            t3 = ((SBOX[s3 >>> 24] << 24) | (SBOX[(s0 >>> 16) & 0xff] << 16) | (SBOX[(s1 >>> 8) & 0xff] << 8) | SBOX[s2 & 0xff]) ^ keySchedule[ksRow++];

            // Set output
            state[0] = t0;
            state[1] = t1;
            state[2] = t2;
            state[3] = t3;
        }

        public function dispose() : void {
            keyWords.length = 0;
            keyWords = null;
        }
    }
}
