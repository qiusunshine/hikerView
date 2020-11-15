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
#include <thread>
#include "JniPlatform.h"

static jobject SubscriptionsToArrayList(JNIEnv* env, std::vector<AdblockPlus::Subscription>&& subscriptions)
{
  jobject list = NewJniArrayList(env);

  for (std::vector<AdblockPlus::Subscription>::iterator it = subscriptions.begin(), end = subscriptions.end(); it != end; it++)
  {
    JniAddObjectToList(env, list, NewJniSubscription(env, std::move(*it)));
  }

  return list;
}

static AdblockPlus::FilterEngine::ContentType ConvertContentType(JNIEnv *env,
    jobject jContentType)
{
  JniLocalReference<jclass> contentTypeClass(env,
      env->GetObjectClass(jContentType));
  jmethodID nameMethod = env->GetMethodID(*contentTypeClass, "name",
      "()Ljava/lang/String;");
  JniLocalReference<jstring> jValue(env,
      (jstring) env->CallObjectMethod(jContentType, nameMethod));
  const std::string value = JniJavaToStdString(env, *jValue);
  return AdblockPlus::FilterEngine::StringToContentType(value);
}

namespace
{
  AdblockPlus::FilterEngine& GetFilterEngineRef(jlong jniPlatformPtr)
  {
    return JniLongToTypePtr<JniPlatform>(jniPlatformPtr)->platform->GetFilterEngine();
  }
}

static jboolean JNICALL JniIsFirstRun(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

    return engine.IsFirstRun() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE);
}

static jobject JNICALL JniGetFilter(JNIEnv* env, jclass clazz, jlong ptr, jstring jText)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);
  std::string text = JniJavaToStdString(env, jText);

  try
  {
    return NewJniFilter(env, engine.GetFilter(text));
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static jobject JNICALL JniGetListedFilters(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  try
  {
    std::vector<AdblockPlus::Filter> filters = engine.GetListedFilters();

    jobject list = NewJniArrayList(env);

    for (std::vector<AdblockPlus::Filter>::iterator it = filters.begin(), end = filters.end(); it != end; it++)
    {
      JniAddObjectToList(env, list, *JniLocalReference<jobject>(env, NewJniFilter(env, std::move(*it))));
    }

    return list;
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static jobject JNICALL JniGetSubscription(JNIEnv* env, jclass clazz, jlong ptr, jstring jUrl)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);
  std::string url = JniJavaToStdString(env, jUrl);

  try
  {
    return NewJniSubscription(env, engine.GetSubscription(url));
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static void JNICALL JniShowNextNotification(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  try
  {
    engine.ShowNextNotification();
  }
  CATCH_AND_THROW(env);
}

static void JNICALL JniSetShowNotificationCallback(JNIEnv* env, jclass clazz,
                                                  jlong ptr, jlong callbackPtr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  JniShowNotificationCallback* const callback =
      JniLongToTypePtr<JniShowNotificationCallback>(callbackPtr);

  auto showNotificationCallback = [callback](AdblockPlus::Notification&& notification)
  {
    callback->Callback(std::move(notification));
  };

  try
  {
    engine.SetShowNotificationCallback(showNotificationCallback);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniRemoveShowNotificationCallback(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  try
  {
    engine.RemoveShowNotificationCallback();
  }
  CATCH_AND_THROW(env);
}

static jobject JNICALL JniGetListedSubscriptions(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  try
  {
    return SubscriptionsToArrayList(env, engine.GetListedSubscriptions());
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static jobject JNICALL JniFetchAvailableSubscriptions(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  try
  {
    return SubscriptionsToArrayList(env, engine.FetchAvailableSubscriptions());
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static void JNICALL JniRemoveFilterChangeCallback(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  try
  {
    engine.RemoveFilterChangeCallback();
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetFilterChangeCallback(JNIEnv* env, jclass clazz,
    jlong ptr, jlong filterPtr)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);
  JniFilterChangeCallback* callback = JniLongToTypePtr<JniFilterChangeCallback>(
      filterPtr);

  auto filterCallback = [callback](const std::string& arg, AdblockPlus::JsValue&& jsValue)
  {
    callback->Callback(arg, std::move(jsValue));
  };

  try
  {
    engine.SetFilterChangeCallback(filterCallback);
  }
  CATCH_AND_THROW(env)
}

static jstring JNICALL JniGetElementHidingStyleSheet(JNIEnv* env, jclass clazz,
                                                     jlong ptr, jstring jDomain, jboolean jSpecificOnly)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string domain = JniJavaToStdString(env, jDomain);

  try
  {
    return JniStdStringToJava(env, engine.GetElementHidingStyleSheet(domain, jSpecificOnly == JNI_TRUE));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniGetElementHidingEmulationSelectors(JNIEnv* env, jclass clazz,
    jlong ptr, jstring jDomain)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string domain = JniJavaToStdString(env, jDomain);

  try
  {
    std::vector<AdblockPlus::FilterEngine::EmulationSelector> selectors = engine.GetElementHidingEmulationSelectors(domain);

    jobject list = NewJniArrayList(env);

    for (auto it = selectors.cbegin(), end = selectors.cend(); it != end; ++it)
    {
      JniAddObjectToList(env, list, NewJniEmulationSelector(env, *it));
    }

    return list;
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static std::vector<std::string> JavaStringListToStringVector(JNIEnv* env, jobject jList)
{
  std::vector<std::string> out;
  if (jList)
  {
    jmethodID getFromListMethod = JniGetGetFromListMethod(env, jList);
    jmethodID getListSizeMethod = JniGetListSizeMethod(env, jList);
    jsize len = JniGetListSize(env, jList, getListSizeMethod);
    out.reserve(len);

    for (jsize i = 0; i < len; i++)
    {
      out.push_back(
          JniJavaToStdString(env,
              *JniLocalReference<jstring>(env,
                  static_cast<jstring>(
                      JniGetObjectFromList(env, jList, getFromListMethod, i)))));
    }
  }
  return out;
}

static jobject JNICALL JniMatchesMany(JNIEnv* env, jclass clazz, jlong ptr,
    jstring jUrl, jobjectArray jContentTypes, jobject jReferrerChain, jstring jSiteKey,
    jboolean jSpecificOnly)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string url = JniJavaToStdString(env, jUrl);

  AdblockPlus::FilterEngine::ContentTypeMask contentTypeMask = 0;
  int contentTypesSize = env->GetArrayLength(jContentTypes);
  for (int i = 0; i < contentTypesSize; i++)
  {
    contentTypeMask |= ConvertContentType(env, env->GetObjectArrayElement(jContentTypes, i));
  }

  std::string siteKey = JniJavaToStdString(env, jSiteKey);
    std::vector<std::string> documentUrls = JavaStringListToStringVector(env, jReferrerChain);

  try
  {
    AdblockPlus::FilterPtr filterPtr = engine.Matches(url, contentTypeMask, documentUrls, siteKey,
                                                      jSpecificOnly == JNI_TRUE);

    return (filterPtr.get() ? NewJniFilter(env, std::move(*filterPtr)) : 0);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jboolean JNICALL JniIsDocumentWhitelisted(JNIEnv* env, jclass clazz, jlong ptr,
    jstring jUrl, jobject jReferrerChain, jstring jSiteKey)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string url = JniJavaToStdString(env, jUrl);
  std::vector<std::string> documentUrls = JavaStringListToStringVector(env, jReferrerChain);
  std::string siteKey = JniJavaToStdString(env, jSiteKey);
  try
  {
    return engine.IsDocumentWhitelisted(url, documentUrls, siteKey) ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsGenericblockWhitelisted(JNIEnv* env, jclass clazz, jlong ptr,
        jstring jUrl, jobject jReferrerChain, jstring jSiteKey)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string url = JniJavaToStdString(env, jUrl);
  std::vector<std::string> documentUrls = JavaStringListToStringVector(env, jReferrerChain);
  std::string siteKey = JniJavaToStdString(env, jSiteKey);
  try
  {
    return engine.IsGenericblockWhitelisted(url, documentUrls, siteKey) ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsElemhideWhitelisted(JNIEnv* env, jclass clazz, jlong ptr,
    jstring jUrl, jobject jReferrerChain, jstring jSiteKey)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string url = JniJavaToStdString(env, jUrl);
  std::vector<std::string> documentUrls = JavaStringListToStringVector(env, jReferrerChain);
  std::string siteKey = JniJavaToStdString(env, jSiteKey);
  try
  {
    return engine.IsElemhideWhitelisted(url, documentUrls, siteKey) ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jobject JNICALL JniGetPref(JNIEnv* env, jclass clazz, jlong ptr, jstring jPref)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string pref = JniJavaToStdString(env, jPref);

  try
  {
    return NewJniJsValue(env, engine.GetPref(pref));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniSetPref(JNIEnv* env, jclass clazz, jlong ptr, jstring jPref, jlong jsValue)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string pref = JniJavaToStdString(env, jPref);
  const AdblockPlus::JsValue& value = JniGetJsValue(jsValue);

  try
  {
    engine.SetPref(pref, value);
  }
  CATCH_AND_THROW(env)
}

static jstring JNICALL JniGetHostFromURL(JNIEnv* env, jclass clazz, jlong ptr, jstring jurl)
{
  if (jurl == NULL)
  {
    return NULL;
  }

  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string url = JniJavaToStdString(env, jurl);
  try
  {
    std::string host = engine.GetHostFromURL(url);

    return JniStdStringToJava(env, host);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniSetAllowedConnectionType(JNIEnv* env, jclass clazz, jlong ptr, jstring jvalue)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  std::string stdValue;
  const std::string* value = (jvalue != NULL
    ? &(stdValue = JniJavaToStdString(env, jvalue))
    : NULL);

  try
  {
    engine.SetAllowedConnectionType(value);
  }
  CATCH_AND_THROW(env)
}

static jstring JNICALL JniGetAllowedConnectionType(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);
    std::unique_ptr<std::string> value = engine.GetAllowedConnectionType();

    if (value == NULL)
    {
      return NULL;
    }

    return JniStdStringToJava(env, *value.get());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniSetAcceptableAdsEnabled(JNIEnv* env, jclass clazz, jlong ptr, jboolean jvalue)
{
  AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);

  try
  {
    engine.SetAAEnabled(jvalue == JNI_TRUE);
  }
  CATCH_AND_THROW(env)
}

static jboolean JNICALL JniIsAcceptableAdsEnabled(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);
    return engine.IsAAEnabled() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jstring JNICALL JniGetAcceptableAdsSubscriptionURL(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);
    std::string url = engine.GetAAUrl();
    return JniStdStringToJava(env, url);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniUpdateFiltersAsync(JNIEnv* env, jclass clazz, jlong jniPlatformPtr, jstring jSubscriptionUrl)
{
  std::string subscriptionUrl = JniJavaToStdString(env, jSubscriptionUrl);
  auto jniPlatform = JniLongToTypePtr<JniPlatform>(jniPlatformPtr);
  jniPlatform->scheduler([jniPlatform, subscriptionUrl]
  {
    auto& filterEngine = jniPlatform->platform->GetFilterEngine();
    for (auto& subscription : filterEngine.GetListedSubscriptions())
    {
      if (stringBeginsWith(subscriptionUrl, subscription.GetProperty("url").AsString()))
      {
        subscription.UpdateFilters();
        return;
      }
    }
  });
}

static jlong JNICALL JniGetFilterEngineNativePtr(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::FilterEngine& engine = GetFilterEngineRef(ptr);
    return (jlong)&engine;
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static JNINativeMethod methods[] =
{
  { (char*)"isFirstRun", (char*)"(J)Z", (void*)JniIsFirstRun },
  { (char*)"getFilter", (char*)"(JLjava/lang/String;)" TYP("Filter"), (void*)JniGetFilter },
  { (char*)"getListedFilters", (char*)"(J)Ljava/util/List;", (void*)JniGetListedFilters },
  { (char*)"getSubscription", (char*)"(JLjava/lang/String;)" TYP("Subscription"), (void*)JniGetSubscription },
  { (char*)"showNextNotification", (char*)"(J)V", (void*)JniShowNextNotification },
  { (char*)"setShowNotificationCallback", (char*)"(JJ)V", (void*)JniSetShowNotificationCallback },
  { (char*)"removeShowNotificationCallback", (char*)"(J)V", (void*)JniRemoveShowNotificationCallback },
  { (char*)"getListedSubscriptions", (char*)"(J)Ljava/util/List;", (void*)JniGetListedSubscriptions },
  { (char*)"fetchAvailableSubscriptions", (char*)"(J)Ljava/util/List;", (void*)JniFetchAvailableSubscriptions },
  { (char*)"setFilterChangeCallback", (char*)"(JJ)V", (void*)JniSetFilterChangeCallback },
  { (char*)"removeFilterChangeCallback", (char*)"(J)V", (void*)JniRemoveFilterChangeCallback },
  { (char*)"getElementHidingStyleSheet", (char*)"(JLjava/lang/String;Z)Ljava/lang/String;", (void*)JniGetElementHidingStyleSheet },
  { (char*)"getElementHidingEmulationSelectors", (char*)"(JLjava/lang/String;)Ljava/util/List;", (void*)JniGetElementHidingEmulationSelectors },
  { (char*)"matches", (char*)"(JLjava/lang/String;" "[" TYP("FilterEngine$ContentType") "Ljava/util/List;Ljava/lang/String;Z)" TYP("Filter"), (void*)JniMatchesMany },
  { (char*)"isDocumentWhitelisted", (char*)"(JLjava/lang/String;Ljava/util/List;Ljava/lang/String;)Z", (void*)JniIsDocumentWhitelisted },
  { (char*)"isGenericblockWhitelisted", (char*)"(JLjava/lang/String;Ljava/util/List;Ljava/lang/String;)Z", (void*)JniIsGenericblockWhitelisted },
  { (char*)"isElemhideWhitelisted", (char*)"(JLjava/lang/String;Ljava/util/List;Ljava/lang/String;)Z", (void*)JniIsElemhideWhitelisted },
  { (char*)"getPref", (char*)"(JLjava/lang/String;)" TYP("JsValue"), (void*)JniGetPref },
  { (char*)"setPref", (char*)"(JLjava/lang/String;J)V", (void*)JniSetPref },
  { (char*)"getHostFromURL", (char*)"(JLjava/lang/String;)Ljava/lang/String;", (void*)JniGetHostFromURL },
  { (char*)"setAllowedConnectionType", (char*)"(JLjava/lang/String;)V", (void*)JniSetAllowedConnectionType },
  { (char*)"getAllowedConnectionType", (char*)"(J)Ljava/lang/String;", (void*)JniGetAllowedConnectionType },
  { (char*)"setAcceptableAdsEnabled", (char*)"(JZ)V", (void*)JniSetAcceptableAdsEnabled },
  { (char*)"isAcceptableAdsEnabled", (char*)"(J)Z", (void*)JniIsAcceptableAdsEnabled },
  { (char*)"getAcceptableAdsSubscriptionURL", (char*)"(J)Ljava/lang/String;", (void*)JniGetAcceptableAdsSubscriptionURL },
  { (char*)"updateFiltersAsync", (char*)"(JLjava/lang/String;)V", (void*)JniUpdateFiltersAsync },
  { (char*)"getNativePtr", (char*)"(J)J", (void*)JniGetFilterEngineNativePtr }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_FilterEngine_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
