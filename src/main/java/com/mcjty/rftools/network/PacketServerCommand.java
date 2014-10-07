package com.mcjty.rftools.network;

import com.mcjty.varia.Coordinate;
import com.sun.org.apache.xpath.internal.Arg;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import scala.actors.threadpool.Arrays;

import java.util.HashMap;
import java.util.Map;

public class PacketServerCommand implements IMessage, IMessageHandler<PacketServerCommand, IMessage> {
    private int x;
    private int y;
    private int z;
    private String command;
    private Map<String,Argument> args;

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        byte[] dst = new byte[buf.readInt()];
        buf.readBytes(dst);
        command = new String(dst);
        int size = buf.readInt();
        if (size == 0) {
            args = null;
        } else {
            args = new HashMap<String,Argument>(size);
            for (int i = 0 ; i < size ; i++) {
                dst = new byte[buf.readInt()];
                buf.readBytes(dst);
                String key = new String(dst);
                ArgumentType type = ArgumentType.getType(buf.readByte());
                switch (type) {
                    case TYPE_STRING:
                        dst = new byte[buf.readInt()];
                        buf.readBytes(dst);
                        args.put(key, new Argument(key, new String(dst)));
                        break;
                    case TYPE_INTEGER:
                        args.put(key, new Argument(key, buf.readInt()));
                        break;
                    case TYPE_COORDINATE:
                        args.put(key, new Argument(key, new Coordinate(buf.readInt(), buf.readInt(), buf.readInt())));
                        break;
                    case TYPE_BOOLEAN:
                        args.put(key, new Argument(key, buf.readByte() == 1));
                        break;
                }
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(command.length());
        buf.writeBytes(command.getBytes());
        if (args == null) {
            buf.writeInt(0);
        } else {
            buf.writeInt(args.size());
            for (Argument arg : args.values()) {
                String key = arg.getName();
                buf.writeInt(key.length());
                buf.writeBytes(key.getBytes());
                buf.writeByte(arg.getType().getIndex());
                switch (arg.getType()) {
                    case TYPE_STRING:
                        String s = arg.getString();
                        buf.writeInt(s == null ? 0 : s.length());
                        if (s != null) {
                            buf.writeBytes(s.getBytes());
                        }
                        break;
                    case TYPE_INTEGER:
                        buf.writeInt(arg.getInteger());
                        break;
                    case TYPE_COORDINATE:
                        Coordinate c = arg.getCoordinate();
                        buf.writeInt(c.getX());
                        buf.writeInt(c.getY());
                        buf.writeInt(c.getZ());
                        break;
                    case TYPE_BOOLEAN:
                        buf.writeByte(arg.getBoolean() ? 1 : 0);
                        break;
                }
            }
        }
    }

    public PacketServerCommand() {
    }

    public PacketServerCommand(int x, int y, int z, String command, Argument... arguments) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.command = command;
        if (arguments == null) {
            this.args = null;
        } else {
            args = new HashMap<String, Argument>(arguments.length);
            for (Argument arg : arguments) {
                args.put(arg.getName(), arg);
            }
        }
    }

    @Override
    public IMessage onMessage(PacketServerCommand message, MessageContext ctx) {
        TileEntity te = ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.x, message.y, message.z);
        if(!(te instanceof CommandHandler)) {
            // @Todo better logging
            System.out.println("createStartScanPacket: TileEntity is not a CommandHandler!");
            return null;
        }
        CommandHandler commandHandler = (CommandHandler) te;
        if (!commandHandler.execute(message.command, message.args)) {
            System.out.println("Command "+message.command+" was not handled!");
        }
        return null;
    }

}