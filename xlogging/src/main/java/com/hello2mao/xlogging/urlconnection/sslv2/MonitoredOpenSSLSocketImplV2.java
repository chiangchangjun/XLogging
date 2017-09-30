package com.hello2mao.xlogging.urlconnection.sslv2;


import android.util.Log;

import com.android.org.conscrypt.OpenSSLSocketImpl;
import com.android.org.conscrypt.SSLParametersImpl;
import com.hello2mao.xlogging.Constant;
import com.hello2mao.xlogging.urlconnection.MonitoredSocketInterface;
import com.hello2mao.xlogging.urlconnection.NetworkTransactionState;
import com.hello2mao.xlogging.urlconnection.UrlBuilder;
import com.hello2mao.xlogging.urlconnection.iov1.HttpRequestParsingOutputStreamV1;
import com.hello2mao.xlogging.urlconnection.iov1.HttpResponseParsingInputStreamV1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;

public class MonitoredOpenSSLSocketImplV2 extends OpenSSLSocketImpl implements MonitoredSocketInterface {

    private int sslHandshakeTime;
    private final Queue<NetworkTransactionState> queue;
    private HttpResponseParsingInputStreamV1 inputStream;
    private HttpRequestParsingOutputStreamV1 outputStream;
    
    public MonitoredOpenSSLSocketImplV2(SSLParametersImpl sslParametersImpl) throws IOException {
        super(sslParametersImpl);
        this.queue = new LinkedList<>();
    }

    public MonitoredOpenSSLSocketImplV2(String host, int port, SSLParametersImpl sslParametersImpl)
            throws IOException{
        super(host, port, sslParametersImpl);
        this.queue = new LinkedList<>();
    }

    public MonitoredOpenSSLSocketImplV2(InetAddress inetAddress, int port,
                                        SSLParametersImpl sslParametersImpl) throws IOException{
        super(inetAddress, port, sslParametersImpl);
        this.queue = new LinkedList<>();
    }

    public MonitoredOpenSSLSocketImplV2(String host, int port, InetAddress clientAddress, int clientPort,
                                        SSLParametersImpl sslParametersImpl) throws IOException{
        super(host, port, clientAddress, clientPort, sslParametersImpl);
        this.queue = new LinkedList<>();
    }

    public MonitoredOpenSSLSocketImplV2(InetAddress inetAddress, int port, InetAddress clientAddress,
                                        int clientPort, SSLParametersImpl sslParametersImpl) throws IOException{
        super(inetAddress, port, clientAddress, clientPort, sslParametersImpl);
        this.queue = new LinkedList<>();
    }

    @Override
    public void startHandshake() throws IOException {
        try {
            long currentTimeMillis = System.currentTimeMillis();
            super.startHandshake();
            this.sslHandshakeTime = (int)(System.currentTimeMillis() - currentTimeMillis);
            Log.d(Constant.TAG, "sslHandshakeTime V2:" + sslHandshakeTime);
        }
        catch (IOException ex) {
            // TODO
            throw ex;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
//        if (inputStream != null) {
            // FIXME: ty 中注释掉这句话了，why？
//            inputStream.notifySocketClosing();
//        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream inputStream = super.getInputStream();
        if (inputStream == null) {
            return null;
        }
        Log.d(Constant.TAG, "get InputStream in open SSL socket impl");
        return this.inputStream = new HttpResponseParsingInputStreamV1(this, inputStream);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        OutputStream outputStream = super.getOutputStream();
        if (outputStream == null) {
            return null;
        }
        Log.d(Constant.TAG, "get OutputStream in open SSL socket impl");
        return this.outputStream = new HttpRequestParsingOutputStreamV1(this, outputStream);
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        super.setSoTimeout(timeout);
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return super.getSoTimeout();
    }

    private NetworkTransactionState createNetworkTransactionState(boolean b) {
        NetworkTransactionState networkTransactionState = new NetworkTransactionState();
        int port = this.getPort();
        networkTransactionState.setPort(port);
        if (port == 443) {
            networkTransactionState.setScheme(UrlBuilder.Scheme.HTTPS);
        } else {
            networkTransactionState.setScheme(UrlBuilder.Scheme.HTTP);
        }
//        networkTransactionState.setCarrier(Agent.getActiveNetworkCarrier());
        networkTransactionState.setSslHandShakeTime(sslHandshakeTime);
        return networkTransactionState;
    }

    @Override
    public NetworkTransactionState createNetworkTransactionState() {
        return createNetworkTransactionState(true);
    }

    @Override
    public NetworkTransactionState dequeueNetworkTransactionState() {
        synchronized (queue) {
            Log.d(Constant.TAG, "SSLSocketV2 start dequeuetransaction len:" + queue.size());
            return queue.poll();
        }
    }

    @Override
    public void enqueueNetworkTransactionState(NetworkTransactionState networkTransactionState) {
        synchronized (queue) {
            queue.add(networkTransactionState);
            Log.d(Constant.TAG, "SSLSocketV2 enqueuetransaction len:" + queue.size());
        }
    }
}