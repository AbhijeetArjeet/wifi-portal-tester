package com.wifiportaltester.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(DnsOverridePlugin.class);
        registerPlugin(PortalNotifierPlugin.class);
        registerPlugin(StatusWidgetPlugin.class);
        registerPlugin(NetworkMonitorPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
