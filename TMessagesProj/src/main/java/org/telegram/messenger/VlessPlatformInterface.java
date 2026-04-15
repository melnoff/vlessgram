package org.telegram.messenger;

import com.hiddify.core.libbox.ConnectionOwner;
import com.hiddify.core.libbox.InterfaceUpdateListener;
import com.hiddify.core.libbox.LocalDNSTransport;
import com.hiddify.core.libbox.NetworkInterfaceIterator;
import com.hiddify.core.libbox.Notification;
import com.hiddify.core.libbox.PlatformInterface;
import com.hiddify.core.libbox.StringIterator;
import com.hiddify.core.libbox.TunOptions;
import com.hiddify.core.libbox.WIFIState;

/**
 * Minimal PlatformInterface stub for sing-box.
 * Only needed for SOCKS5 inbound — TUN methods throw, others return defaults.
 */
public class VlessPlatformInterface implements PlatformInterface {

    @Override
    public boolean usePlatformAutoDetectInterfaceControl() {
        return false;
    }

    @Override
    public void autoDetectInterfaceControl(int fd) throws Exception {
    }

    @Override
    public int openTun(TunOptions options) throws Exception {
        throw new Exception("TUN not supported");
    }

    @Override
    public boolean useProcFS() {
        return false;
    }

    @Override
    public ConnectionOwner findConnectionOwner(int ipProtocol, String sourceAddress, int sourcePort,
                                                String destinationAddress, int destinationPort) throws Exception {
        throw new Exception("not supported");
    }

    @Override
    public void startDefaultInterfaceMonitor(InterfaceUpdateListener listener) throws Exception {
    }

    @Override
    public void closeDefaultInterfaceMonitor(InterfaceUpdateListener listener) throws Exception {
    }

    @Override
    public NetworkInterfaceIterator getInterfaces() throws Exception {
        return new NetworkInterfaceIterator() {
            @Override
            public boolean hasNext() { return false; }
            @Override
            public com.hiddify.core.libbox.NetworkInterface next() { return null; }
        };
    }

    @Override
    public boolean underNetworkExtension() {
        return false;
    }

    @Override
    public boolean includeAllNetworks() {
        return false;
    }

    @Override
    public void clearDNSCache() {
    }

    @Override
    public WIFIState readWIFIState() {
        return null;
    }

    @Override
    public StringIterator systemCertificates() {
        return new StringIterator() {
            @Override
            public boolean hasNext() { return false; }
            @Override
            public String next() { return null; }
            @Override
            public int len() { return 0; }
        };
    }

    @Override
    public LocalDNSTransport localDNSTransport() {
        return null;
    }

    @Override
    public void sendNotification(Notification notification) throws Exception {
    }
}
