package me.cubesicle;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public final class Pinger {
    private String address = "localhost";

    private int port = 25565;

    private int timeout = 2000;

    private int pingVersion = -1;

    private int protocolVersion = -1;

    private String gameVersion;

    private String motd;

    private int playersOnline = -1;

    private int maxPlayers = -1;

    public Pinger(String address, int port) {
        setAddress(address);
        setPort(port);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return this.address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return this.timeout;
    }

    private void setPingVersion(int pingVersion) {
        this.pingVersion = pingVersion;
    }

    public int getPingVersion() {
        return this.pingVersion;
    }

    private void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    private void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public String getGameVersion() {
        return this.gameVersion;
    }

    private void setMotd(String motd) {
        this.motd = motd;
    }

    public String getMotd() {
        return this.motd;
    }

    private void setPlayersOnline(int playersOnline) {
        this.playersOnline = playersOnline;
    }

    public int getPlayersOnline() {
        return this.playersOnline;
    }

    private void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean fetchData() {
        try {
            Socket socket = new Socket();
            socket.setSoTimeout(this.timeout);
            socket.connect(
                    new InetSocketAddress(getAddress(), getPort()),
                    getTimeout());
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,
                    StandardCharsets.UTF_16BE);

            // Send server list ping request, in 1.4-1.5 format
            // See https://wiki.vg/Server_List_Ping#1.4_to_1.5
            dataOutputStream.write(0xFE);
            dataOutputStream.write(0x01);

            // Then, read the ping response (a kick packet)
            // Beta 1.8 - 1.3 servers should respond with this format: https://wiki.vg/Server_List_Ping#Beta_1.8_to_1.3
            // 1.4+ servers should respond with this format: https://wiki.vg/Server_List_Ping#1.4_to_1.5
            // which uses the same response format as: https://wiki.vg/Server_List_Ping#1.6

            // Read packet ID field (1 byte), should be 0xFF (kick packet ID)
            int packetId = inputStream.read();
            if (packetId == -1) {
                try {
                    socket.close();
                } catch (IOException iOException) {
                }
                socket = null;
                return false;
            }
            if (packetId != 0xFF) {
                try {
                    socket.close();
                } catch (IOException iOException) {
                }
                socket = null;
                return false;
            }

            // Read string length field (2 bytes)
            int length = inputStreamReader.read();
            if (length == -1) {
                try {
                    socket.close();
                } catch (IOException iOException) {
                }
                socket = null;
                return false;
            }
            if (length == 0) {
                try {
                    socket.close();
                } catch (IOException iOException) {
                }
                socket = null;
                return false;
            }

            // Read string (length bytes)
            char[] chars = new char[length];
            if (inputStreamReader.read(chars, 0, length) != length) {
                try {
                    socket.close();
                } catch (IOException iOException) {
                }
                socket = null;
                return false;
            }
            String string = new String(chars);

            // Read the fields of the string
            if (string.startsWith("§")) {
                // If the string starts with '§', the server is probably running 1.4+
                // See https://wiki.vg/Server_List_Ping#1.4_to_1.5
                // and https://wiki.vg/Server_List_Ping#1.6

                // In this format, fields are delimited by '\0' characters
                String[] data = string.split("\0");
                setPingVersion(Integer.parseInt(data[0].substring(1)));
                setProtocolVersion(Integer.parseInt(data[1]));
                setGameVersion(data[2]);
                setMotd(data[3]);
                setPlayersOnline(Integer.parseInt(data[4]));
                setMaxPlayers(Integer.parseInt(data[5]));
            } else {
                // If the string doesn't start with '§', the server is probably running Beta 1.8 - 1.3
                // See https://wiki.vg/Server_List_Ping#Beta_1.8_to_1.3

                // In this format, fields are delimited by '§' characters
                String[] data = string.split("§");
                setMotd(data[0]);
                setPlayersOnline(Integer.parseInt(data[1]));
                setMaxPlayers(Integer.parseInt(data[2]));
            }
            dataOutputStream.close();
            outputStream.close();
            inputStreamReader.close();
            inputStream.close();
            socket.close();
        } catch (SocketException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        }
        return true;
    }
}
