package carpet.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.MinecraftServer.prometheusExtension;

public class CommandPrometheus extends CommandCarpetBase {
    @Override
    public String getName() {
        return "prometheus";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/prometheus <start|stop>\n" +
                "Or /prometheus port <port> (number has to be between 2 and 65535)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(this.getRequiredPermissionLevel(), this.getName());
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (command_enabled("prometheusIntegration", sender)) {
            if (checkPermission(server, sender)) {
                if (args.length == 1) {
                    if (args[0].equals("start")) {
                        if (prometheusExtension.getServer() == null) {
                            prometheusExtension.onServerRun(server);
                            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Prometheus started"));
                        }
                        else {
                            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Prometheus was running"));
                        }
                    }
                    else if (args[0].equals("stop")) {
                        prometheusExtension.onServerStop();
                        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Prometheus stopped"));
                    }
                    else {
                        throw new WrongUsageException(getUsage(sender));
                    }
                }
                else if (args.length == 2) {
                    if (args[0].equals("port")) {
                        try {
                            int port = Integer.parseInt(args[1]);
                            if (port > 1 && port < 65535) {
                                if (port != 123) {
                                    prometheusExtension.changePorts(port);
                                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Port successfully changed to " + port));
                                }
                                else {
                                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "123 can't be a port, idk why, might be more of this"));
                                }
                            }
                        } catch (NumberFormatException e) {
                            throw new SyntaxErrorException("That is not a valid number");
                        }
                    }
                }
                else {
                    throw new WrongUsageException(getUsage(sender));
                }

            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            options.add("start");
            options.add("stop");
            options.add("port");
        }

        return options;
    }
}
