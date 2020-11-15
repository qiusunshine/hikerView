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
#include "JniWebRequest.h"

namespace
{
// precached in JNI_OnLoad and released in JNI_OnUnload
JniGlobalReference<jclass>* httpRequestClass;
jmethodID httpRequestClassCtor;

JniGlobalReference<jclass>* headerEntryClass;
JniGlobalReference<jclass>* serverResponseClass;
jfieldID responseField;

JniGlobalReference<jclass>* webRequestCallbackClass;
jmethodID callbackClassCtor;
}

void JniWebRequest_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved)
{
  httpRequestClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("HttpRequest")));
  httpRequestClassCtor = env->GetMethodID(httpRequestClass->Get(), "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;ZZ)V");

  headerEntryClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("HeaderEntry")));
  serverResponseClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("ServerResponse")));

  webRequestCallbackClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("HttpClient$JniCallback")));
  callbackClassCtor = env->GetMethodID(webRequestCallbackClass->Get(), "<init>", "(J)V");

  responseField = env->GetFieldID(serverResponseClass->Get(), "response", "Ljava/nio/ByteBuffer;");
}

void JniWebRequest_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved)
{
  if (httpRequestClass)
  {
    delete httpRequestClass;
    httpRequestClass = NULL;
  }

  if (headerEntryClass)
  {
    delete headerEntryClass;
    headerEntryClass = NULL;
  }

  if (serverResponseClass)
  {
    delete serverResponseClass;
    serverResponseClass = NULL;
  }

  if (webRequestCallbackClass)
  {
    delete webRequestCallbackClass;
    webRequestCallbackClass = NULL;
  }
}

JniWebRequestCallback::JniWebRequestCallback(JNIEnv* env, const AdblockPlus::Scheduler& scheduler, jobject callbackObject)
  : JniCallbackBase(env, callbackObject)
  , m_scheduler(scheduler)
{
}

void JniWebRequestCallback::GET(const std::string& url,
         const AdblockPlus::HeaderList& requestHeaders,
         const AdblockPlus::IWebRequest::GetCallback& getCallback)
{
  m_scheduler([this, url, requestHeaders, getCallback]()->void
  {
    SyncGET(url, requestHeaders, getCallback);
  });
}

void JniWebRequestCallback::SyncGET(const std::string& url,
         const AdblockPlus::HeaderList& requestHeaders,
         const AdblockPlus::IWebRequest::GetCallback& getCallback)
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env, env->GetObjectClass(GetCallbackObject())),
      "request",
      "(" TYP("HttpRequest") TYP("HttpClient$Callback") ")V" );

  if (method)
  {
    JniLocalReference<jstring> jUrl{*env, JniStdStringToJava(*env, url)};

    std::string stdRequestMethod = "GET";
    JniLocalReference<jstring> jRequestMethod{*env, JniStdStringToJava(*env, stdRequestMethod)};

    JniLocalReference<jobject> jHeaders(*env, NewJniArrayList(*env));
    jmethodID addMethod = JniGetAddToListMethod(*env, *jHeaders);

    for (AdblockPlus::HeaderList::const_iterator it = requestHeaders.begin(),
        end = requestHeaders.end(); it != end; it++)
    {
      JniLocalReference<jobject> headerEntry = NewTuple(*env, it->first, it->second);
      JniAddObjectToList(*env, *jHeaders, addMethod, *headerEntry);
    }

    JniLocalReference<jobject> jHttpRequest{*env, env->NewObject(
        httpRequestClass->Get(),
        httpRequestClassCtor,
        *jUrl, *jRequestMethod, *jHeaders, true, false)};

    JniLocalReference<jobject> jCallback{*env, env->NewObject(
        webRequestCallbackClass->Get(),
        callbackClassCtor,
        JniPtrToLong(new AdblockPlus::IWebRequest::GetCallback(getCallback)))};

    env->CallVoidMethod(GetCallbackObject(), method, *jHttpRequest, *jCallback);

    if (CheckAndLogJavaException(*env))
    {
      AdblockPlus::ServerResponse response;
      response.status = AdblockPlus::IWebRequest::NS_ERROR_FAILURE;
      getCallback(response);
    }
  }
}

JniLocalReference<jobject> JniWebRequestCallback::NewTuple(JNIEnv* env, const std::string& a,
    const std::string& b)
{
  jmethodID factory = env->GetMethodID(headerEntryClass->Get(), "<init>",
      "(Ljava/lang/String;Ljava/lang/String;)V");

  JniLocalReference<jstring> strA(env, env->NewStringUTF(a.c_str()));
  JniLocalReference<jstring> strB(env, env->NewStringUTF(b.c_str()));

  return JniLocalReference<jobject>{env, env->NewObject(headerEntryClass->Get(), factory, *strA, *strB)};
}

static void JNICALL JniCallbackOnFinished(JNIEnv* env, jclass clazz, jlong ptr, jobject response)
{
  try
  {
    AdblockPlus::ServerResponse sResponse;
    sResponse.status = AdblockPlus::IWebRequest::NS_ERROR_FAILURE;

    if (response)
    {
      sResponse.status = JniGetLongField(env, serverResponseClass->Get(), response, "status");
      sResponse.responseStatus = JniGetIntField(env,
                                                serverResponseClass->Get(),
                                                response,
                                                "responseStatus");
      JniLocalReference<jobject> jByteBuffer{env, env->GetObjectField(response, responseField)};

      if (jByteBuffer)
      {
        const char* responseBuffer = reinterpret_cast<const char*>(env->GetDirectBufferAddress(*jByteBuffer));
        if (responseBuffer == NULL)
        {
          throw std::runtime_error("GetDirectBufferAddress() returned NULL");
        }
        // obtain and call method limit() on ByteBuffer object
        jmethodID byteBufferLimitMethod = env->GetMethodID(env->GetObjectClass(*jByteBuffer), "limit", "()I");
        int responseSize = env->CallIntMethod(*jByteBuffer, byteBufferLimitMethod);
        sResponse.responseText.assign(responseBuffer, responseSize);
      }

      // map headers
      JniLocalReference<jobjectArray> responseHeadersArray{env, JniGetStringArrayField(env,
                                                                 serverResponseClass->Get(),
                                                                 response,
                                                                 "headers")};

      if (responseHeadersArray)
      {
        int itemsCount = env->GetArrayLength(*responseHeadersArray) / 2;
        for (int i = 0; i < itemsCount; i++)
        {
          JniLocalReference<jstring> jKey{env, (jstring)env->GetObjectArrayElement(*responseHeadersArray, i * 2)};
          std::string stdKey = JniJavaToStdString(env, *jKey);

          JniLocalReference<jstring> jValue{env, (jstring)env->GetObjectArrayElement(*responseHeadersArray, i * 2 + 1)};
          std::string stdValue = JniJavaToStdString(env, *jValue);

          sResponse.responseHeaders.push_back(std::make_pair(stdKey, stdValue));
        }
      }
    }

    (*JniLongToTypePtr<AdblockPlus::IWebRequest::GetCallback>(ptr))(sResponse);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniCallbackDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::IWebRequest::GetCallback>(ptr);
}

static JNINativeMethod methods[] =
{
  { (char*)"callbackOnFinished", (char*)"(J" TYP("ServerResponse") ")V", (void*)JniCallbackOnFinished },
  { (char*)"callbackDtor", (char*)"(J)V", (void*)JniCallbackDtor }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_HttpClient_registerNatives(
    JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}

