package net.md_5.bungee.protocol;

import com.google.common.base.Preconditions;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Kick;
import net.md_5.bungee.protocol.packet.Login;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.protocol.packet.LoginSuccess;
import net.md_5.bungee.protocol.packet.PingPacket;
import net.md_5.bungee.protocol.packet.PlayerListHeaderFooter;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PluginMessage;
import net.md_5.bungee.protocol.packet.Respawn;
import net.md_5.bungee.protocol.packet.ScoreboardDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardObjective;
import net.md_5.bungee.protocol.packet.ScoreboardScore;
import net.md_5.bungee.protocol.packet.SetCompression;
import net.md_5.bungee.protocol.packet.StatusRequest;
import net.md_5.bungee.protocol.packet.StatusResponse;
import net.md_5.bungee.protocol.packet.TabCompleteRequest;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;
import net.md_5.bungee.protocol.packet.Team;
import net.md_5.bungee.protocol.packet.Title;

public enum Protocol
{

    // Undef
    HANDSHAKE
            {

                {
                    TO_SERVER.registerPacket( 0x00, Handshake.class );
                }
            },
    // 0
    GAME
            {

                {
                    TO_CLIENT.registerPacket( 0x00, 0x20, KeepAlive.class );
                    TO_CLIENT.registerPacket( 0x01, 0x24, Login.class );
                    TO_CLIENT.registerPacket( 0x02, 0x0F, Chat.class );
                    TO_CLIENT.registerPacket( 0x07, 0x34, Respawn.class );
                    TO_CLIENT.registerPacket( 0x38, 0x2E, PlayerListItem.class ); // PlayerInfo
                    TO_CLIENT.registerPacket( 0x3A, 0x0E, TabCompleteResponse.class );
                    TO_CLIENT.registerPacket( 0x3B, 0x40, ScoreboardObjective.class );
                    TO_CLIENT.registerPacket( 0x3C, 0x43, ScoreboardScore.class );
                    TO_CLIENT.registerPacket( 0x3D, 0x39, ScoreboardDisplay.class );
                    TO_CLIENT.registerPacket( 0x3E, 0x42, Team.class );
                    TO_CLIENT.registerPacket( 0x3F, 0x18, PluginMessage.class );
                    TO_CLIENT.registerPacket( 0x40, 0x1A, Kick.class );
                    TO_CLIENT.registerPacket( 0x45, 0x46, Title.class );
                    TO_CLIENT.registerPacket( 0x46, 0x1E, SetCompression.class );
                    TO_CLIENT.registerPacket( 0x47, 0x49, PlayerListHeaderFooter.class );

                    TO_SERVER.registerPacket( 0x00, 0x0B, KeepAlive.class );
                    TO_SERVER.registerPacket( 0x01, 0x02, Chat.class );
                    TO_SERVER.registerPacket( 0x14, 0x01, TabCompleteRequest.class );
                    TO_SERVER.registerPacket( 0x15, 0x04, ClientSettings.class );
                    TO_SERVER.registerPacket( 0x17, 0x09, PluginMessage.class );
                }
            },
    // 1
    STATUS
            {

                {
                    TO_CLIENT.registerPacket( 0x00, StatusResponse.class );
                    TO_CLIENT.registerPacket( 0x01, PingPacket.class );

                    TO_SERVER.registerPacket( 0x00, StatusRequest.class );
                    TO_SERVER.registerPacket( 0x01, PingPacket.class );
                }
            },
    //2
    LOGIN
            {

                {
                    TO_CLIENT.registerPacket( 0x00, Kick.class );
                    TO_CLIENT.registerPacket( 0x01, EncryptionRequest.class );
                    TO_CLIENT.registerPacket( 0x02, LoginSuccess.class );
                    TO_CLIENT.registerPacket( 0x03, SetCompression.class );

                    TO_SERVER.registerPacket( 0x00, LoginRequest.class );
                    TO_SERVER.registerPacket( 0x01, EncryptionResponse.class );
                }
            };
    /*========================================================================*/
    public static final int MAX_PACKET_ID = 0xFF;
    public static List<Integer> supportedVersions = Arrays.asList(
            ProtocolConstants.MINECRAFT_1_8,
            ProtocolConstants.MINECRAFT_SNAPSHOT
    );
    /*========================================================================*/
    public final DirectionData TO_SERVER = new DirectionData( ProtocolConstants.Direction.TO_SERVER );
    public final DirectionData TO_CLIENT = new DirectionData( ProtocolConstants.Direction.TO_CLIENT );

    @RequiredArgsConstructor
    public class DirectionData
    {

        @Getter
        private final ProtocolConstants.Direction direction;
        private final TObjectIntMap<Class<? extends DefinedPacket>> packetMap = new TObjectIntHashMap<>( MAX_PACKET_ID );
        private final Class<? extends DefinedPacket>[] packetClasses = new Class[ MAX_PACKET_ID ];
        private final Constructor<? extends DefinedPacket>[] packetConstructors = new Constructor[ MAX_PACKET_ID ];

        private final TIntObjectMap<TIntIntMap> packetRemap = new TIntObjectHashMap<>();
        private final TIntObjectMap<TIntIntMap> packetRemapInv = new TIntObjectHashMap<>();

        {
            packetRemap.put( ProtocolConstants.MINECRAFT_1_8, new TIntIntHashMap() );
            packetRemapInv.put( ProtocolConstants.MINECRAFT_1_8, new TIntIntHashMap() );
            packetRemap.put( ProtocolConstants.MINECRAFT_SNAPSHOT, new TIntIntHashMap() );
            packetRemapInv.put( ProtocolConstants.MINECRAFT_SNAPSHOT, new TIntIntHashMap() );
        }

        public boolean hasPacket(int id, int protocol)
        {
            TIntIntMap remap = packetRemap.get( protocol );
            if ( remap == null )
            {
                // Unsupported versions will go down this path, normally
                // caused by status pings by newer/older clients
                return id < MAX_PACKET_ID && packetConstructors[id] != null;
            }
            return id < MAX_PACKET_ID && remap.containsKey( id );
        }

        public final DefinedPacket createPacket(int id, int protocol)
        {
            TIntIntMap remap = packetRemap.get( protocol );
            if ( remap != null )
            {
                if ( !remap.containsKey( id ) )
                {
                    throw new BadPacketException( "No packet with id " + id );
                }
                id = remap.get( id );
            }
            if ( id > MAX_PACKET_ID )
            {
                throw new BadPacketException( "Packet with id " + id + " outside of range " );
            }

            Constructor<? extends DefinedPacket> constructor = packetConstructors[id];
            try
            {
                return ( constructor == null ) ? null : constructor.newInstance();
            } catch ( ReflectiveOperationException ex )
            {
                throw new BadPacketException( "Could not construct packet with id " + id, ex );
            }
        }

        protected final void registerPacket(int id, Class<? extends DefinedPacket> packetClass)
        {
            registerPacket( id, id, packetClass );
        }

        protected final void registerPacket(int id, int newId, Class<? extends DefinedPacket> packetClass)
        {
            try
            {
                packetConstructors[id] = packetClass.getDeclaredConstructor();
            } catch ( NoSuchMethodException ex )
            {
                throw new BadPacketException( "No NoArgsConstructor for packet class " + packetClass );
            }
            packetClasses[id] = packetClass;
            packetMap.put( packetClass, id );

            packetRemap.get( ProtocolConstants.MINECRAFT_1_8 ).put( id, id );
            packetRemapInv.get( ProtocolConstants.MINECRAFT_1_8 ).put( id, id );
            packetRemap.get( ProtocolConstants.MINECRAFT_SNAPSHOT ).put( newId, id );
            packetRemapInv.get( ProtocolConstants.MINECRAFT_SNAPSHOT ).put( id, newId );
        }

        protected final void unregisterPacket(int id)
        {
            packetMap.remove( packetClasses[id] );
            packetClasses[id] = null;
            packetConstructors[id] = null;
        }

        final int getId(Class<? extends DefinedPacket> packet, int protocol)
        {
            Preconditions.checkArgument( packetMap.containsKey( packet ), "Cannot get ID for packet " + packet );

            int id = packetMap.get( packet );
            TIntIntMap remap = packetRemapInv.get( protocol );
            if ( remap != null )
            {
                return remap.get( id );
            }
            return id;
        }
    }
}
