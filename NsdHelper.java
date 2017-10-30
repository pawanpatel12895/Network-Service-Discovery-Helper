package com.thelastpawn.p2p;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;

/**
 * Network Service Registry and Discovery
 * <p>
 * Constructor: arguments -> Context, ServiceName
 * </p>
 *
 */
class NsdHelper {

    private static final String TAG = "NsdHelper";
    private String SERVICE_NAME = "NsdService";
    private static final String SERVICE_TYPE = "_http._tcp.";


    private boolean discovering = false;
    private boolean registered = false;

    private final Context mContext;
    private NsdServiceInfo mServiceInfo = null;
    private NsdManager mNsdManager;
    private MyRegistrationListener myRegistrationListener;
    private MyDiscoveryListener myDiscoveryListener;
    private MyResolveListener myResolveListener;
    private String mServiceName;

    NsdHelper(Context context, String serviceName) {
        SERVICE_NAME = serviceName;
        mContext = context;
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);

    }

    void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, myRegistrationListener);

        mServiceInfo = serviceInfo;
    }

    void initListeners() {
        initRegistrationListener();
        initDiscoveryListener();
        initResolveListener();
    }

    private void initResolveListener() {
        myResolveListener = new MyResolveListener();
    }

    private void initRegistrationListener() {
        myRegistrationListener = new MyRegistrationListener();
    }

    private void initDiscoveryListener() {
        myDiscoveryListener = new MyDiscoveryListener();
    }

    NsdServiceInfo getChosenServiceInfo() {
        return mServiceInfo;
    }

    void discoverServices() {
        if (!isDiscovering() && mNsdManager != null) {
            Log.i(TAG, "Starting Discovery");
            mNsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, myDiscoveryListener);
            discovering = true;
        }
    }

    void stopDiscovery() {
        if (isDiscovering()) {
            Log.i(TAG, "Stopping Discovery");
            mNsdManager.stopServiceDiscovery(myDiscoveryListener);
            discovering = false;
        }
    }

    boolean isDiscovering() {
        return discovering;
    }

    public void tearDown() {
        mNsdManager.unregisterService(myRegistrationListener);
    }

    boolean isRegistered() {
        return registered;
    }

    private class MyRegistrationListener implements NsdManager.RegistrationListener {
        @Override
        public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
            Log.e(TAG, "Registration Failed");
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
            Log.e(TAG, "unregistration Failed");
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            Log.i(TAG, "Registration Success");
            mServiceInfo = nsdServiceInfo;
            mServiceName = nsdServiceInfo.getServiceName();
            registered = true;
            Toast.makeText(mContext, "Registered : " + nsdServiceInfo.getServiceName(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
            registered = false;
            Log.i(TAG, "Unregistered");
        }
    }

    private class MyDiscoveryListener implements NsdManager.DiscoveryListener {
        @Override
        public void onStartDiscoveryFailed(String s, int i) {
        }

        @Override
        public void onStopDiscoveryFailed(String s, int i) {

        }

        @Override
        public void onDiscoveryStarted(String s) {
            Toast.makeText(mContext, "Discovery Started", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDiscoveryStopped(String s) {
            Toast.makeText(mContext, "Discovery Stopped", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            Log.i(TAG, "Service Discovered " + nsdServiceInfo.getServiceName());
            Toast.makeText(mContext, "Discovery Found " + nsdServiceInfo.getServiceName(), Toast.LENGTH_SHORT).show();

            if (!nsdServiceInfo.getServiceType().equals(SERVICE_TYPE)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(TAG, "Unknown Service Type: " + nsdServiceInfo.getServiceType());
            } else if (nsdServiceInfo.getServiceName().equals(mServiceName)) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Log.d(TAG, "Same machine: " + mServiceName);
                myResolveListener = new MyResolveListener();
                mNsdManager.resolveService(nsdServiceInfo, myResolveListener);
            } else if (nsdServiceInfo.getServiceName().contains("NsdChat")) {
                myResolveListener = new MyResolveListener();
                mNsdManager.resolveService(nsdServiceInfo, myResolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {

        }
    }

    private class MyResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
            Log.i(TAG, "Service Resolution Failed");
        }

        @Override
        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
            Log.i(TAG, "Service Resolved");
            mServiceInfo = nsdServiceInfo;
            if (nsdServiceInfo.getServiceName().equals(mServiceName)) {
                Log.d(TAG, "Same IP.");
                Toast.makeText(mContext, "LocalHost : " + nsdServiceInfo.getPort(), Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(mContext, "Service Resolved : " + nsdServiceInfo.getHost() + " : " + nsdServiceInfo.getPort(), Toast.LENGTH_SHORT).show();
            mServiceInfo = nsdServiceInfo;
            int port = mServiceInfo.getPort();
            InetAddress host = mServiceInfo.getHost();
            Log.i(TAG, host + " " + port);
        }
    }
}