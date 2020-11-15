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

#ifndef JNICALLBACKS_H
#define JNICALLBACKS_H

#include <AdblockPlus.h>
#include "Utils.h"
#include "JniJsValue.h"

void JniCallbacks_OnLoad(JavaVM* vm, JNIEnv* env, void* reserved);

void JniCallbacks_OnUnload(JavaVM* vm, JNIEnv* env, void* reserved);

class JniCallbackBase
{
public:
  JniCallbackBase(JNIEnv* env, jobject callbackObject);
  virtual ~JniCallbackBase();
  void LogException(JNIEnv* env, jthrowable throwable) const;
  bool CheckAndLogJavaException(JNIEnv* env) const;

  JavaVM* GetJavaVM() const
  {
    return javaVM;
  }

  jobject GetCallbackObject() const
  {
    return callbackObject->Get();
  }

private:
  JavaVM* javaVM;
  const JniGlobalReference<jobject>::Ptr callbackObject;
};

class JniEventCallback : public JniCallbackBase
{
public:
  JniEventCallback(JNIEnv* env, jobject callbackObject);
  void Callback(AdblockPlus::JsValueList&& params);
};

class JniFilterChangeCallback : public JniCallbackBase
{
public:
  JniFilterChangeCallback(JNIEnv* env, jobject callbackObject);
  void Callback(const std::string& arg, AdblockPlus::JsValue&& jsValue);
};

class JniLogSystemCallback : public JniCallbackBase, public AdblockPlus::LogSystem
{
public:
  JniLogSystemCallback(JNIEnv* env, jobject callbackObject);
  void operator()(AdblockPlus::LogSystem::LogLevel logLevel, const std::string& message, const std::string& source);
};

class JniFileSystemCallback : public JniCallbackBase, public AdblockPlus::IFileSystem
{
private:
    std::string basePath;
    // Returns the absolute path to a file.
    std::string Resolve(const std::string& path) const;
    // Get last JNI exception message and clear it
    std::string PeekException(JNIEnv *env) const;
public:
    JniFileSystemCallback(JNIEnv* env, jobject callbackObject, jstring basePath);
    void Read(const std::string& fileName,
              const ReadCallback& doneCallback,
              const Callback& errorCallback) const override;
    void Write(const std::string& fileName,
               const IOBuffer& data,
               const Callback& callback) override;
    void Move(const std::string& fromFileName,
              const std::string& toFileName,
              const Callback& callback) override;
    void Remove(const std::string& fileName,
                const Callback& callback) override;
    void Stat(const std::string& fileName,
              const StatCallback& callback) const override;
};

class JniShowNotificationCallback : public JniCallbackBase
{
public:
  JniShowNotificationCallback(JNIEnv* env, jobject callbackObject);
  void Callback(AdblockPlus::Notification&&);
};

class JniWebRequestCallback : public JniCallbackBase, public AdblockPlus::IWebRequest
{
public:
    JniWebRequestCallback(JNIEnv* env, const AdblockPlus::Scheduler& scheduler, jobject callbackObject);
    void GET(const std::string& url,
             const AdblockPlus::HeaderList& requestHeaders,
             const AdblockPlus::IWebRequest::GetCallback& getCallback) override;
    void SyncGET(const std::string& url,
             const AdblockPlus::HeaderList& requestHeaders,
             const AdblockPlus::IWebRequest::GetCallback& getCallback);

private:
  static JniLocalReference<jobject> NewTuple(JNIEnv* env, const std::string& a, const std::string& b);
private:
  AdblockPlus::Scheduler m_scheduler;
};

class JniIsAllowedConnectionTypeCallback : public JniCallbackBase
{
public:
  JniIsAllowedConnectionTypeCallback(JNIEnv* env, jobject callbackObject);
  bool Callback(const std::string* allowedConnectionType);
};

#endif /* JNICALLBACKS_H */
