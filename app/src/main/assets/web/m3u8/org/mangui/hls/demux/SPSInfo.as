/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {

    import flash.utils.ByteArray;

/* inspired from https://github.com/aizvorski/h264bitstream/blob/master/h264_stream.c#L241-L342 */

    public class SPSInfo {
        public var width : int;
        public var height : int;

        public function SPSInfo(sps : ByteArray) {
            var profile_idc : int;
            sps.position++;
            profile_idc = sps.readUnsignedByte();
            var eg : ExpGolomb = new ExpGolomb(sps);
            // constraint_set[0-5]_flag, u(1), reserved_zero_2bits u(2), level_idc u(8)
            eg.readBits(16);
            // skip seq_parameter_set_id
            eg.readUE();
            if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 144) {
                var chroma_format_idc : int = eg.readUE();
                if (3 === chroma_format_idc) {
                    // separate_colour_plane_flag
                    eg.readBits(1);
                }
                // bit_depth_luma_minus8
                eg.readUE();
                // bit_depth_chroma_minus8
                eg.readUE();
                // qpprime_y_zero_transform_bypass_flag
                eg.readBits(1);
                // seq_scaling_matrix_present_flag
                var seq_scaling_matrix_present_flag : Boolean = eg.readBoolean();
                if (seq_scaling_matrix_present_flag) {
                    var imax : int = (chroma_format_idc != 3) ? 8 : 12;
                    for (var i : int = 0; i < imax; ++i) {
                        // seq_scaling_list_present_flag[ i ]
                        if (eg.readBoolean()) {
                            if (i < 6) {
                                scaling_list(16, eg);
                            } else {
                                scaling_list(64, eg);
                            }
                        }
                    }
                }
            }
            // log2_max_frame_num_minus4
            eg.readUE();
            var pic_order_cnt_type : int = eg.readUE();
            if ( 0 === pic_order_cnt_type ) {
                // log2_max_pic_order_cnt_lsb_minus4
                eg.readUE();
            } else if ( 1 === pic_order_cnt_type ) {
                // delta_pic_order_always_zero_flag
                eg.readBits(1);
                // offset_for_non_ref_pic
                eg.readUE();
                // offset_for_top_to_bottom_field
                eg.readUE();
                var num_ref_frames_in_pic_order_cnt_cycle : int = eg.readUE();
                for (i = 0; i < num_ref_frames_in_pic_order_cnt_cycle; ++i) {
                    // offset_for_ref_frame[ i ]
                    eg.readUE();
                }
            }
            // max_num_ref_frames
            eg.readUE();
            // gaps_in_frame_num_value_allowed_flag
            eg.readBits(1);
            var pic_width_in_mbs_minus1 : int = eg.readUE();
            var pic_height_in_map_units_minus1 : int = eg.readUE();
            var frame_mbs_only_flag : int = eg.readBits(1);
            if (0 === frame_mbs_only_flag) {
                // mb_adaptive_frame_field_flag
                eg.readBits(1);
            }
            // direct_8x8_inference_flag
            eg.readBits(1);
            var frame_cropping_flag : int = eg.readBits(1);
            if (frame_cropping_flag) {
                var frame_crop_left_offset : int = eg.readUE();
                var frame_crop_right_offset : int = eg.readUE();
                var frame_crop_top_offset : int = eg.readUE();
                var frame_crop_bottom_offset : int = eg.readUE();
            }
            width = ((pic_width_in_mbs_minus1 + 1) * 16) - frame_crop_left_offset * 2 - frame_crop_right_offset * 2;
            height = ((2 - frame_mbs_only_flag) * (pic_height_in_map_units_minus1 + 1) * 16) - (frame_crop_top_offset * 2) - (frame_crop_bottom_offset * 2);
        }

        private static function scaling_list(sizeOfScalingList : int, eg : ExpGolomb) : void {
            var lastScale : int = 8;
            var nextScale : int = 8;
            var delta_scale : int;
            for (var j : int = 0; j < sizeOfScalingList; ++j) {
                if (nextScale != 0) {
                    delta_scale = eg.readSE();
                    nextScale = (lastScale + delta_scale + 256) % 256;
                }
                lastScale = (nextScale == 0) ? lastScale : nextScale;
            }
        }
    }
}
