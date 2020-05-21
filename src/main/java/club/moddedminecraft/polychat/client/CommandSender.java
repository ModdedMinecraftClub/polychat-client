package club.moddedminecraft.polychat.client;

import club.moddedminecraft.polychat.networking.io.CommandMessage;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandSender implements ICommandSender {

    private final CommandMessage commandMessage;
    private final ArrayList<String> output = new ArrayList<>();
    private final String color;
    private boolean parseSuccess;

    public CommandSender(CommandMessage commandMessage, String color) {
        this.commandMessage = commandMessage;
        this.color = color;
        parseSuccess = false;
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
        if (!parseSuccess) {
            return;
        }

        StringBuilder commandOutput = new StringBuilder();
        for (String output : this.output) {
            commandOutput.append(output).append("\n");
        }
        sendOutputMessage("/" + getCommand(), commandOutput.toString());
    }

    private void sendOutputMessage(String title, String description) {
        String serverID = commandMessage.getServerID();
        String channel = commandMessage.getChannel();
        CommandOutputMessage message = new CommandOutputMessage(serverID, title, description, channel, color);
        ModClass.sendMessage(message);
    }

    public int calculateParameters(String command) {
        Pattern pattern = Pattern.compile("(\\$\\d+)");
        Matcher matcher = pattern.matcher(command);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public String getCommand() {
        String name = commandMessage.getName();
        String command = commandMessage.getCommand();
        ArrayList<String> args = commandMessage.getArgs();

        // Replaces default command with override if exists
        String override_lookup = "override_command_" + name;
        String override = ModClass.properties.getProperty(override_lookup, "");
        if (!override.isEmpty()) {
            command = override;
        }

        int commandArgs = calculateParameters(command);
        if (args.size() < commandArgs) {
            sendOutputMessage("Error parsing command", "Expected at least " + commandArgs + " parameters, received " + args.size());
            return null;
        }

        // get the last instance of every unique $(number)
        // ie. /ranks set $1 $2 $1 $3 returns $2 $1 $3
        Pattern pattern = Pattern.compile("(\\$\\d+)(?!.*\\1)");
        Matcher matcher = pattern.matcher(command);

        while (matcher.find()) {
            for (int i = 0; i <= matcher.groupCount(); i++) {
                String toBeReplaced = matcher.group(i);
                String replaceWith;
                int argNum = Integer.parseInt(toBeReplaced.substring(1));
                replaceWith = args.get(argNum - 1);
                command = command.replace(toBeReplaced, replaceWith);
            }
        }

        command = command.replace("$args", String.join(" ", args));

        parseSuccess = true;
        return command;
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
