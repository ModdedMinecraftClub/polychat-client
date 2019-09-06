package club.moddedminecraft.polychat.client;

import club.moddedminecraft.polychat.networking.io.CommandOutputMessage;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class CommandSender implements ICommandSender {

    private String command;
    private String channel;

    public CommandSender(String command, String channel) {
        this.command = command;
        this.channel = channel;
    }

    @Override
    public String getName() {
        return "PolyChat";
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    public void sendMessage(ITextComponent component) {
        String commandText = "";
        if (this.command != null){
            commandText = "/" + this.command;
        }
        CommandOutputMessage message = new CommandOutputMessage(commandText, component.getUnformattedText(), channel);
        ModClass.sendMessage(message);
        this.command = null; // Only show command once
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public BlockPos getPosition() {
        return BlockPos.ORIGIN;
    }

    @Override
    public Vec3d getPositionVector() {
        return Vec3d.ZERO;
    }

    @Override
    public World getEntityWorld() {
        return ModClass.server.getWorld(0);
    }

    @Nullable
    @Override
    public Entity getCommandSenderEntity() {
        return null;
    }

    @Override
    public void setCommandStat(CommandResultStats.Type type, int amount) {
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return ModClass.server;
    }

}
