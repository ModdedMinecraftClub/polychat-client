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
import java.util.ArrayList;

public class CommandSender implements ICommandSender {

    private final String serverID;
    private final String command;
    private final String channel;
    private ArrayList<String> output = new ArrayList<>();

    public CommandSender(String serverID, String command, String channel) {
        this.serverID = serverID;
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
        String text = component.getFormattedText();
        this.output.add(text.replaceAll("§.", ""));
    }

    public void sendOutput() {
        StringBuilder commandOutput = new StringBuilder();
        for (String output : this.output) {
            commandOutput.append(output).append("\n");
        }
        CommandOutputMessage message = new CommandOutputMessage(serverID, "/" + this.command, commandOutput.toString(), channel);
        ModClass.sendMessage(message);
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
