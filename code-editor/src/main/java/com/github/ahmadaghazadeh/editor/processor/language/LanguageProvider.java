/*
 * Copyright (C) 2018 Light Team Software
 *
 * This file is part of ModPE IDE.
 *
 * ModPE IDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ModPE IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ahmadaghazadeh.editor.processor.language;


public class LanguageProvider {

    public static Language getLanguage(String lang) {
        if (lang.toLowerCase().equals("js") || lang.toLowerCase().equals("javascript")) {
            return new JSLanguage();
        } else if (lang.toLowerCase().equals("html")) {
            return new HtmlLanguage();
        }else if (lang.toLowerCase().equals("css")) {
            return new CSSLanguage();
        } else {
            return new JSLanguage();
        }
    }
}
