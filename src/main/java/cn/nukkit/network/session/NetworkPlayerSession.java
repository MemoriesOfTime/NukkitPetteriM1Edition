package cn.nukkit.network.session;

import cn.nukkit.Player;
import cn.nukkit.network.CompressionProvider;
import cn.nukkit.network.protocol.DataPacket;

import javax.crypto.SecretKey;

public interface NetworkPlayerSession {

    void enableEncryption(SecretKey secretKey);

    void sendPacket(DataPacket packet);
    void sendImmediatePacket(DataPacket packet, Runnable callback);

    void flush();

    void disconnect(String reason);

    Player getPlayer();

    void setCompression(CompressionProvider compression);
    CompressionProvider getCompression();
}