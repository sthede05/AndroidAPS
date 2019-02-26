package info.nightscout.androidaps.plugins.PumpOmnipod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import info.nightscout.androidaps.logging.L;

public class OmnipyNetworkDiscovery {

    private Logger _log;
    private String _lastKnownAddress;
    Context _context;
    private Boolean _discovering = false;

    public OmnipyNetworkDiscovery(Context context)
    {
        _log = LoggerFactory.getLogger(L.PUMP);
        _context = context;
        _lastKnownAddress = null;
    }

    public String GetLastKnownAddress()
    {
        return _lastKnownAddress;
    }

    public void ClearKnownAddress()
    {
        _lastKnownAddress = null;
    }

    public void SetAddress(String address) {
        _lastKnownAddress = address;
        _discovering = false;
//        Intent intent = new Intent(_context, OmnipodFragment.class);
//        intent.getExtras().putString("omnipy_address", address);
//        _context.startActivity(intent);
//        ((Activity) _context).finish();
    }

    public void RunDiscovery()
    {
        if (_discovering)
            return;

        _discovering = true;
        Timer timer = new Timer("OmnipyNetworkDiscovery", true);
        timer.schedule(new OmnipyNetworkDiscoveryTask(null, _context, this) , 200);
    }
}

class OmnipyNetworkDiscoveryTask extends TimerTask {

    private OmnipyUDPBroadcastTask _broadcastTask;
    private Context _context;
    private OmnipyNetworkDiscovery _mainDiscovery;

    public OmnipyNetworkDiscoveryTask(OmnipyUDPBroadcastTask broadcastTask, Context context,
                                      OmnipyNetworkDiscovery mainDiscovery)
    {
        _broadcastTask = broadcastTask;
        _context = context;
        _mainDiscovery = mainDiscovery;
    }

    @Override
    public void run() {
        if (_broadcastTask == null)
        {
            _broadcastTask = new OmnipyUDPBroadcastTask(_context);
            _broadcastTask.execute();
            this.reschedule(10000);
        }
        else
        {
            if (_broadcastTask.getStatus() == AsyncTask.Status.FINISHED)
            {
                String address = null;
                try {
                    address = _broadcastTask.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                finally {
                    _broadcastTask = null;
                }

                if (address != null)
                {
                    _mainDiscovery.SetAddress(address);
                }
                else
                {
                    this.reschedule(60000);
                }
            }
            else
            {
                this.reschedule(5000);
            }
        }
    }

    private void reschedule(long delay)
    {
        Timer timer = new Timer("OmnipyNetworkDiscovery", true);
        timer.schedule(new OmnipyNetworkDiscoveryTask(_broadcastTask, _context, _mainDiscovery) , delay);
    }

}

class OmnipyUDPBroadcastTask extends AsyncTask<Void, Void, String> {

    Context _context;

    public OmnipyUDPBroadcastTask(Context context)
    {
        super();
        _context = context;
    }

    @Override
    protected String doInBackground(Void... voids) {
        String address = null;
        try
        {
            byte[] receive = new byte[1024];
            DatagramSocket listenSocket = new DatagramSocket(6665);
            DatagramPacket listenPacket = new DatagramPacket(receive, receive.length);
            listenSocket.setSoTimeout(5000);

            DatagramSocket sendSocket = new DatagramSocket();
            sendSocket.setSoTimeout(3000);
            sendSocket.setBroadcast(true);
            byte[] data = "Oh dear.".getBytes(StandardCharsets.US_ASCII);

            WifiManager wifi = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
            assert wifi != null;
            DhcpInfo dhcp = wifi.getDhcpInfo();

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
            InetAddress broadcastAddress = InetAddress.getByAddress(quads);
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, broadcastAddress,
                    6664);

            sendSocket.send(sendPacket);
            listenSocket.receive(listenPacket);

            byte[] received = listenPacket.getData();
            byte[] compare = "wut".getBytes(StandardCharsets.US_ASCII);
            if (received.length == 3 && received[0] == compare[0] &&
                received[1] == compare[1] && received[2] == compare[2])
            {
                address = listenPacket.getAddress().getHostAddress();
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }
}
