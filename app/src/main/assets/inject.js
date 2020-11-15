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

console.log("injected JS started")
var hideElements = function()
{
  // no need to invoke if already invoked on another event
  if (document.{{HIDDEN_FLAG}} === true)
  {
    {{DEBUG}} console.log('already hidden, exiting');
    return;
  }

  {{DEBUG}} console.log("Not yet hidden!")

  // hide by injecting CSS stylesheet
  {{HIDE}}

  document.{{HIDDEN_FLAG}} = true; // set flag not to do it again
};

if (document.readyState === "complete")
{
  {{DEBUG}} console.log('document is in "complete" state, apply hiding')
  hideElements();
}
else
{
  {{DEBUG}} console.log('installing listener')

  // onreadystatechange event
  document.onreadystatechange = function()
  {
    {{DEBUG}} console.log('onreadystatechange() event fired (' + document.readyState + ')')
    if (document.readyState == 'interactive')
    {
      hideElements();
    }
  }

   // load event
  window.addEventListener('load', function(event)
  {
    {{DEBUG}} console.log('load() event fired');
    hideElements();
  });

  // DOMContentLoaded event
  document.addEventListener('DOMContentLoaded', function()
  {
    {{DEBUG}} console.log('DOMContentLoaded() event fired');
    hideElements();
  }, false);
}
console.log("injected JS finished");

