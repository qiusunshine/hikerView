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

#include <AdblockPlus.h>
#include "Utils.h"
#include "JniCallbacks.h"
#include "JniPlatform.h"

static void TransformAppInfo(JNIEnv* env, jobject jAppInfo, AdblockPlus::AppInfo& appInfo)
{
  jclass clazz = env->GetObjectClass(jAppInfo);

  appInfo.application = JniGetStringField(env, clazz, jAppInfo, "application");
  appInfo.applicationVersion = JniGetStringField(env, clazz, jAppInfo, "applicationVersion");
  appInfo.locale = JniGetStringField(env, clazz, jAppInfo, "locale");
  appInfo.name = JniGetStringField(env, clazz, jAppInfo, "name");
  appInfo.version = JniGetStringField(env, clazz, jAppInfo, "version");

  appInfo.developmentBuild = JniGetBooleanField(env, clazz, jAppInfo, "developmentBuild");
}

static AdblockPlus::Platform& GetPlatformRef(jlong ptr)
{
  return *JniLongToTypePtr<JniPlatform>(ptr)->platform;
}

static jlong JNICALL JniCtor(JNIEnv* env, jclass clazz,
                             jobject logSystem,
                             jobject fileSystem,
                             jobject webRequest,
                             jstring jBasePath)
{
  try
  {
    JniPlatform* jniPlatform = new JniPlatform();
    AdblockPlus::DefaultPlatformBuilder platformBuilder;
    jniPlatform->scheduler = platformBuilder.GetDefaultAsyncExecutor();
    if (logSystem)
    {
      platformBuilder.logSystem.reset(new JniLogSystemCallback(env, logSystem));
    }
    if (fileSystem)
    {
      platformBuilder.fileSystem.reset(new JniFileSystemCallback(env, fileSystem, jBasePath));
    }
    else
    {
      if (jBasePath)
      {
        platformBuilder.CreateDefaultFileSystem(JniJavaToStdString(env, jBasePath));
      }
    }
    if (webRequest)
    {
      platformBuilder.webRequest.reset(new JniWebRequestCallback(env, jniPlatform->scheduler,  webRequest));
    }
    jniPlatform->platform = platformBuilder.CreatePlatform();
    return JniPtrToLong(jniPlatform);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<JniPlatform>(ptr);
}

static void JNICALL JniSetUpJsEngine(JNIEnv* env, jclass clazz,
                                     jlong ptr, jobject jAppInfo, jlong v8IsolateProviderPtr)
{
  try
  {
    AdblockPlus::AppInfo appInfo;
    TransformAppInfo(env, jAppInfo, appInfo);
    std::unique_ptr<AdblockPlus::IV8IsolateProvider> isolateProvider;
    if (v8IsolateProviderPtr)
    {
      isolateProvider.reset(JniLongToTypePtr<AdblockPlus::IV8IsolateProvider>(v8IsolateProviderPtr));
    }

    GetPlatformRef(ptr).SetUpJsEngine(appInfo, std::move(isolateProvider));
  }
  CATCH_AND_THROW(env)
}

static long JNICALL JniGetJsEnginePtr(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniPtrToLong(&GetPlatformRef(ptr).GetJsEngine());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniSetUpFilterEngine(JNIEnv* env, jclass clazz, jlong ptr, jobject jIsSubscriptionDownloadAllowedCallback)
{
  try
  {
    AdblockPlus::FilterEngine::CreationParameters creationParameters;
    if (jIsSubscriptionDownloadAllowedCallback)
    {
      auto callback = std::make_shared<JniIsAllowedConnectionTypeCallback>(env, jIsSubscriptionDownloadAllowedCallback);
      auto scheduler = JniLongToTypePtr<JniPlatform>(ptr)->scheduler;
      creationParameters.isSubscriptionDownloadAllowedCallback =
        [scheduler, callback](const std::string* allowedConnectionTypeArg, const std::function<void(bool)>& doneCallback)
        {
          std::shared_ptr<std::string> allowedConnectionType;
          if (allowedConnectionTypeArg)
          {
            allowedConnectionType = std::make_shared<std::string>(*allowedConnectionTypeArg);
          }
          scheduler([callback, allowedConnectionType, doneCallback]
            {
              doneCallback(callback->Callback(allowedConnectionType.get()));
            });
        };
    }
    GetPlatformRef(ptr).CreateFilterEngineAsync(creationParameters);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniEnsureFilterEngine(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetPlatformRef(ptr).GetFilterEngine();
  }
  CATCH_AND_THROW(env)
}

static JNINativeMethod methods[] =
{
  { (char*)"ctor", (char*)"(" TYP("LogSystem") TYP("FileSystem") TYP("HttpClient") "Ljava/lang/String;)J", (void*)JniCtor },
  { (char*)"dtor", (char*)"(J)V", (void*)JniDtor },

  { (char*)"setUpJsEngine", (char*)"(J" TYP("AppInfo") "J)V", (void*)JniSetUpJsEngine },
  { (char*)"getJsEnginePtr", (char*)"(J)J", (void*)JniGetJsEnginePtr },
  { (char*)"setUpFilterEngine", (char*)"(J" TYP("IsAllowedConnectionCallback") ")V", (void*)JniSetUpFilterEngine },
  { (char*)"ensureFilterEngine", (char*)"(J)V", (void*)JniEnsureFilterEngine }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_Platform_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
