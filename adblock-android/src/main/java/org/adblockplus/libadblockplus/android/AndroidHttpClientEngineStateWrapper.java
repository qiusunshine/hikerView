package org.adblockplus.libadblockplus.android;

import org.adblockplus.libadblockplus.HttpClient;
import org.adblockplus.libadblockplus.HttpRequest;
import org.adblockplus.libadblockplus.ServerResponse;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class AndroidHttpClientEngineStateWrapper extends HttpClient
{
  private final HttpClient httpClient;
  private final WeakReference<AdblockEngine> engineRef;

  public AndroidHttpClientEngineStateWrapper(final HttpClient httpClient, final AdblockEngine engine)
  {
    this.httpClient = httpClient;
    this.engineRef = new WeakReference<>(engine);
  }

  @Override
  public void request(final HttpRequest request, final Callback callback)
  {
    final AdblockEngine engine = engineRef.get();

    if (engine != null && !engine.isEnabled())
    {
      Timber.d("Connection refused: engine is disabled");

      final ServerResponse response = new ServerResponse();
      response.setResponseStatus(0);
      response.setStatus(ServerResponse.NsStatus.ERROR_CONNECTION_REFUSED);

      callback.onFinished(response);
      return;
    }

    httpClient.request(request, callback);
  }
}
