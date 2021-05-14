/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.example.hikerview.ui.js.javascript;

/**
 * Collection of utilities
 */

public class Kit
{

    public static int xDigitToInt(int c, int accumulator)
    {
        check: {
            // Use 0..9 < A..Z < a..z
            if (c <= '9') {
                c -= '0';
                if (0 <= c) { break check; }
            } else if (c <= 'F') {
                if ('A' <= c) {
                    c -= ('A' - 10);
                    break check;
                }
            } else if (c <= 'f') {
                if ('a' <= c) {
                    c -= ('a' - 10);
                    break check;
                }
            }
            return -1;
        }
        return (accumulator << 4) | c;
    }

    public static RuntimeException codeBug()
        throws RuntimeException
    {
        RuntimeException ex = new IllegalStateException("FAILED ASSERTION");
        // Print stack trace ASAP
        ex.printStackTrace(System.err);
        throw ex;
    }

    public static RuntimeException codeBug(String msg)
        throws RuntimeException
    {
        msg = "FAILED ASSERTION: " + msg;
        RuntimeException ex = new IllegalStateException(msg);
        // Print stack trace ASAP
        ex.printStackTrace(System.err);
        throw ex;
    }
}
