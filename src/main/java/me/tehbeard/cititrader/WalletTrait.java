package me.tehbeard.cititrader;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.util.DataKey;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.permissions.Permissible;

/**
 * Wallet for traders Wallets can be of four types - PRIVATE :: Amount held
 * inside Wallet structure - OWNER :: Use Owner's wallet - BANK :: Use a bank
 * account owner owns. (Owner of NPC must be owner of account) - ADMIN ::
 * Infinite wallet
 *
 * @author James
 *
 */
public class WalletTrait extends Trait {

    public WalletTrait() {
        super("wallet");
    }
    WalletType type = WalletType.PRIVATE;
    double amount = 0;
    String account = "";

    @Override
    public void load(DataKey key) throws NPCLoadException {
        type = WalletType.valueOf(key.getString("type"));
        amount = key.getDouble("amount");
        account = key.getString("account");
    }

    @Override
    public void save(DataKey key) {
        key.setString("type", type.toString());
        key.setDouble("amount", amount);
        key.setString("account", account);
    }

    public enum WalletType {

        PRIVATE,
        OWNER,
        BANK,
        ADMIN,
        TOWN_BANK;

        public boolean hasPermission(Permissible p) {
            return p.hasPermission("traders.wallet." + this.toString().toLowerCase());
        }
    }

    /**
     * Add to wallet
     *
     * @param amount
     * @return
     */
    public boolean deposit(double amount) {
        if (amount <= 0) {
            return false;
        }
        switch (type) {
            case PRIVATE:
                this.amount += amount;
                return true;
            case OWNER:
                return CitiTrader.economy.depositPlayer(npc.getTrait(Owner.class).getOwner(), amount).transactionSuccess();
            case BANK:
                return CitiTrader.economy.isBankOwner(account, npc.getTrait(Owner.class).getOwner()).transactionSuccess() ? CitiTrader.economy.bankDeposit(account, amount).transactionSuccess() : false;
            case ADMIN:
                return true;
            case TOWN_BANK: {
                if (CitiTrader.isTowny) {
                    if (isMayorOrAssistant()) {
                        EconomyResponse resp = CitiTrader.economy.depositPlayer(account, amount);
                        return resp.transactionSuccess();
                    }
                }
            }
        }

        return false;
    }

    /**
     * remove from wallet
     *
     * @param amount
     * @return
     */
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            return false;
        }

        switch (type) {
            case PRIVATE:
                if (amount > this.amount) {
                    return false;
                }
                this.amount -= amount;
                return true;
            case OWNER:
                return CitiTrader.economy.withdrawPlayer(npc.getTrait(Owner.class).getOwner(), amount).transactionSuccess();
            case BANK:
                return CitiTrader.economy.isBankOwner(account, npc.getTrait(Owner.class).getOwner()).transactionSuccess() ? CitiTrader.economy.bankWithdraw(account, amount).transactionSuccess() : false;
            case ADMIN:
                return true;
            case TOWN_BANK: {
                if (CitiTrader.isTowny) {
                    if (isMayorOrAssistant()) {
                        EconomyResponse resp = CitiTrader.economy.withdrawPlayer(account, amount);
                        return resp.transactionSuccess();
                    }

                }
            }
        }
        return false;
    }

    /**
     * Do they have this much money
     *
     * @param amount
     * @return
     */
    public boolean has(double amount) {
        if (amount <= 0) {
            return false;
        }
        switch (type) {
            case PRIVATE:
                return this.amount >= amount;
            case OWNER:
                return CitiTrader.economy.has(npc.getTrait(Owner.class).getOwner(), amount);
            case BANK:
                return CitiTrader.economy.isBankOwner(account, npc.getTrait(Owner.class).getOwner()).transactionSuccess() ? CitiTrader.economy.bankHas(account, amount).transactionSuccess() : false;
            case ADMIN:
                return true;
            case TOWN_BANK: {
                if (CitiTrader.isTowny) {
                    if (isMayorOrAssistant()) {
                        return CitiTrader.economy.has(account, amount);
                    }
                }
            }
        }
        return false;
    }

    public final WalletType getType() {
        return type;
    }

    public final void setType(WalletType type) {
        this.type = type;
    }

    public final String getAccount() {
        return account;
    }

    public final void setAccount(String account) {
        this.account = account;
    }

    public final double getAmount() {
        switch (type) {
            case PRIVATE:
                return this.amount;
            case OWNER:
                return CitiTrader.economy.getBalance(npc.getTrait(Owner.class).getOwner());
            case BANK:
                return CitiTrader.economy.isBankOwner(account, npc.getTrait(Owner.class).getOwner()).transactionSuccess() ? CitiTrader.economy.bankBalance(account).amount : 0.0D;
            case ADMIN:
                return 0.0D;
            case TOWN_BANK:
                if (CitiTrader.isTowny) {
                    if (isMayorOrAssistant()) {
                        //EconomyResponse resp = CitiTrader.economy.getBalance(account);
                        //System.out.println(account + " " + resp.balance + " " + resp.type);
                        return CitiTrader.economy.getBalance(account);
                    }
                }
                        System.out.println("No Mayor");
                return 0.0;
        }
        throw new IllegalStateException("NO VALID WALLET TYPE SELECTED");

    }

    public boolean isMayorOrAssistant() {
        if (!CitiTrader.isTowny) {
            return false;
        }

        com.palmergames.bukkit.towny.object.Resident resident;


        try {
            resident = com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getResident(npc.getTrait(Owner.class).getOwner());
        } catch (Exception ex) {
            Logger.getLogger(CitiTrader.class
                    .getName()).log(Level.SEVERE, null, ex);

            return false;
        }

        if (resident.hasTown()) {
            try {
                com.palmergames.bukkit.towny.object.Town town = resident.getTown();
                if (resident.isMayor() || town.getAssistants().contains(resident)) {
                    return true;
                }
            } catch (Exception ex) {
                Logger.getLogger(WalletTrait.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }

        return false;
    }
}
