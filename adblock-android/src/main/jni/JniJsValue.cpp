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
#include "JniJsValue.h"

// precached in JNI_OnLoad and released in JNI_OnUnload
JniGlobalReference<jclass>* jsValueClass;
jmethodID jsValueCtor;

void JniJsValue_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved)
{
  jsValueClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("JsValue")));
  jsValueCtor = env->GetMethodID(jsValueClass->Get(), "<init>", "(J)V");
}

void JniJsValue_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved)
{
  if (jsValueClass)
  {
    delete jsValueClass;
    jsValueClass = NULL;
  }
}

jclass GetJsValueClass()
{
  return (jsValueClass != NULL ? jsValueClass->Get() : NULL);
}

static jboolean JNICALL JniIsUndefined(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsUndefined() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsNull(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsNull() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsNumber(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsNumber() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsString(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsString() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsBoolean(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsBool() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsObject(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsObject() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsArray(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsArray() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsFunction(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->IsFunction() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jstring JNICALL JniAsString(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::JsValue* jsValue = JniGetJsValuePtr(ptr);
    return env->NewStringUTF(jsValue->AsString().c_str());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jlong JNICALL JniAsLong(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return static_cast<jlong>(JniGetJsValuePtr(ptr)->AsInt());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jboolean JNICALL JniAsBoolean(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValuePtr(ptr)->AsBool() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jobject JNICALL JniAsList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniJsValueListToArrayList(env, JniGetJsValuePtr(ptr)->AsList());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniGetProperty(JNIEnv* env, jclass clazz, jlong ptr, jstring name)
{
  try
  {
    return NewJniJsValue(env, JniGetJsValuePtr(ptr)->GetProperty(JniJavaToStdString(env, name)));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniSetProperty(JNIEnv* env, jclass clazz, jlong ptr, jstring name, jlong valuePtr)
{
  try
  {
    AdblockPlus::JsValue jsValue = JniGetJsValue(valuePtr);
    JniGetJsValuePtr(ptr)->SetProperty(JniJavaToStdString(env, name), jsValue);
  }
  CATCH_AND_THROW(env)
}

static jstring JNICALL JniGetClass(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniStdStringToJava(env, JniGetJsValuePtr(ptr)->GetClass());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniGetOwnPropertyNames(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniStringVectorToArrayList(env, JniGetJsValuePtr(ptr)->GetOwnPropertyNames());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static AdblockPlus::JsValueList convertToJsValueList(JNIEnv* env, jlongArray jParamPtrs)
{
  AdblockPlus::JsValueList jsValueList;

  int size = env->GetArrayLength(jParamPtrs);
  jboolean jniCopyMode = JNI_FALSE; // no copy
  jlong* paramPtrs = env->GetLongArrayElements(jParamPtrs, &jniCopyMode);

  for (int i = 0; i < size; i++)
  {
    jsValueList.push_back(JniGetJsValue(paramPtrs[i]));
  }

  env->ReleaseLongArrayElements(jParamPtrs, paramPtrs, jniCopyMode);
  return jsValueList;
}

static jobject JNICALL JniCall(JNIEnv* env, jclass clazz, jlong ptr, jlongArray jParamPtrs)
{
  try
  {
    AdblockPlus::JsValue* jsValue = JniGetJsValuePtr(ptr);
    AdblockPlus::JsValueList jsValueList = convertToJsValueList(env, jParamPtrs);
    return NewJniJsValue(env, jsValue->Call(jsValueList));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniCallThisValue(
    JNIEnv* env, jclass clazz, jlong ptr, jlongArray jParamPtrs, jlong thisValuePtr)
{
  try
  {
    AdblockPlus::JsValue* jsValue = JniGetJsValuePtr(ptr);
    AdblockPlus::JsValue jsThisValue = JniGetJsValue(thisValuePtr);
    AdblockPlus::JsValueList jsValueList = convertToJsValueList(env, jParamPtrs);
    return NewJniJsValue(env, jsValue->Call(jsValueList, jsThisValue));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::JsValue>(ptr);
}

jobject NewJniJsValue(JNIEnv* env, AdblockPlus::JsValue&& jsValue, jclass jsValueClassArg)
{
  auto pJsValue = reinterpret_cast<uintptr_t>(new AdblockPlus::JsValue(std::move(jsValue)));
  return env->NewObject(jsValueClass->Get(), jsValueCtor, static_cast<jlong>(pJsValue));
}

AdblockPlus::JsValue* JniGetJsValuePtr(jlong ptr)
{
  return JniLongToTypePtr<AdblockPlus::JsValue>(ptr);
}

AdblockPlus::JsValue& JniGetJsValue(jlong ptr)
{
  return *JniLongToTypePtr<AdblockPlus::JsValue>(ptr);
}

jobject JniJsValueListToArrayList(JNIEnv* env, AdblockPlus::JsValueList&& list)
{
  jobject arrayList = NewJniArrayList(env);

  for (AdblockPlus::JsValueList::iterator it = list.begin(), end = list.end(); it != end; ++it)
  {
    JniAddObjectToList(env, arrayList, *JniLocalReference<jobject>(env, NewJniJsValue(env, std::move(*it))));
  }

  return arrayList;
}

// TODO: List of functions that lack JNI bindings
//void SetProperty(const std::string& name, const std::string& val);
//void SetProperty(const std::string& name, int64_t val);
//void SetProperty(const std::string& name, bool val);
//void SetProperty(const std::string& name, const char* val);
//inline void SetProperty(const std::string& name, int val);

static JNINativeMethod methods[] =
{
  { (char*)"isUndefined", (char*)"(J)Z", (void*)JniIsUndefined },
  { (char*)"isNull", (char*)"(J)Z", (void*)JniIsNull },
  { (char*)"isNumber", (char*)"(J)Z", (void*)JniIsNumber },
  { (char*)"isString", (char*)"(J)Z", (void*)JniIsString },
  { (char*)"isBoolean", (char*)"(J)Z", (void*)JniIsBoolean },
  { (char*)"isObject", (char*)"(J)Z", (void*)JniIsObject },
  { (char*)"isArray", (char*)"(J)Z", (void*)JniIsArray },
  { (char*)"isFunction", (char*)"(J)Z", (void*)JniIsFunction },
  { (char*)"asString", (char*)"(J)Ljava/lang/String;", (void*)JniAsString },
  { (char*)"asLong", (char*)"(J)J", (void*)JniAsLong },
  { (char*)"asBoolean", (char*)"(J)Z", (void*)JniAsBoolean },
  { (char*)"asList", (char*)"(J)Ljava/util/List;", (void*)JniAsList },
  { (char*)"getProperty", (char*)"(JLjava/lang/String;)" TYP("JsValue"), (void*)JniGetProperty },
  { (char*)"setProperty", (char*)"(JLjava/lang/String;J)V", (void*)JniSetProperty },
  { (char*)"getJsClass", (char*)"(J)Ljava/lang/String;", (void*)JniGetClass },
  { (char*)"getOwnPropertyNames", (char*)"(J)Ljava/util/List;", (void*)JniGetOwnPropertyNames },
  { (char*)"call", (char*)"(J[J)" TYP("JsValue"), (void*)JniCall },
  { (char*)"call", (char*)"(J[JJ)" TYP("JsValue"), (void*)JniCallThisValue },
  { (char*)"dtor", (char*)"(J)V", (void*)JniDtor }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_JsValue_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
