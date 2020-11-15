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
#include "JniLogSystem.h"

// precached in JNI_OnLoad and released in JNI_OnUnload
JniGlobalReference<jclass>* logLevelClass;

void JniLogSystem_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved)
{
  logLevelClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("LogSystem$LogLevel")));
}

void JniLogSystem_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved)
{
  if (logLevelClass)
  {
    delete logLevelClass;
    logLevelClass = NULL;
  }
}

JniLogSystemCallback::JniLogSystemCallback(JNIEnv* env, jobject callbackObject)
  : JniCallbackBase(env, callbackObject), AdblockPlus::LogSystem()
{
}

void JniLogSystemCallback::operator()(AdblockPlus::LogSystem::LogLevel logLevel,
    const std::string& message, const std::string& source)
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env,
          env->GetObjectClass(GetCallbackObject())),
      "logCallback",
      "(" TYP("LogSystem$LogLevel") "Ljava/lang/String;Ljava/lang/String;)V");

  // TODO: Set log level from Java and handle it here (to reduce C++->Java calls)

  if (method)
  {
    const char* enumName = 0;

    switch (logLevel)
    {
    default:
    case AdblockPlus::LogSystem::LOG_LEVEL_TRACE:
      enumName = "TRACE";
      break;
    case AdblockPlus::LogSystem::LOG_LEVEL_LOG:
      enumName = "LOG";
      break;
    case AdblockPlus::LogSystem::LOG_LEVEL_INFO:
      enumName = "INFO";
      break;
    case AdblockPlus::LogSystem::LOG_LEVEL_WARN:
      enumName = "WARN";
      break;
    case AdblockPlus::LogSystem::LOG_LEVEL_ERROR:
      enumName = "ERROR";
      break;
    }

    jclass enumClass = logLevelClass->Get();
    if (enumClass)
    {
      jfieldID enumField = env->GetStaticFieldID(enumClass, enumName,
          TYP("LogSystem$LogLevel"));
      JniLocalReference<jobject> jLogLevel(*env,
          env->GetStaticObjectField(enumClass, enumField));

      JniLocalReference<jstring> jMessage(*env,
          env->NewStringUTF(message.c_str()));
      JniLocalReference<jstring> jSource(*env,
          env->NewStringUTF(source.c_str()));

      env->CallVoidMethod(GetCallbackObject(), method, *jLogLevel, *jMessage,
          *jSource);
    }

    CheckAndLogJavaException(*env);
  }
}
