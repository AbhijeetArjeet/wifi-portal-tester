package com.wifiportaltester.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * DnsVpnService
 *
 * A "no-root DNS changer" style VpnService, using the same split-tunnel
 * technique as open-source apps like DNS66 / Cloudflare 1.1.1.1:
 *
 *  - We do NOT route all traffic (0.0.0.0/0) through the tunnel.
 *  - We only add a route for a private fake "DNS server" address, and tell
 *    Android to use that address as the system DNS resolver.
 *  - Android then sends ONLY DNS queries into our tun interface.
 *  - We parse the raw IPv4/UDP packet, forward the DNS query to the real
 *    upstream DNS server (e.g. 1.1.1.1) over a *protected* socket (so it
 *    doesn't loop back into our own VPN), and write the reply back into
 *    the tun interface as a properly checksummed IPv4/UDP packet.
 *  - All other app traffic (HTTP, TCP, etc.) is completely untouched and
 *    goes over the normal network route, so browsing speed is unaffected.
 *
 * This requires no root. It DOES show Android's persistent VPN key icon
 * and a foreground notification while active, which is expected/required
 * behavior for any VpnService-based app.
 */
class DnsVpnService : VpnService() {

    companion object {
        private const val TAG = "DnsVpnService"
        const val CHANNEL_ID = "dns_vpn_channel"
        const val NOTIF_ID = 42001

        const val EXTRA_DNS_HOST = "dns_host"
        const val EXTRA_DNS_LABEL = "dns_label"

        const val ACTION_STOP = "com.wifiportaltester.app.STOP_DNS_VPN"

        private const val TUN_ADDRESS = "10.111.222.1"
        private const val UPSTREAM_DNS_PORT = 53

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var activeDnsLabel: String = ""
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private var upstreamHost: String = "1.1.1.1"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        val host = intent?.getStringExtra(EXTRA_DNS_HOST) ?: "1.1.1.1"
        val label = intent?.getStringExtra(EXTRA_DNS_LABEL) ?: host
        upstreamHost = host
        activeDnsLabel = label

        startForeground(NOTIF_ID, buildNotification(label))
        startVpn(host)
        return START_STICKY
    }

    private fun buildNotification(label: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "DNS Override",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "Shows when app-wide custom DNS is active"
                nm.createNotificationChannel(channel)
            }
        }

        val stopIntent = Intent(this, DnsVpnService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = Notification.Builder(this).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
            setContentTitle("Custom DNS active: $label")
            setContentText("All app DNS lookups are routed through $label")
            setSmallIcon(android.R.drawable.ic_menu_compass)
            setOngoing(true)
            addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disable", stopPending)
        }
        return builder.build()
    }

    private fun startVpn(dnsHost: String) {
        if (running.get()) {
            // Already running — just restart with the new upstream server.
            stopVpnInternal()
        }

        try {
            val builder = Builder()
                .setSession("WiFi Portal Tester DNS")
                .addAddress(TUN_ADDRESS, 32)
                .addDnsServer(TUN_ADDRESS)
                // Only this single /32 address is routed into the tunnel.
                // Every other destination continues to use the normal
                // network path, so only DNS traffic is intercepted.
                .addRoute(TUN_ADDRESS, 32)
                .setMtu(1500)
                .setBlocking(true)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface (permission not granted?)")
                stopSelf()
                return
            }

            running.set(true)
            isRunning = true

            workerThread = thread(start = true, name = "DnsVpnWorker") {
                runPacketLoop(dnsHost)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startVpn failed", e)
            stopSelf()
        }
    }

    private fun runPacketLoop(dnsHost: String) {
        val pfd = vpnInterface ?: return
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32767)

        while (running.get()) {
            try {
                val length = input.read(buffer)
                if (length <= 0) continue

                val packet = buffer.copyOf(length)
                if (!isIPv4Udp53(packet)) continue

                handleDnsPacket(packet, dnsHost, output)
            } catch (e: IOException) {
                if (running.get()) Log.w(TAG, "Packet loop IO error: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Packet loop error: ${e.message}")
            }
        }
    }

    /** Quick check: IPv4 header + protocol UDP + destination port 53. */
    private fun isIPv4Udp53(packet: ByteArray): Boolean {
        if (packet.isEmpty()) return false
        val version = (packet[0].toInt() shr 4) and 0xF
        if (version != 4) return false
        val ihl = (packet[0].toInt() and 0xF) * 4
        if (packet.size < ihl + 8) return false
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false // UDP
        val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        return destPort == UPSTREAM_DNS_PORT
    }

    private fun handleDnsPacket(packet: ByteArray, primaryDnsHost: String, output: FileOutputStream) {
        val ihl = (packet[0].toInt() and 0xF) * 4
        val udpStart = ihl
        val udpPayloadStart = udpStart + 8
        if (packet.size <= udpPayloadStart) return

        val srcIp = packet.copyOfRange(12, 16)
        val srcPort = ((packet[udpStart].toInt() and 0xFF) shl 8) or (packet[udpStart + 1].toInt() and 0xFF)
        val dnsQuery = packet.copyOfRange(udpPayloadStart, packet.size)

        val targetHosts = listOf(primaryDnsHost, "1.1.1.1", "8.8.8.8", "9.9.9.9", "1.0.0.1").distinct()

        for (host in targetHosts) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                protect(socket)

                val outPacket = DatagramPacket(dnsQuery, dnsQuery.size, InetSocketAddress(host, UPSTREAM_DNS_PORT))
                socket.soTimeout = 2500
                socket.send(outPacket)

                val replyBuf = ByteArray(4096)
                val replyPacket = DatagramPacket(replyBuf, replyBuf.size)
                socket.receive(replyPacket)

                val dnsResponse = replyBuf.copyOf(replyPacket.length)
                val reply = buildIPv4UdpPacket(
                    srcIp = TUN_ADDRESS_BYTES,
                    dstIp = srcIp,
                    srcPort = UPSTREAM_DNS_PORT,
                    dstPort = srcPort,
                    payload = dnsResponse
                )
                output.write(reply)
                return // Successfully forwarded and replied
            } catch (e: Exception) {
                // If primary host fails, loop will try next fallback host
            } finally {
                socket?.close()
            }
        }
    }

    private val TUN_ADDRESS_BYTES: ByteArray by lazy {
        TUN_ADDRESS.split(".").map { it.toInt().toByte() }.toByteArray()
    }

    /** Builds a raw IPv4 packet carrying a UDP payload, with correct header checksums. */
    private fun buildIPv4UdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val buf = ByteBuffer.allocate(totalLength)

        // IPv4 header
        buf.put(0x45.toByte())      // version(4) + IHL(5)
        buf.put(0x00.toByte())      // DSCP/ECN
        buf.putShort(totalLength.toShort())
        buf.putShort(0)             // identification
        buf.putShort(0x4000.toShort()) // flags: don't fragment
        buf.put(64.toByte())        // TTL
        buf.put(17.toByte())        // protocol: UDP
        buf.putShort(0)             // header checksum placeholder
        buf.put(srcIp)
        buf.put(dstIp)

        // UDP header
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort(udpLength.toShort())
        buf.putShort(0)             // UDP checksum (0 = not computed, valid for IPv4)
        buf.put(payload)

        val bytes = buf.array()

        // Compute and patch IPv4 header checksum (bytes 10-11)
        val checksum = ipChecksum(bytes, 0, 20)
        bytes[10] = ((checksum.toInt() shr 8) and 0xFF).toByte()
        bytes[11] = (checksum.toInt() and 0xFF).toByte()

        return bytes
    }

    private fun ipChecksum(data: ByteArray, offset: Int, length: Int): Short {
        var sum = 0L
        var i = offset
        while (i < offset + length - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (length % 2 == 1) {
            sum += (data[offset + length - 1].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv().toShort()
    }

    private fun stopVpnInternal() {
        running.set(false)
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Error closing tun: ${e.message}")
        }
        vpnInterface = null
        workerThread?.interrupt()
        workerThread = null
    }

    private fun stopVpn() {
        stopVpnInternal()
        isRunning = false
        activeDnsLabel = ""
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpnInternal()
        isRunning = false
        activeDnsLabel = ""
        super.onDestroy()
    }

    override fun onRevoke() {
        // User revoked VPN permission from system settings.
        stopVpn()
        super.onRevoke()
    }
}
