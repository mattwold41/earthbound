package com.earthbound;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EarthBankCommand implements CommandExecutor {

    private static final double MAX_PAYMENT =
            1000000000.0;

    private static final int MAX_REASON_LENGTH =
            512;


    /*
     * ============================================================
     * GOVERNMENT BANKING COMMAND
     * ============================================================
     */

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        /*
         * ADMINISTRATOR PERMISSION
         */

        if (!sender.hasPermission(
                "earthbound.bank.admin"
        )) {

            sender.sendMessage(
                    ChatColor.RED
                    + "You do not have permission."
            );

            return true;
        }


        /*
         * SERVER THREAD VALIDATION
         */

        if (!Bukkit.isPrimaryThread()) {

            sender.sendMessage(
                    ChatColor.RED
                    + "Banking commands are unavailable."
            );

            return true;
        }


        /*
         * ECONOMY AVAILABILITY
         */

        if (!EarthEconomy.isReady()) {

            sender.sendMessage(
                    ChatColor.RED
                    + "EarthBound financial storage "
                    + "is temporarily unavailable."
            );

            sender.sendMessage(
                    ChatColor.YELLOW
                    + "Check the server console "
                    + "before attempting transactions."
            );

            return true;
        }


        /*
         * DISPLAY ALL GOVERNMENT BALANCES
         */

        if (args.length == 0) {

            showBalances(sender);

            return true;
        }


        /*
         * INDIVIDUAL ACCOUNT LOOKUP
         */

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
                        + format(
                                EarthEconomy.getStoreBalance()
                        )
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


        /*
         * GOVERNMENT PAYMENTS
         */

        if (args[0].equalsIgnoreCase("pay")) {

            processPayment(sender, args);

            return true;
        }

        showHelp(sender);

        return true;
    }


    /*
     * ============================================================
     * PROCESS GOVERNMENT PAYMENT
     * ============================================================
     */

    private void processPayment(
            CommandSender sender,
            String[] args
    ) {

        /*
         * COMMAND FORMAT
         */

        if (args.length < 5) {

            sender.sendMessage(
                    ChatColor.RED
                    + "A treasury, player, amount "
                    + "and payment reason are required."
            );

            sender.sendMessage(
                    ChatColor.YELLOW
                    + "/earthbank pay <treasury> "
                    + "<player> <amount> <reason>"
            );

            sender.sendMessage(
                    ChatColor.GRAY
                    + "Example: /earthbank pay anacortes "
                    + "Steve 500 bridge_construction"
            );

            return;
        }


        /*
         * FIND GOVERNMENT TREASURY
         */

        EarthEconomy.Treasury treasury =
                findTreasury(args[1]);

        if (treasury == null) {

            sender.sendMessage(
                    ChatColor.RED
                    + "Unknown treasury. Use "
                    + "anacortes, washington, or usa."
            );

            return;
        }


        /*
         * FIND PAYMENT RECIPIENT
         */

        Player recipient =
                Bukkit.getPlayerExact(args[2]);

        if (recipient == null
                || !recipient.isOnline()) {

            sender.sendMessage(
                    ChatColor.RED
                    + "Player must be online."
            );

            return;
        }


        /*
         * VALIDATE PAYMENT AMOUNT
         */

        double amount =
                parsePayment(args[3]);

        if (amount <= 0.0) {

            sender.sendMessage(
                    ChatColor.RED
                    + "Enter a valid payment between "
                    + "$0.01 and $1,000,000,000 "
                    + "using no more than two decimal places."
            );

            return;
        }


        /*
         * BUILD PAYMENT REASON
         */

        StringBuilder reasonBuilder =
                new StringBuilder();

        for (int i = 4; i < args.length; i++) {

            if (i > 4) {

                reasonBuilder.append(' ');
            }

            reasonBuilder.append(args[i]);
        }

        String reason =
                reasonBuilder.toString().trim();

        if (!validReason(reason)) {

            sender.sendMessage(
                    ChatColor.RED
                    + "Enter a valid payment reason."
            );

            return;
        }


        /*
         * RECORD ADMINISTRATOR IDENTITY
         */

        String administrator;

        if (sender instanceof Player) {

            Player administratorPlayer =
                    (Player) sender;

            administrator =
                    administratorPlayer.getUniqueId()
                    .toString();

        } else {

            administrator =
                    sender.getName();
        }

        String auditReason =
                "[Authorized by "
                + administrator
                + "] "
                + reason;

        if (auditReason.length()
                > MAX_REASON_LENGTH) {

            sender.sendMessage(
                    ChatColor.RED
                    + "Payment reason is too long."
            );

            return;
        }


        /*
         * CHECK AVAILABLE GOVERNMENT FUNDS
         */

        double available =
                EarthEconomy.getTreasuryBalance(
                        treasury
                );

        if (available < amount) {

            sender.sendMessage(
                    ChatColor.RED
                    + "The treasury does not have "
                    + "enough money."
            );

            sender.sendMessage(
                    ChatColor.YELLOW
                    + "Available balance: "
                    + format(available)
            );

            return;
        }


        /*
         * COMMIT SECURE GOVERNMENT PAYMENT
         */

        boolean success =
                EarthEconomy.treasuryPaysPlayer(
                        treasury,
                        recipient,
                        amount,
                        auditReason
                );

        if (!success) {

            if (!EarthEconomy.isReady()) {

                sender.sendMessage(
                        ChatColor.RED
                        + "Financial storage failure. "
                        + "Administrator review required."
                );

                sender.sendMessage(
                        ChatColor.RED
                        + "Do not repeat this payment "
                        + "until the economy is checked."
                );

            } else {

                sender.sendMessage(
                        ChatColor.RED
                        + "Government payment could "
                        + "not be completed."
                );
            }

            return;
        }


        /*
         * ADMINISTRATOR AUDIT LOG
         */

        Bukkit.getLogger().info(
                "[EarthBound] Government payment: "
                + format(amount)
                + " | Treasury: "
                + treasury.name()
                + " | Recipient: "
                + recipient.getName()
                + " ("
                + recipient.getUniqueId()
                + ")"
                + " | Authorized by: "
                + administrator
                + " | Reason: "
                + reason
        );


        /*
         * CONFIRM SUCCESSFUL PAYMENT
         */

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

        sender.sendMessage(
                ChatColor.YELLOW
                + "Remaining treasury balance: "
                + ChatColor.GREEN
                + format(
                        EarthEconomy.getTreasuryBalance(
                                treasury
                        )
                )
        );

        recipient.sendMessage(
                ChatColor.GREEN
                + "You received "
                + format(amount)
                + " for "
                + reason
                + "."
        );
    }


    /*
     * ============================================================
     * VALIDATE PAYMENT AMOUNT
     * ============================================================
     */

    private double parsePayment(String input) {

        if (input == null
                || input.length() > 32
                || !input.matches(
                        "[0-9]+(\\.[0-9]{1,2})?"
                )) {

            return -1.0;
        }

        try {

            BigDecimal amount =
                    new BigDecimal(input);

            amount = amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );

            if (amount.compareTo(
                    new BigDecimal("0.01")
            ) < 0) {

                return -1.0;
            }

            if (amount.compareTo(
                    BigDecimal.valueOf(MAX_PAYMENT)
            ) > 0) {

                return -1.0;
            }

            double result =
                    amount.doubleValue();

            if (!Double.isFinite(result)) {

                return -1.0;
            }

            return result;

        } catch (RuntimeException exception) {

            return -1.0;
        }
    }


    /*
     * ============================================================
     * VALIDATE PAYMENT REASON
     * ============================================================
     */

    private boolean validReason(String reason) {

        if (reason == null
                || reason.isBlank()
                || reason.length()
                        > MAX_REASON_LENGTH) {

            return false;
        }

        for (int i = 0;
                i < reason.length();
                i++) {

            char character =
                    reason.charAt(i);

            if (Character.isISOControl(character)
                    || character == '\u00A7') {

                return false;
            }
        }

        return true;
    }


    /*
     * ============================================================
     * DISPLAY GOVERNMENT BALANCES
     * ============================================================
     */

    private void showBalances(
            CommandSender sender
    ) {

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
                + "Total government funds: "
                + ChatColor.GREEN
                + format(
                        EarthEconomy.getTreasuryBalance()
                )
        );

        sender.sendMessage(
                ChatColor.GOLD
                + "===== General Store ====="
        );

        sender.sendMessage(
                ChatColor.YELLOW
                + "Store reserve: "
                + ChatColor.GREEN
                + format(
                        EarthEconomy.getStoreBalance()
                )
        );
    }


    /*
     * ============================================================
     * FIND GOVERNMENT TREASURY
     * ============================================================
     */

    private EarthEconomy.Treasury findTreasury(
            String value
    ) {

        if (value == null) {

            return null;
        }

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


    /*
     * ============================================================
     * BANKING HELP
     * ============================================================
     */

    private void showHelp(
            CommandSender sender
    ) {

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


    /*
     * ============================================================
     * FORMAT MONEY
     * ============================================================
     */

    private String format(double amount) {

        return String.format(
                Locale.US,
                "$%,.2f",
                amount
        );
    }

}
