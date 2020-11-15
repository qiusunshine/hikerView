/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

{
   {{DEBUG}} console.log('starting injecting css rules');
   var styleSheet = {{BRIDGE}}.getElemhideStyleSheet();
   {{DEBUG}} console.log('stylesheet length: ' + styleSheet.length);
   if (styleSheet)
   {
      var head = document.getElementsByTagName("head")[0];
      var style = document.createElement("style");
      head.appendChild(style);
      style.textContent = styleSheet;
      {{DEBUG}} console.log('finished injecting css rules');
   }
   else
   {
      {{DEBUG}} console.log('stylesheet is empty, skipping injection');
   }
}