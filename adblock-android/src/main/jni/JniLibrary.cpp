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

#include "JniJsValue.h"
#include "JniFilter.h"
#include "JniLogSystem.h"
#include "JniCallbacks.h"
#include "JniNotification.h"
#include "JniWebRequest.h"
#include "JniFileSystem.h"

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), ABP_JNI_VERSION) != JNI_OK)
  {
    return JNI_ERR;
  }

  JniJsValue_OnLoad(vm, env, reserved);
  JniFilter_OnLoad(vm, env, reserved);
  JniLogSystem_OnLoad(vm, env, reserved);
  JniCallbacks_OnLoad(vm, env, reserved);
  JniNotification_OnLoad(vm, env, reserved);
  JniWebRequest_OnLoad(vm, env, reserved);
  JniUtils_OnLoad(vm, env, reserved);
  JniFileSystem_OnLoad(vm, env, reserved);

  return ABP_JNI_VERSION;
}

void JNI_OnUnload(JavaVM* vm, void* reserved)
{
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), ABP_JNI_VERSION) != JNI_OK)
  {
    return;
  }

  JniJsValue_OnUnload(vm, env, reserved);
  JniFilter_OnUnload(vm, env, reserved);
  JniLogSystem_OnUnload(vm, env, reserved);
  JniCallbacks_OnUnload(vm, env, reserved);
  JniNotification_OnUnload(vm, env, reserved);
  JniWebRequest_OnUnload(vm, env, reserved);
  JniUtils_OnUnload(vm, env, reserved);
  JniFileSystem_OnUnload(vm, env, reserved);
}