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
#include "AdblockPlus/IFileSystem.h"
#include "Utils.h"
#include <istream>
#include <streambuf>
#include "JniFileSystem.h"

#ifdef _WIN32
#include <Shlwapi.h>
#endif

#define METHOD_EXCEPTION_MESSAGE(method) ("Exception thrown in FileSystem." method "(): ")

namespace {
// precached in JNI_OnLoad and released in JNI_OnUnload
JniGlobalReference<jclass>* throwableClass;
jmethodID throwableGetMessageMethod;

JniGlobalReference<jclass>* statResultClass;
jmethodID statResultIsExistsMethod;
jmethodID statResultGetModifiedMethod;

JniGlobalReference<jclass>* readCallbackClass;
jmethodID readCallbackClassCtor;

JniGlobalReference<jclass>* fileSystemCallbackClass;
jmethodID callbackClassCtor;

JniGlobalReference<jclass>* statCallbackClass;
jmethodID statCallbackClassCtor;
}

void JniFileSystem_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved)
{
  throwableClass = new JniGlobalReference<jclass>(env, env->FindClass("java/lang/Throwable"));
  throwableGetMessageMethod = env->GetMethodID(throwableClass->Get(), "getMessage", "()Ljava/lang/String;");

  statResultClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("FileSystem$StatResult")));
  statResultIsExistsMethod = env->GetMethodID(statResultClass->Get(), "isExists", "()Z");
  statResultGetModifiedMethod = env->GetMethodID(statResultClass->Get(), "getModified", "()J");

  readCallbackClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("FileSystem$ReadCallback")));
  readCallbackClassCtor = env->GetMethodID(readCallbackClass->Get(), "<init>", "(J)V");

  fileSystemCallbackClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("FileSystem$Callback")));
  callbackClassCtor = env->GetMethodID(fileSystemCallbackClass->Get(), "<init>", "(J)V");

  statCallbackClass = new JniGlobalReference<jclass>(env, env->FindClass(PKG("FileSystem$StatCallback")));
  statCallbackClassCtor = env->GetMethodID(statCallbackClass->Get(), "<init>", "(J)V");
}

void JniFileSystem_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved)
{
  if (throwableClass)
  {
    delete throwableClass;
    throwableClass = NULL;
  }

  if (statResultClass)
  {
    delete statResultClass;
    statResultClass = NULL;
  }

  if (readCallbackClass)
  {
    delete readCallbackClass;
    readCallbackClass = NULL;
  }

  if (fileSystemCallbackClass)
  {
    delete fileSystemCallbackClass;
    fileSystemCallbackClass = NULL;
  }

  if (statCallbackClass)
  {
    delete statCallbackClass;
    statCallbackClass = NULL;
  }
}

JniFileSystemCallback::JniFileSystemCallback(JNIEnv* env, jobject callbackObject, jstring jBasePath)
    : JniCallbackBase(env, callbackObject)
{
  basePath = JniJavaToStdString(env, jBasePath);
  if (basePath.size() > 1 && *basePath.rbegin() == PATH_SEPARATOR)
  {
    basePath.resize(basePath.size() - 1);
  }
}

// copy-paste from C++'s DefaultFileSystem.cpp
std::string JniFileSystemCallback::Resolve(const std::string& path) const
{
  if (basePath.empty())
  {
    return path;
  }
  else
  {
#ifdef _WIN32
    if (PathIsRelative(NormalizePath(path).c_str()))
#else
    if (path.length() && *path.begin() != PATH_SEPARATOR)
#endif
    {
      if (*basePath.rbegin() != PATH_SEPARATOR)
        return basePath + PATH_SEPARATOR + path;
      else
        return basePath + path;
    }
    else
    {
      return path;
    }
  }
}

std::string JniFileSystemCallback::PeekException(JNIEnv *env) const
{
  JniLocalReference<jthrowable> throwable(env, env->ExceptionOccurred());
  env->ExceptionClear();
  jstring jError = (jstring)(env->CallObjectMethod(throwable.Get(), throwableGetMessageMethod));
  return JniJavaToStdString(env, jError);
}

void JniFileSystemCallback::Read(const std::string& fileName,
                                 const AdblockPlus::IFileSystem::ReadCallback& doneCallback,
                                 const AdblockPlus::IFileSystem::Callback& errorCallback) const
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env, env->GetObjectClass(GetCallbackObject())),
      "read",
      "(Ljava/lang/String;" TYP("FileSystem$ReadCallback") TYP("FileSystem$Callback") ")V");

  if (method)
  {
    jstring jFilename = JniStdStringToJava(*env, Resolve(fileName));
    jobject jReadCallback = env->NewObject(
        readCallbackClass->Get(),
        readCallbackClassCtor,
        JniPtrToLong(new AdblockPlus::IFileSystem::ReadCallback(doneCallback)));
    jobject jErrorCallback = env->NewObject(
        fileSystemCallbackClass->Get(),
        callbackClassCtor,
        JniPtrToLong(new AdblockPlus::IFileSystem::Callback(errorCallback)));

    jvalue args[3];
    args[0].l = jFilename;
    args[1].l = jReadCallback;
    args[2].l = jErrorCallback;
    env->CallVoidMethodA(GetCallbackObject(), method, args);

    if (env->ExceptionCheck())
    {
      errorCallback(METHOD_EXCEPTION_MESSAGE("read") + PeekException(*env));
    }
  }
}

class JniWriteCallback {
private:
    AdblockPlus::IFileSystem::IOBuffer _data;
    const AdblockPlus::IFileSystem::Callback* _callback;
    jobject _byteBuffer;

public:
    JniWriteCallback(JNIEnv *env,
                     const AdblockPlus::IFileSystem::IOBuffer& data,
                     const AdblockPlus::IFileSystem::Callback& callback);
    virtual ~JniWriteCallback();

    const AdblockPlus::IFileSystem::IOBuffer GetData() const
    {
      return _data;
    }
    const AdblockPlus::IFileSystem::Callback* GetCallback() const
    {
      return _callback;
    }
    jobject GetByteBuffer() const
    {
      return _byteBuffer;
    }
};

JniWriteCallback::JniWriteCallback(JNIEnv *env,
                                   const AdblockPlus::IFileSystem::IOBuffer& data,
                                   const AdblockPlus::IFileSystem::Callback& callback)
{
  _data = data;
  _callback = new AdblockPlus::IFileSystem::Callback(callback);
  _byteBuffer = env->NewDirectByteBuffer((void*)&_data[0], _data.size());
}

JniWriteCallback::~JniWriteCallback()
{
  delete _callback;
}

void JniFileSystemCallback::Write(const std::string& fileName,
                                  const AdblockPlus::IFileSystem::IOBuffer& data,
                                  const AdblockPlus::IFileSystem::Callback& callback)
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env, env->GetObjectClass(GetCallbackObject())),
      "write",
      "(Ljava/lang/String;Ljava/nio/ByteBuffer;" TYP("FileSystem$Callback") ")V");

  if (method)
  {
    jstring jFilename = JniStdStringToJava(*env, Resolve(fileName));
    JniWriteCallback* writeCallback = new JniWriteCallback(*env, data, callback);
    jobject jCallback = env->NewObject(
        fileSystemCallbackClass->Get(),
        callbackClassCtor,
        JniPtrToLong(writeCallback));

    jvalue args[3];
    args[0].l = jFilename;
    args[1].l = writeCallback->GetByteBuffer();
    args[2].l = jCallback;
    env->CallVoidMethodA(GetCallbackObject(), method, args);

    if (env->ExceptionCheck())
    {
      callback(METHOD_EXCEPTION_MESSAGE("write") + PeekException(*env));
    }
  }
}

void JniFileSystemCallback::Move(const std::string& fromFileName,
                                 const std::string& toFileName,
                                 const AdblockPlus::IFileSystem::Callback& callback)
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env, env->GetObjectClass(GetCallbackObject())),
      "move",
      "(Ljava/lang/String;Ljava/lang/String;" TYP("FileSystem$Callback") ")V");

  if (method)
  {
    jstring jFromFilename = JniStdStringToJava(*env, Resolve(fromFileName));
    jstring jToFilename = JniStdStringToJava(*env, Resolve(toFileName));
    jobject jCallback = env->NewObject(
        fileSystemCallbackClass->Get(),
        callbackClassCtor,
        JniPtrToLong(new AdblockPlus::IFileSystem::Callback(callback)));

    jvalue args[3];
    args[0].l = jFromFilename;
    args[1].l = jToFilename;
    args[2].l = jCallback;
    env->CallVoidMethodA(GetCallbackObject(), method, args);

    if (env->ExceptionCheck())
    {
      callback(METHOD_EXCEPTION_MESSAGE("move") + PeekException(*env));
    }
  }
}

void JniFileSystemCallback::Remove(const std::string& fileName,
                                   const AdblockPlus::IFileSystem::Callback& callback)
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env, env->GetObjectClass(GetCallbackObject())),
      "remove",
      "(Ljava/lang/String;" TYP("FileSystem$Callback") ")V");

  if (method)
  {
    jstring jfilename = JniStdStringToJava(*env, Resolve(fileName));
    jobject jCallback = env->NewObject(
        fileSystemCallbackClass->Get(),
        callbackClassCtor,
        JniPtrToLong(new AdblockPlus::IFileSystem::Callback(callback)));

    jvalue args[2];
    args[0].l = jfilename;
    args[1].l = jCallback;
    env->CallVoidMethodA(GetCallbackObject(), method, args);

    if (env->ExceptionCheck())
    {
      callback(METHOD_EXCEPTION_MESSAGE("remove") + PeekException(*env));
    }
  }
}

void JniFileSystemCallback::Stat(const std::string& fileName,
                                 const AdblockPlus::IFileSystem::StatCallback& callback) const
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env, env->GetObjectClass(GetCallbackObject())),
      "stat",
      "(Ljava/lang/String;" TYP("FileSystem$StatCallback") ")V");

  if (method)
  {
    jstring jFilename = JniStdStringToJava(*env, Resolve(fileName));
    jobject jStatCallback = env->NewObject(
        statCallbackClass->Get(),
        statCallbackClassCtor,
        JniPtrToLong(new AdblockPlus::IFileSystem::StatCallback(callback)));

    jvalue args[2];
    args[0].l = jFilename;
    args[1].l = jStatCallback;
    env->CallVoidMethodA(GetCallbackObject(), method, args);

    if (env->ExceptionCheck())
    {
      AdblockPlus::IFileSystem::StatResult statResult;
      callback(statResult, METHOD_EXCEPTION_MESSAGE("stat") + PeekException(*env));
    }
  }
}

static void JNICALL JniCallbackOnFinished(JNIEnv* env, jclass clazz, jlong ptr, jstring jError)
{
  std::string error = JniJavaToStdString(env, jError);
  (*JniLongToTypePtr<JniWriteCallback>(ptr)->GetCallback())(error);
}

static void JNICALL JniCallbackDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<JniWriteCallback>(ptr);
}

static void JNICALL JniReadCallbackOnFinished(JNIEnv* env, jclass clazz, jlong ptr, jobject jByteBuffer)
{
  jlong size = env->GetDirectBufferCapacity(jByteBuffer);
  uint8_t* buffer = static_cast<uint8_t*>(env->GetDirectBufferAddress(jByteBuffer));
  AdblockPlus::IFileSystem::IOBuffer ioBuffer(buffer, buffer + size);
  (*JniLongToTypePtr<AdblockPlus::IFileSystem::ReadCallback>(ptr))(std::move(ioBuffer));
}

static void JNICALL JniReadCallbackDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::IFileSystem::ReadCallback>(ptr);
}

static void JNICALL JniStatCallbackOnFinished(JNIEnv* env, jclass clazz, jlong ptr,
                                              jobject jStatResult, jstring jError)
{
  AdblockPlus::IFileSystem::StatResult statResult;
  if (jStatResult)
  {
    statResult.exists = (env->CallBooleanMethod(jStatResult, statResultIsExistsMethod) == JNI_TRUE);
    statResult.lastModified = env->CallLongMethod(jStatResult, statResultGetModifiedMethod);
  }
  std::string error = JniJavaToStdString(env, jError);
  (*JniLongToTypePtr<AdblockPlus::IFileSystem::StatCallback>(ptr))(statResult, error);
}

static void JNICALL JniStatCallbackDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::IFileSystem::StatCallback>(ptr);
}

static JNINativeMethod methods[] =
{
  { (char*)"callbackOnFinished", (char*)"(JLjava/lang/String;)V", (void*)JniCallbackOnFinished },
  { (char*)"callbackDtor", (char*)"(J)V", (void*)JniCallbackDtor },
  { (char*)"readCallbackOnFinished", (char*)"(JLjava/nio/ByteBuffer;)V", (void*)JniReadCallbackOnFinished },
  { (char*)"readCallbackDtor", (char*)"(J)V", (void*)JniReadCallbackDtor },
  { (char*)"statCallbackOnFinished", (char*)"(J" TYP("FileSystem$StatResult") "Ljava/lang/String;)V", (void*)JniStatCallbackOnFinished },
  { (char*)"statCallbackDtor", (char*)"(J)V", (void*)JniStatCallbackDtor }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_FileSystem_registerNatives(
    JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
