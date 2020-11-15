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

#include "Utils.h"
#include "JniNotification.h"

// precached in JNI_OnLoad and released in JNI_OnUnload
JniGlobalReference<jclass>* notificationEnumClass;

void JniNotification_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved)
{
  notificationEnumClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("Notification$Type")));
}

void JniNotification_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved)
{
  if (notificationEnumClass)
  {
    delete notificationEnumClass;
    notificationEnumClass = NULL;
  }
}

static AdblockPlus::Notification* GetNotificationPtr(jlong ptr)
{
  return JniLongToTypePtr<AdblockPlus::Notification>(ptr);
}

static jobject JNICALL JniGetType(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::NotificationType type;
  try
  {
    type = GetNotificationPtr(ptr)->GetType();
  }
  CATCH_THROW_AND_RETURN(env, 0)

  const char* enumName = 0;

  switch (type)
  {
  case AdblockPlus::NotificationType::NOTIFICATION_TYPE_CRITICAL:
    enumName = "CRITICAL";
    break;
  case AdblockPlus::NotificationType::NOTIFICATION_TYPE_INFORMATION:
    enumName = "INFORMATION";
    break;
  case AdblockPlus::NotificationType::NOTIFICATION_TYPE_NEWTAB:
    enumName = "NEWTAB";
    break;
  case AdblockPlus::NotificationType::NOTIFICATION_TYPE_RELENTLESS:
    enumName = "RELENTLESS";
    break;
  default:
    enumName = "INVALID";
    break;
  }

  jfieldID enumField = env->GetStaticFieldID(
      notificationEnumClass->Get(),
      enumName,
      TYP("Notification$Type"));

  return env->GetStaticObjectField(notificationEnumClass->Get(), enumField);
}

static jstring JniGetTitle(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return env->NewStringUTF(GetNotificationPtr(ptr)->GetTexts().title.c_str());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jstring JniGetMessageString(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return env->NewStringUTF(GetNotificationPtr(ptr)->GetTexts().message.c_str());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JniGetLinks(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniStringVectorToArrayList(env, GetNotificationPtr(ptr)->GetLinks());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JniMarkAsShown(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetNotificationPtr(ptr)->MarkAsShown();
  }
  CATCH_AND_THROW(env)
}

static JNINativeMethod methods[] =
{
  { (char*) "markAsShown", (char*) "(J)V", (void*) JniMarkAsShown },
  { (char*) "getMessageString", (char*) "(J)Ljava/lang/String;", (void*) JniGetMessageString },
  { (char*) "getTitle", (char*) "(J)Ljava/lang/String;", (void*) JniGetTitle },
  { (char*) "getType", (char*) "(J)" TYP("Notification$Type"), (void*) JniGetType },
  { (char*) "getLinks", (char*) "(J)Ljava/util/List;", (void*) JniGetLinks }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_Notification_registerNatives(
    JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}

