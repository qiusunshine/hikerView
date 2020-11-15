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
#include "JniFilter.h"

// precached in JNI_OnLoad and released in JNI_OnUnload
JniGlobalReference<jclass>* filterEnumClass;

void JniFilter_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved)
{
  filterEnumClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("Filter$Type")));
}

void JniFilter_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved)
{
  if (filterEnumClass)
  {
    delete filterEnumClass;
    filterEnumClass = NULL;
  }
}

static AdblockPlus::Filter* GetFilterPtr(jlong ptr)
{
  return JniLongToTypePtr<AdblockPlus::Filter>(ptr);
}

static jobject JNICALL JniGetType(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::Filter::Type type;
  try
  {
    type = GetFilterPtr(ptr)->GetType();
  }
  CATCH_THROW_AND_RETURN(env, 0)

  const char* enumName = 0;

  switch (type)
  {
  case AdblockPlus::Filter::TYPE_BLOCKING:
    enumName = "BLOCKING";
    break;
  case AdblockPlus::Filter::TYPE_COMMENT:
    enumName = "COMMENT";
    break;
  case AdblockPlus::Filter::TYPE_ELEMHIDE:
    enumName = "ELEMHIDE";
    break;
  case AdblockPlus::Filter::TYPE_ELEMHIDE_EXCEPTION:
    enumName = "ELEMHIDE_EXCEPTION";
    break;
  case AdblockPlus::Filter::TYPE_ELEMHIDE_EMULATION:
    enumName = "ELEMHIDE_EMULATION";
    break;
  case AdblockPlus::Filter::TYPE_EXCEPTION:
    enumName = "EXCEPTION";
    break;
  default:
    enumName = "INVALID";
    break;
  }

  jfieldID enumField = env->GetStaticFieldID(filterEnumClass->Get(), enumName, TYP("Filter$Type"));
  return env->GetStaticObjectField(filterEnumClass->Get(), enumField);
}

static jboolean JNICALL JniIsListed(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return GetFilterPtr(ptr)->IsListed() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static void JNICALL JniAddToList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetFilterPtr(ptr)->AddToList();
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniRemoveFromList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetFilterPtr(ptr)->RemoveFromList();
  }
  CATCH_AND_THROW(env)
}

static jboolean JNICALL JniOperatorEquals(JNIEnv* env, jclass clazz, jlong ptr, jlong otherPtr)
{
  AdblockPlus::Filter* me = GetFilterPtr(ptr);
  AdblockPlus::Filter* other = GetFilterPtr(otherPtr);

  try
  {
    return *me == *other ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static JNINativeMethod methods[] =
{
  { (char*)"getType", (char*)"(J)" TYP("Filter$Type"), (void*)JniGetType },
  { (char*)"isListed", (char*)"(J)Z", (void*)JniIsListed },
  { (char*)"addToList", (char*)"(J)V", (void*)JniAddToList },
  { (char*)"removeFromList", (char*)"(J)V", (void*)JniRemoveFromList },
  { (char*)"operatorEquals", (char*)"(JJ)Z", (void*)JniOperatorEquals }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_Filter_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
