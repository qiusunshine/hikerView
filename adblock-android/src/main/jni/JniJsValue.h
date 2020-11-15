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

#ifndef JNIJSVALUE_H
#define JNIJSVALUE_H

#include <jni.h>
#include <AdblockPlus/JsValue.h>

void JniJsValue_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved);

void JniJsValue_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved);

jclass GetJsValueClass();

jobject NewJniJsValue(JNIEnv* env, AdblockPlus::JsValue&& jsValue, jclass jsValueClass = 0);

jobject JniJsValueListToArrayList(JNIEnv* env, AdblockPlus::JsValueList&& list);

AdblockPlus::JsValue* JniGetJsValuePtr(jlong ptr);

AdblockPlus::JsValue& JniGetJsValue(jlong ptr);

#endif /* JNIJSVALUE_H */
