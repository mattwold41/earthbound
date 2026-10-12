package com.earthbound;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EarthBankCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!sender.hasPermission("earthbound.bank.admin")) {
            sender.sendMessage(
                    ChatColor.RED
                    + "You do not have permission."
            );
            return true;
        }

        if (args.length == 0) {
            showBalances(sender);
            return true;
        }

        if (args.length == 1) {

            String account =
                    args[0].toLowerCase(Locale.ROOT);

            if (account.equals("treasury")
                    || account.equals("balances")) {
                showBalances(sender);
                return true;
            }

            if (account.equals("store")) {
                sender.sendMessage(
                        ChatColor.GOLD
                        + "General Store balance: "
                        + ChatColor.GREEN
                        + format(EarthEconomy.getStoreBalance())
                );
                return true;
            }

            EarthEconomy.Treasury treasury =
                    findTreasury(account);

            if (treasury != null) {
                sender.sendMessage(
                        ChatColor.GOLD
                        + treasury.name()
                        + " Treasury: "
                        + ChatColor.GREEN
                        + format(
                                EarthEconomy.getTreasuryBalance(
                                        treasury
                                )
                        )
                );
                return true;
            }
        }

        if (args.length >= 4
                && args[0].equalsIgnoreCase("pay")) {

            EarthEconomy.Treasury treasury =
                    findTreasury(args[1]);

            if (treasury == null) {
                sender.sendMessage(
                        ChatColor.RED
                        + "Unknown treasury. Use "
                        + "anacortes, washington, or usa."
                );
                return true;
            }

            Player recipient =
                    org.bukkit.Bukkit.getPlayerExact(
                            args[2]
                    );

            if (recipient == null) {
                sender.sendMessage(
                        ChatColor.RED
                        + "Player must be online."
                );
                return true;
            }

            double amount;

            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(
                        ChatColor.RED
                        + "Invalid amount."
                );
                return true;
            }

            if (!Double.isFinite(amount)
                    || amount <= 0.0
                    || Math.round(amount * 100.0) <= 0) {
                sender.sendMessage(
                        ChatColor.RED
                        + "Amount must be positive."
                );
                return true;
            }

            if (args.length < 5) {
                sender.sendMessage(
                        ChatColor.RED
                        + "A payment reason is required."
                );
                sender.sendMessage(
                        ChatColor.GRAY
                        + "Example: /earthbank pay anacortes "
                        + "Steve 500 bridge_construction"
                );
                return true;
            }

            StringBuilder reason = new StringBuilder();

            for (int i = 4; i < args.length; i++) {
                if (i > 4) {
                    reason.append(' ');
                }
                reason.append(args[i]);
            }

            boolean success =
                    EarthEconomy.treasuryPaysPlayer(
                            treasury,
                            recipient,
                            amount,
                            reason.toString()
                    );

            if (!success) {
                sender.sendMessage(
                        ChatColor.RED
                        + "Payment failed. Check treasury funds."
                );
                return true;
            }

            sender.sendMessage(
                    ChatColor.GREEN
                    + "Paid "
                    + format(amount)
                    + " to "
                    + recipient.getName()
                    + " from "
                    + treasury.name()
                    + " treasury."
            );

            recipient.sendMessage(
                    ChatColor.GREEN
                    + "You received "
                    + format(amount)
                    + " for "
                    + reason
            );

            return true;
        }

        showHelp(sender);
        return true;
    }

    private void showBalances(CommandSender sender) {

        sender.sendMessage(
                ChatColor.GOLD
                + "===== EarthBound Government ====="
        );

        for (EarthEconomy.Treasury treasury :
                EarthEconomy.Treasury.values()) {

            sender.sendMessage(
                    ChatColor.YELLOW
                    + treasury.name()
                    + ": "
                    + ChatColor.GREEN
                    + format(
                            EarthEconomy.getTreasuryBalance(
                                    treasury
                            )
                    )
            );
        }

        sender.sendMessage(
                ChatColor.YELLOW
                + "General Store: "
                + ChatColor.GREEN
                + format(EarthEconomy.getStoreBalance())
        );
    }

    private EarthEconomy.Treasury findTreasury(
            String value
    ) {

        switch (value.toLowerCase(Locale.ROOT)) {
            case "anacortes":
            case "city":
                return EarthEconomy.Treasury.ANACORTES;

            case "washington":
            case "state":
                return EarthEconomy.Treasury.WASHINGTON;

            case "usa":
            case "federal":
                return EarthEconomy.Treasury.USA;

            default:
                return null;
        }
    }

    private void showHelp(CommandSender sender) {

        sender.sendMessage(
                ChatColor.GOLD
                + "===== EarthBound Banking ====="
        );

        sender.sendMessage(
                ChatColor.YELLOW
                + "/earthbank treasury"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                + "/earthbank store"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                + "/earthbank anacortes"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                + "/earthbank washington"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                + "/earthbank usa"
        );

        sender.sendMessage(
                ChatColor.YELLOW
                + "/earthbank pay <treasury> "
                + "<player> <amount> <reason>"
        );
    }

    private String format(double amount) {

        return String.format(
                Locale.US,
                "$%,.2f",
                amount
        );
    }
}
