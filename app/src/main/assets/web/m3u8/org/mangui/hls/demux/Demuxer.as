/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {
    import flash.utils.ByteArray;

    public interface Demuxer {
        function append(data : ByteArray) : void;

        function notifycomplete() : void;

        function cancel() : void;

        function  get audioExpected() : Boolean;

        function  get videoExpected() : Boolean;
    }
}
