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

#include "JniCallbacks.h"

// precached in JNI_OnLoad and released in JNI_OnUnload
JniGlobalReference<jclass>* exceptionHandlerClass;

void JniCallbacks_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved)
{
  exceptionHandlerClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("JniExceptionHandler")));
}

void JniCallbacks_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved)
{
  if (exceptionHandlerClass)
  {
    delete exceptionHandlerClass;
    exceptionHandlerClass = NULL;
  }
}

JniCallbackBase::JniCallbackBase(JNIEnv* env, jobject callbackObject)
  : callbackObject(new JniGlobalReference<jobject>(env, callbackObject))
{
  env->GetJavaVM(&javaVM);
}

JniCallbackBase::~JniCallbackBase()
{

}

void JniCallbackBase::LogException(JNIEnv* env, jthrowable throwable) const
{
  jmethodID logMethod = env->GetStaticMethodID(
    exceptionHandlerClass->Get(), "logException", "(Ljava/lang/Throwable;)V");
  if (logMethod)
  {
    env->CallStaticVoidMethod(exceptionHandlerClass->Get(), logMethod, throwable);
  }
}

bool JniCallbackBase::CheckAndLogJavaException(JNIEnv* env) const
{
  if (env->ExceptionCheck())
  {
    JniLocalReference<jthrowable> throwable(env, env->ExceptionOccurred());
    env->ExceptionClear();
    LogException(env, *throwable);
    return true;
  }
  return false;
}
