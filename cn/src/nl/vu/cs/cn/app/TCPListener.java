package nl.vu.cs.cn.app;

import android.graphics.Bitmap;

/**
 * Created by jeffrey on 7/1/13.
 */
public interface TCPListener {

    public void onConnected(boolean isServer);

    public void onMessage(boolean isServer, String msg);

    public void onSendFailed(boolean isServer);

    public void onClosed(boolean isServer);

    public void onConnectionFailed(boolean isServer);

    public void onImage(boolean isServer, Bitmap bitmap);
}
