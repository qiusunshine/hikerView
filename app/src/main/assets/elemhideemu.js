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

"use strict";


/*
 * This template is used for JavaScript code generation in adblock_bridge.cc
 *
 * Concatenated files from adblockpluscore:
 *   lib/common.js
 *   lib/coreUtils.js
 *   lib/content/elemHideEmulation.js
 *
 * The files were refined: commented out `require` statements and `export`-ing.
 *
 * The application of the element hiding emulation is at the end of this file.
 */


// common.js ------------------------------


/**
 * Converts raw text into a regular expression string
 * @param {string} text the string to convert
 * @return {string} regular expression representation of the text
 */
function textToRegExp(text)
{
  return text.replace(/[-/\\^$*+?.()|[\]{}]/g, "\\$&");
}

// exports.textToRegExp = textToRegExp;

/**
 * Converts filter text into regular expression string
 * @param {string} text as in Filter()
 * @param {boolean} [captureAll=false] whether to enable the capturing of
 *   leading and trailing wildcards in the filter text; by default, leading and
 *   trailing wildcards are stripped out
 * @return {string} regular expression representation of filter text
 */
function filterToRegExp(text, captureAll = false)
{
  // remove multiple wildcards
  text = text.replace(/\*+/g, "*");

  if (!captureAll)
  {
    // remove leading wildcard
    if (text[0] == "*")
      text = text.substring(1);

    // remove trailing wildcard
    if (text[text.length - 1] == "*")
      text = text.substring(0, text.length - 1);
  }

  return text
    // remove anchors following separator placeholder
    .replace(/\^\|$/, "^")
    // escape special symbols
    .replace(/\W/g, "\\$&")
    // replace wildcards by .*
    .replace(/\\\*/g, ".*")
    // process separator placeholders (all ANSI characters but alphanumeric
    // characters and _%.-)
    .replace(/\\\^/g, "(?:[\\x00-\\x24\\x26-\\x2C\\x2F\\x3A-\\x40\\x5B-\\x5E\\x60\\x7B-\\x7F]|$)")
    // process extended anchor at expression start
    .replace(/^\\\|\\\|/, "^[\\w\\-]+:\\/+(?!\\/)(?:[^\\/]+\\.)?")
    // process anchor at expression start
    .replace(/^\\\|/, "^")
    // process anchor at expression end
    .replace(/\\\|$/, "$");
}

// exports.filterToRegExp = filterToRegExp;

function splitSelector(selector)
{
  if (!selector.includes(","))
    return [selector];

  let selectors = [];
  let start = 0;
  let level = 0;
  let sep = "";

  for (let i = 0; i < selector.length; i++)
  {
    let chr = selector[i];

    if (chr == "\\")        // ignore escaped characters
      i++;
    else if (chr == sep)    // don't split within quoted text
      sep = "";             // e.g. [attr=","]
    else if (sep == "")
    {
      if (chr == '"' || chr == "'")
        sep = chr;
      else if (chr == "(")  // don't split between parentheses
        level++;            // e.g. :matches(div,span)
      else if (chr == ")")
        level = Math.max(0, level - 1);
      else if (chr == "," && level == 0)
      {
        selectors.push(selector.substring(start, i));
        start = i + 1;
      }
    }
  }

  selectors.push(selector.substring(start));
  return selectors;
}

// exports.splitSelector = splitSelector;

function findTargetSelectorIndex(selector)
{
  let index = 0;
  let whitespace = 0;
  let scope = [];

  // Start from the end of the string and go character by character, where each
  // character is a Unicode code point.
  for (let character of [...selector].reverse())
  {
    let currentScope = scope[scope.length - 1];

    if (character == "'" || character == "\"")
    {
      // If we're already within the same type of quote, close the scope;
      // otherwise open a new scope.
      if (currentScope == character)
        scope.pop();
      else
        scope.push(character);
    }
    else if (character == "]" || character == ")")
    {
      // For closing brackets and parentheses, open a new scope only if we're
      // not within a quote. Within quotes these characters should have no
      // meaning.
      if (currentScope != "'" && currentScope != "\"")
        scope.push(character);
    }
    else if (character == "[")
    {
      // If we're already within a bracket, close the scope.
      if (currentScope == "]")
        scope.pop();
    }
    else if (character == "(")
    {
      // If we're already within a parenthesis, close the scope.
      if (currentScope == ")")
        scope.pop();
    }
    else if (!currentScope)
    {
      // At the top level (not within any scope), count the whitespace if we've
      // encountered it. Otherwise if we've hit one of the combinators,
      // terminate here; otherwise if we've hit a non-colon character,
      // terminate here.
      if (/\s/.test(character))
      {
        whitespace++;
      }
      else if ((character == ">" || character == "+" || character == "~") ||
               (whitespace > 0 && character != ":"))
      {
        break;
      }
    }

    // Zero out the whitespace count if we've entered a scope.
    if (scope.length > 0)
      whitespace = 0;

    // Increment the index by the size of the character. Note that for Unicode
    // composite characters (like emoji) this will be more than one.
    index += character.length;
  }

  return selector.length - index + whitespace;
}

/**
 * Qualifies a CSS selector with a qualifier, which may be another CSS selector
 * or an empty string. For example, given the selector "div.bar" and the
 * qualifier "#foo", this function returns "div#foo.bar".
 * @param {string} selector The selector to qualify.
 * @param {string} qualifier The qualifier with which to qualify the selector.
 * @returns {string} The qualified selector.
 */
function qualifySelector(selector, qualifier)
{
  let qualifiedSelector = "";

  let qualifierTargetSelectorIndex = findTargetSelectorIndex(qualifier);
  let [, qualifierType = ""] =
    /^([a-z][a-z-]*)?/i.exec(qualifier.substring(qualifierTargetSelectorIndex));

  for (let sub of splitSelector(selector))
  {
    sub = sub.trim();

    qualifiedSelector += ", ";

    let index = findTargetSelectorIndex(sub);

    // Note that the first group in the regular expression is optional. If it
    // doesn't match (e.g. "#foo::nth-child(1)"), type will be an empty string.
    let [, type = "", rest] =
      /^([a-z][a-z-]*)?\*?(.*)/i.exec(sub.substring(index));

    if (type == qualifierType)
      type = "";

    // If the qualifier ends in a combinator (e.g. "body #foo>"), we put the
    // type and the rest of the selector after the qualifier
    // (e.g. "body #foo>div.bar"); otherwise (e.g. "body #foo") we merge the
    // type into the qualifier (e.g. "body div#foo.bar").
    if (/[\s>+~]$/.test(qualifier))
      qualifiedSelector += sub.substring(0, index) + qualifier + type + rest;
    else
      qualifiedSelector += sub.substring(0, index) + type + qualifier + rest;
  }

  // Remove the initial comma and space.
  return qualifiedSelector.substring(2);
}

// exports.qualifySelector = qualifySelector;


// end of common.js ------------------------------


// coreUtils.js  ------------------------------

function desc(properties)
{
  let descriptor = {};
  let keys = Object.keys(properties);

  for (let key of keys)
    descriptor[key] = Object.getOwnPropertyDescriptor(properties, key);

  return descriptor;
}
// exports.desc = desc;

function extend(cls, properties)
{
  return Object.create(cls.prototype, desc(properties));
}
// exports.extend = extend;

function findIndex(iterable, callback, thisArg)
{
  let index = 0;
  for (let item of iterable)
  {
    if (callback.call(thisArg, item))
      return index;

    index++;
  }

  return -1;
}
// exports.findIndex = findIndex;

function indexOf(iterable, searchElement)
{
  return findIndex(iterable, item => item === searchElement);
}
// exports.indexOf = indexOf;


// end of coreUtils.js ------------------------------





// elemHideEmulation.js  ------------------------------


// const {textToRegExp, filterToRegExp, splitSelector,
//        qualifySelector} = require("../common");
// const {indexOf} = require("../coreUtils");

let MIN_INVOCATION_INTERVAL = 3000;
const MAX_SYNCHRONOUS_PROCESSING_TIME = 50;
const abpSelectorRegexp = /:-abp-([\w-]+)\(/i;

let testInfo = null;

function setTestMode()
{
  testInfo = {
    lastProcessedElements: new Set()
  };
}

// exports.setTestMode = setTestMode;

function getTestInfo()
{
  return testInfo;
}

// exports.getTestInfo = getTestInfo;

function getCachedPropertyValue(object, name, defaultValueFunc = () => {})
{
  let value = object[name];
  if (typeof value == "undefined")
    Object.defineProperty(object, name, {value: value = defaultValueFunc()});
  return value;
}

/** Return position of node from parent.
 * @param {Node} node the node to find the position of.
 * @return {number} One-based index like for :nth-child(), or 0 on error.
 */
function positionInParent(node)
{
  return indexOf(node.parentNode.children, node) + 1;
}

function makeSelector(node, selector = "")
{
  if (node == null)
    return null;
  if (!node.parentElement)
  {
    let newSelector = ":root";
    if (selector)
      newSelector += " > " + selector;
    return newSelector;
  }
  let idx = positionInParent(node);
  if (idx > 0)
  {
    let newSelector = `${node.tagName}:nth-child(${idx})`;
    if (selector)
      newSelector += " > " + selector;
    return makeSelector(node.parentElement, newSelector);
  }

  return selector;
}

function parseSelectorContent(content, startIndex)
{
  let parens = 1;
  let quote = null;
  let i = startIndex;
  for (; i < content.length; i++)
  {
    let c = content[i];
    if (c == "\\")
    {
      // Ignore escaped characters
      i++;
    }
    else if (quote)
    {
      if (c == quote)
        quote = null;
    }
    else if (c == "'" || c == '"')
      quote = c;
    else if (c == "(")
      parens++;
    else if (c == ")")
    {
      parens--;
      if (parens == 0)
        break;
    }
  }

  if (parens > 0)
    return null;
  return {text: content.substring(startIndex, i), end: i};
}

/** Stringified style objects
 * @typedef {Object} StringifiedStyle
 * @property {string} style CSS style represented by a string.
 * @property {string[]} subSelectors selectors the CSS properties apply to.
 */

/**
 * Produce a string representation of the stylesheet entry.
 * @param {CSSStyleRule} rule the CSS style rule.
 * @return {StringifiedStyle} the stringified style.
 */
function stringifyStyle(rule)
{
  let styles = [];
  for (let i = 0; i < rule.style.length; i++)
  {
    let property = rule.style.item(i);
    let value = rule.style.getPropertyValue(property);
    let priority = rule.style.getPropertyPriority(property);
    styles.push(`${property}: ${value}${priority ? " !" + priority : ""};`);
  }
  styles.sort();
  return {
    style: styles.join(" "),
    subSelectors: splitSelector(rule.selectorText)
  };
}

let scopeSupported = null;

function tryQuerySelector(subtree, selector, all)
{
  let elements = null;
  try
  {
    elements = all ? subtree.querySelectorAll(selector) :
      subtree.querySelector(selector);
    scopeSupported = true;
  }
  catch (e)
  {
    // Edge doesn't support ":scope"
    scopeSupported = false;
  }
  return elements;
}

/**
 * Query selector. If it is relative, will try :scope.
 * @param {Node} subtree the element to query selector
 * @param {string} selector the selector to query
 * @param {bool} [all=false] true to perform querySelectorAll()
 * @returns {?(Node|NodeList)} result of the query. null in case of error.
 */
function scopedQuerySelector(subtree, selector, all)
{
  if (selector[0] == ">")
  {
    selector = ":scope" + selector;
    if (scopeSupported)
    {
      return all ? subtree.querySelectorAll(selector) :
        subtree.querySelector(selector);
    }
    if (scopeSupported == null)
      return tryQuerySelector(subtree, selector, all);
    return null;
  }
  return all ? subtree.querySelectorAll(selector) :
    subtree.querySelector(selector);
}

function scopedQuerySelectorAll(subtree, selector)
{
  return scopedQuerySelector(subtree, selector, true);
}

const regexpRegexp = /^\/(.*)\/([imu]*)$/;

/**
 * Make a regular expression from a text argument. If it can be parsed as a
 * regular expression, parse it and the flags.
 * @param {string} text the text argument.
 * @return {?RegExp} a RegExp object or null in case of error.
 */
function makeRegExpParameter(text)
{
  let [, pattern, flags] =
      regexpRegexp.exec(text) || [null, textToRegExp(text)];

  try
  {
    return new RegExp(pattern, flags);
  }
  catch (e)
  {
  }
  return null;
}

function* evaluate(chain, index, prefix, subtree, styles, targets)
{
  if (index >= chain.length)
  {
    yield prefix;
    return;
  }
  for (let [selector, element] of
       chain[index].getSelectors(prefix, subtree, styles, targets))
  {
    if (selector == null)
      yield null;
    else
      yield* evaluate(chain, index + 1, selector, element, styles, targets);
  }
  // Just in case the getSelectors() generator above had to run some heavy
  // document.querySelectorAll() call which didn't produce any results, make
  // sure there is at least one point where execution can pause.
  yield null;
}

class PlainSelector
{
  constructor(selector)
  {
    this._selector = selector;
    this.maybeDependsOnAttributes = /[#.]|\[.+\]/.test(selector);
    this.dependsOnDOM = this.maybeDependsOnAttributes;
    this.maybeContainsSiblingCombinators = /[~+]/.test(selector);
  }

  /**
   * Generator function returning a pair of selector
   * string and subtree.
   * @param {string} prefix the prefix for the selector.
   * @param {Node} subtree the subtree we work on.
   * @param {StringifiedStyle[]} styles the stringified style objects.
   * @param {Node[]} [targets] the nodes we are interested in.
   */
  *getSelectors(prefix, subtree, styles, targets)
  {
    yield [prefix + this._selector, subtree];
  }
}

const incompletePrefixRegexp = /[\s>+~]$/;

class HasSelector
{
  constructor(selectors)
  {
    this.dependsOnDOM = true;

    this._innerSelectors = selectors;
  }

  get dependsOnStyles()
  {
    return this._innerSelectors.some(selector => selector.dependsOnStyles);
  }

  get dependsOnCharacterData()
  {
    return this._innerSelectors.some(
      selector => selector.dependsOnCharacterData
    );
  }

  get maybeDependsOnAttributes()
  {
    return this._innerSelectors.some(
      selector => selector.maybeDependsOnAttributes
    );
  }

  *getSelectors(prefix, subtree, styles, targets)
  {
    for (let element of this.getElements(prefix, subtree, styles, targets))
      yield [makeSelector(element), element];
  }

  /**
   * Generator function returning selected elements.
   * @param {string} prefix the prefix for the selector.
   * @param {Node} subtree the subtree we work on.
   * @param {StringifiedStyle[]} styles the stringified style objects.
   * @param {Node[]} [targets] the nodes we are interested in.
   */
  *getElements(prefix, subtree, styles, targets)
  {
    let actualPrefix = (!prefix || incompletePrefixRegexp.test(prefix)) ?
        prefix + "*" : prefix;
    let elements = scopedQuerySelectorAll(subtree, actualPrefix);
    if (elements)
    {
      for (let element of elements)
      {
        // If the element is neither an ancestor nor a descendant of one of the
        // targets, we can skip it.
        if (targets && !targets.some(target => element.contains(target) ||
                                               target.contains(element)))
        {
          yield null;
          continue;
        }

        let iter = evaluate(this._innerSelectors, 0, "", element, styles,
                            targets);
        for (let selector of iter)
        {
          if (selector == null)
            yield null;
          else if (scopedQuerySelector(element, selector))
            yield element;
        }
        yield null;

        if (testInfo)
          testInfo.lastProcessedElements.add(element);
      }
    }
  }
}

class ContainsSelector
{
  constructor(textContent)
  {
    this.dependsOnDOM = true;
    this.dependsOnCharacterData = true;

    this._regexp = makeRegExpParameter(textContent);
  }

  *getSelectors(prefix, subtree, styles, targets)
  {
    for (let element of this.getElements(prefix, subtree, styles, targets))
      yield [makeSelector(element), subtree];
  }

  *getElements(prefix, subtree, styles, targets)
  {
    let actualPrefix = (!prefix || incompletePrefixRegexp.test(prefix)) ?
        prefix + "*" : prefix;

    let elements = scopedQuerySelectorAll(subtree, actualPrefix);

    if (elements)
    {
      let lastRoot = null;
      for (let element of elements)
      {
        // For a filter like div:-abp-contains(Hello) and a subtree like
        // <div id="a"><div id="b"><div id="c">Hello</div></div></div>
        // we're only interested in div#a
        if (lastRoot && lastRoot.contains(element))
        {
          yield null;
          continue;
        }

        lastRoot = element;

        if (targets && !targets.some(target => element.contains(target) ||
                                               target.contains(element)))
        {
          yield null;
          continue;
        }

        if (this._regexp && this._regexp.test(element.textContent))
          yield element;
        else
          yield null;

        if (testInfo)
          testInfo.lastProcessedElements.add(element);
      }
    }
  }
}

class PropsSelector
{
  constructor(propertyExpression)
  {
    this.dependsOnStyles = true;
    this.dependsOnDOM = true;

    let regexpString;
    if (propertyExpression.length >= 2 && propertyExpression[0] == "/" &&
        propertyExpression[propertyExpression.length - 1] == "/")
    {
      regexpString = propertyExpression.slice(1, -1);
    }
    else
      regexpString = filterToRegExp(propertyExpression);

    this._regexp = new RegExp(regexpString, "i");
  }

  *findPropsSelectors(styles, prefix, regexp)
  {
    for (let style of styles)
      if (regexp.test(style.style))
        for (let subSelector of style.subSelectors)
        {
          if (subSelector.startsWith("*") &&
              !incompletePrefixRegexp.test(prefix))
          {
            subSelector = subSelector.substring(1);
          }
          let idx = subSelector.lastIndexOf("::");
          if (idx != -1)
            subSelector = subSelector.substring(0, idx);
          yield qualifySelector(subSelector, prefix);
        }
  }

  *getSelectors(prefix, subtree, styles, targets)
  {
    for (let selector of this.findPropsSelectors(styles, prefix, this._regexp))
      yield [selector, subtree];
  }
}

class Pattern
{
  constructor(selectors, text)
  {
    this.selectors = selectors;
    this.text = text;
  }

  get dependsOnStyles()
  {
    return getCachedPropertyValue(
      this, "_dependsOnStyles",
      () => this.selectors.some(selector => selector.dependsOnStyles)
    );
  }

  get dependsOnDOM()
  {
    return getCachedPropertyValue(
      this, "_dependsOnDOM",
      () => this.selectors.some(selector => selector.dependsOnDOM)
    );
  }

  get dependsOnStylesAndDOM()
  {
    return getCachedPropertyValue(
      this, "_dependsOnStylesAndDOM",
      () => this.selectors.some(selector => selector.dependsOnStyles &&
                                            selector.dependsOnDOM)
    );
  }

  get maybeDependsOnAttributes()
  {
    // Observe changes to attributes if either there's a plain selector that
    // looks like an ID selector, class selector, or attribute selector in one
    // of the patterns (e.g. "a[href='https://example.com/']")
    // or there's a properties selector nested inside a has selector
    // (e.g. "div:-abp-has(:-abp-properties(color: blue))")
    return getCachedPropertyValue(
      this, "_maybeDependsOnAttributes",
      () => this.selectors.some(
              selector => selector.maybeDependsOnAttributes ||
                          (selector instanceof HasSelector &&
                           selector.dependsOnStyles)
            )
    );
  }

  get dependsOnCharacterData()
  {
    // Observe changes to character data only if there's a contains selector in
    // one of the patterns.
    return getCachedPropertyValue(
      this, "_dependsOnCharacterData",
      () => this.selectors.some(selector => selector.dependsOnCharacterData)
    );
  }

  get maybeContainsSiblingCombinators()
  {
    return getCachedPropertyValue(
      this, "_maybeContainsSiblingCombinators",
      () => this.selectors.some(selector =>
                                selector.maybeContainsSiblingCombinators)
    );
  }

  matchesMutationTypes(mutationTypes)
  {
    let mutationTypeMatchMap = getCachedPropertyValue(
      this, "_mutationTypeMatchMap",
      () => new Map([
        // All types of DOM-dependent patterns are affected by mutations of
        // type "childList".
        ["childList", true],
        ["attributes", this.maybeDependsOnAttributes],
        ["characterData", this.dependsOnCharacterData]
      ])
    );

    for (let mutationType of mutationTypes)
    {
      if (mutationTypeMatchMap.get(mutationType))
        return true;
    }

    return false;
  }
}

function extractMutationTypes(mutations)
{
  let types = new Set();

  for (let mutation of mutations)
  {
    types.add(mutation.type);

    // There are only 3 types of mutations: "attributes", "characterData", and
    // "childList".
    if (types.size == 3)
      break;
  }

  return types;
}

function extractMutationTargets(mutations)
{
  if (!mutations)
    return null;

  let targets = new Set();

  for (let mutation of mutations)
  {
    if (mutation.type == "childList")
    {
      // When new nodes are added, we're interested in the added nodes rather
      // than the parent.
      for (let node of mutation.addedNodes)
        targets.add(node);
    }
    else
    {
      targets.add(mutation.target);
    }
  }

  return [...targets];
}

function filterPatterns(patterns, {stylesheets, mutations})
{
  if (!stylesheets && !mutations)
    return patterns.slice();

  let mutationTypes = mutations ? extractMutationTypes(mutations) : null;

  return patterns.filter(
    pattern => (stylesheets && pattern.dependsOnStyles) ||
               (mutations && pattern.dependsOnDOM &&
                pattern.matchesMutationTypes(mutationTypes))
  );
}

function shouldObserveAttributes(patterns)
{
  return patterns.some(pattern => pattern.maybeDependsOnAttributes);
}

function shouldObserveCharacterData(patterns)
{
  return patterns.some(pattern => pattern.dependsOnCharacterData);
}

class ElemHideEmulation
{
  constructor(hideElemsFunc)
  {
    this._filteringInProgress = false;
    this._lastInvocation = -MIN_INVOCATION_INTERVAL;
    this._scheduledProcessing = null;

    this.document = document;
    this.hideElemsFunc = hideElemsFunc;
    this.observer = new MutationObserver(this.observe.bind(this));
  }

  isSameOrigin(stylesheet)
  {
    try
    {
      return new URL(stylesheet.href).origin == this.document.location.origin;
    }
    catch (e)
    {
      // Invalid URL, assume that it is first-party.
      return true;
    }
  }

  /** Parse the selector
   * @param {string} selector the selector to parse
   * @return {Array} selectors is an array of objects,
   * or null in case of errors.
   */
  parseSelector(selector)
  {
    if (selector.length == 0)
      return [];

    let match = abpSelectorRegexp.exec(selector);
    if (!match)
      return [new PlainSelector(selector)];

    let selectors = [];
    if (match.index > 0)
      selectors.push(new PlainSelector(selector.substring(0, match.index)));

    let startIndex = match.index + match[0].length;
    let content = parseSelectorContent(selector, startIndex);
    if (!content)
    {
      console.error(new SyntaxError("Failed to parse Adblock Plus " +
                                    `selector ${selector} ` +
                                    "due to unmatched parentheses."));
      return null;
    }
    if (match[1] == "properties")
      selectors.push(new PropsSelector(content.text));
    else if (match[1] == "has")
    {
      let hasSelectors = this.parseSelector(content.text);
      if (hasSelectors == null)
        return null;
      selectors.push(new HasSelector(hasSelectors));
    }
    else if (match[1] == "contains")
      selectors.push(new ContainsSelector(content.text));
    else
    {
      // this is an error, can't parse selector.
      console.error(new SyntaxError("Failed to parse Adblock Plus " +
                                    `selector ${selector}, invalid ` +
                                    `pseudo-class :-abp-${match[1]}().`));
      return null;
    }

    let suffix = this.parseSelector(selector.substring(content.end + 1));
    if (suffix == null)
      return null;

    selectors.push(...suffix);

    if (selectors.length == 1 && selectors[0] instanceof ContainsSelector)
    {
      console.error(new SyntaxError("Failed to parse Adblock Plus " +
                                    `selector ${selector}, can't ` +
                                    "have a lonely :-abp-contains()."));
      return null;
    }
    return selectors;
  }

  /**
   * Processes the current document and applies all rules to it.
   * @param {CSSStyleSheet[]} [stylesheets]
   *    The list of new stylesheets that have been added to the document and
   *    made reprocessing necessary. This parameter shouldn't be passed in for
   *    the initial processing, all of document's stylesheets will be considered
   *    then and all rules, including the ones not dependent on styles.
   * @param {MutationRecord[]} [mutations]
   *    The list of DOM mutations that have been applied to the document and
   *    made reprocessing necessary. This parameter shouldn't be passed in for
   *    the initial processing, the entire document will be considered
   *    then and all rules, including the ones not dependent on the DOM.
   * @param {function} [done]
   *    Callback to call when done.
   */
  _addSelectors(stylesheets, mutations, done)
  {
    if (testInfo)
      testInfo.lastProcessedElements.clear();

    let patterns = filterPatterns(this.patterns, {stylesheets, mutations});

    let elements = [];
    let elementFilters = [];

    let cssStyles = [];

    // If neither any style sheets nor any DOM mutations have been specified,
    // do full processing.
    if (!stylesheets && !mutations)
      stylesheets = this.document.styleSheets;

    // If there are any DOM mutations and any of the patterns depends on both
    // style sheets and the DOM (e.g. -abp-has(-abp-properties)), find all the
    // rules in every style sheet in the document, because we need to run
    // querySelectorAll afterwards. On the other hand, if we only have patterns
    // that depend on either styles or DOM both not both (e.g. -abp-contains),
    // we can skip this part.
    if (mutations && patterns.some(pattern => pattern.dependsOnStylesAndDOM))
      stylesheets = this.document.styleSheets;

    for (let stylesheet of stylesheets || [])
    {
      // Explicitly ignore third-party stylesheets to ensure consistent behavior
      // between Firefox and Chrome.
      if (!this.isSameOrigin(stylesheet))
        continue;

      let rules;
      try
      {
        rules = stylesheet.cssRules;
      }
      catch (e)
      {
        // On Firefox, there is a chance that an InvalidAccessError
        // get thrown when accessing cssRules. Just skip the stylesheet
        // in that case.
        // See https://searchfox.org/mozilla-central/rev/f65d7528e34ef1a7665b4a1a7b7cdb1388fcd3aa/layout/style/StyleSheet.cpp#699
        continue;
      }

      if (!rules)
        continue;

      for (let rule of rules)
      {
        if (rule.type != rule.STYLE_RULE)
          continue;

        cssStyles.push(stringifyStyle(rule));
      }
    }

    let targets = extractMutationTargets(mutations);

    let pattern = null;
    let generator = null;

    let processPatterns = () =>
    {
      let cycleStart = performance.now();

      if (!pattern)
      {
        if (!patterns.length)
        {
          if (elements.length > 0)
            this.hideElemsFunc(elements, elementFilters);
          if (typeof done == "function")
            done();
          return;
        }

        pattern = patterns.shift();

        let evaluationTargets = targets;

        // If the pattern appears to contain any sibling combinators, we can't
        // easily optimize based on the mutation targets. Since this is a
        // special case, skip the optimization. By setting it to null here we
        // make sure we process the entire DOM.
        if (pattern.maybeContainsSiblingCombinators)
          evaluationTargets = null;

        generator = evaluate(pattern.selectors, 0, "",
                             this.document, cssStyles, evaluationTargets);
      }
      for (let selector of generator)
      {
        if (selector != null)
        {
          for (let element of this.document.querySelectorAll(selector))
          {
            elements.push(element);
            elementFilters.push(pattern.text);
          }
        }
        if (performance.now() - cycleStart > MAX_SYNCHRONOUS_PROCESSING_TIME)
        {
          setTimeout(processPatterns, 0);
          return;
        }
      }
      pattern = null;
      return processPatterns();
    };

    processPatterns();
  }

  // This property is only used in the tests
  // to shorten the invocation interval
  get MIN_INVOCATION_INTERVAL()
  {
    return MIN_INVOCATION_INTERVAL;
  }

  set MIN_INVOCATION_INTERVAL(interval)
  {
    MIN_INVOCATION_INTERVAL = interval;
  }

  /**
   * Re-run filtering either immediately or queued.
   * @param {CSSStyleSheet[]} [stylesheets]
   *    new stylesheets to be processed. This parameter should be omitted
   *    for full reprocessing.
   * @param {MutationRecord[]} [mutations]
   *    new DOM mutations to be processed. This parameter should be omitted
   *    for full reprocessing.
   */
  queueFiltering(stylesheets, mutations)
  {
    let completion = () =>
    {
      this._lastInvocation = performance.now();
      this._filteringInProgress = false;
      if (this._scheduledProcessing)
      {
        let params = Object.assign({}, this._scheduledProcessing);
        this._scheduledProcessing = null;
        this.queueFiltering(params.stylesheets, params.mutations);
      }
    };

    if (this._scheduledProcessing)
    {
      if (!stylesheets && !mutations)
      {
        this._scheduledProcessing = {};
      }
      else if (this._scheduledProcessing.stylesheets ||
               this._scheduledProcessing.mutations)
      {
        if (stylesheets)
        {
          if (!this._scheduledProcessing.stylesheets)
            this._scheduledProcessing.stylesheets = [];
          this._scheduledProcessing.stylesheets.push(...stylesheets);
        }
        if (mutations)
        {
          if (!this._scheduledProcessing.mutations)
            this._scheduledProcessing.mutations = [];
          this._scheduledProcessing.mutations.push(...mutations);
        }
      }
    }
    else if (this._filteringInProgress)
    {
      this._scheduledProcessing = {stylesheets, mutations};
    }
    else if (performance.now() - this._lastInvocation < MIN_INVOCATION_INTERVAL)
    {
      this._scheduledProcessing = {stylesheets, mutations};
      setTimeout(() =>
      {
        let params = Object.assign({}, this._scheduledProcessing);
        this._filteringInProgress = true;
        this._scheduledProcessing = null;
        this._addSelectors(params.stylesheets, params.mutations, completion);
      },
      MIN_INVOCATION_INTERVAL - (performance.now() - this._lastInvocation));
    }
    else if (this.document.readyState == "loading")
    {
      this._scheduledProcessing = {stylesheets, mutations};
      let handler = () =>
      {
        this.document.removeEventListener("DOMContentLoaded", handler);
        let params = Object.assign({}, this._scheduledProcessing);
        this._filteringInProgress = true;
        this._scheduledProcessing = null;
        this._addSelectors(params.stylesheets, params.mutations, completion);
      };
      this.document.addEventListener("DOMContentLoaded", handler);
    }
    else
    {
      this._filteringInProgress = true;
      this._addSelectors(stylesheets, mutations, completion);
    }
  }

  onLoad(event)
  {
    let stylesheet = event.target.sheet;
    if (stylesheet)
      this.queueFiltering([stylesheet]);
  }

  observe(mutations)
  {
    if (testInfo)
    {
      // In test mode, filter out any mutations likely done by us
      // (i.e. style="display: none !important"). This makes it easier to
      // observe how the code responds to DOM mutations.
      mutations = mutations.filter(
        ({type, attributeName, target: {style: newValue}, oldValue}) =>
        !(type == "attributes" && attributeName == "style" &&
          newValue.display == "none" && oldValue.display != "none")
      );

      if (mutations.length == 0)
        return;
    }

    this.queueFiltering(null, mutations);
  }

  apply(patterns)
  {
    this.patterns = [];
    for (let pattern of patterns)
    {
      let selectors = this.parseSelector(pattern.selector);
      if (selectors != null && selectors.length > 0)
        this.patterns.push(new Pattern(selectors, pattern.text));
    }

    if (this.patterns.length > 0)
    {
      this.queueFiltering();
      this.observer.observe(
        this.document,
        {
          childList: true,
          attributes: shouldObserveAttributes(this.patterns),
          characterData: shouldObserveCharacterData(this.patterns),
          subtree: true
        }
      );
      this.document.addEventListener("load", this.onLoad.bind(this), true);
    }
  }
}

// exports.ElemHideEmulation = ElemHideEmulation;


// end of elemHideEmulation.js  ------------------------------



// elemHidingEmulatedPatterns array definition is generated in adblock_bridge.cc

let elemHidingEmulatedPatterns = JSON.parse({{BRIDGE}}.getElemhideEmulationSelectors());

// adopted from applyElemHideEmulation function in:
// https://gitlab.com/eyeo/adblockplus/adblockpluscore/blob/master/test/browser/elemHideEmulation.js

let elemHideEmulation = new ElemHideEmulation(
  elems =>
  {
    for (let elem of elems)
      elem.style.display = "none";
  }
);

elemHideEmulation.apply(elemHidingEmulatedPatterns);
