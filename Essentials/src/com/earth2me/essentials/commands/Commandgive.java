package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import static com.earth2me.essentials.I18n._;
import com.earth2me.essentials.MetaItemStack;
import com.earth2me.essentials.User;
import com.earth2me.essentials.craftbukkit.InventoryWorkaround;
import com.earth2me.essentials.utils.NumberUtil;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;


public class Commandgive extends EssentialsCommand
{
	public Commandgive()
	{
		super("give");
	}

	@Override
	public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception
	{
		if (args.length < 2)
		{
			throw new NotEnoughArgumentsException();
		}

		ItemStack stack = ess.getItemDb().get(args[1]);

		final String itemname = stack.getType().toString().toLowerCase(Locale.ENGLISH).replace("_", "");
		if (sender.isPlayer()
			&& (ess.getSettings().permissionBasedItemSpawn()
				? (!ess.getUser(sender.getPlayer()).isAuthorized("essentials.itemspawn.item-all")
				   && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.itemspawn.item-" + itemname)
				   && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.itemspawn.item-" + stack.getTypeId()))
				: (!ess.getUser(sender.getPlayer()).isAuthorized("essentials.itemspawn.exempt")
				   && !ess.getUser(sender.getPlayer()).canSpawnItem(stack.getTypeId()))))
		{
			throw new Exception(_("cantSpawnItem", itemname));
		}

		final User giveTo = getPlayer(server, sender, args, 0);

		try
		{
			if (args.length > 3 && NumberUtil.isInt(args[2]) && NumberUtil.isInt(args[3]))
			{
				stack.setAmount(Integer.parseInt(args[2]));
				stack.setDurability(Short.parseShort(args[3]));
			}
			else if (args.length > 2 && Integer.parseInt(args[2]) > 0)
			{
				stack.setAmount(Integer.parseInt(args[2]));
			}
			else if (ess.getSettings().getDefaultStackSize() > 0)
			{
				stack.setAmount(ess.getSettings().getDefaultStackSize());
			}
			else if (ess.getSettings().getOversizedStackSize() > 0 && giveTo.isAuthorized("essentials.oversizedstacks"))
			{
				stack.setAmount(ess.getSettings().getOversizedStackSize());
			}
		}
		catch (NumberFormatException e)
		{
			throw new NotEnoughArgumentsException();
		}

		if (args.length > 3)
		{
			MetaItemStack metaStack = new MetaItemStack(stack);
			boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments();
			if (allowUnsafe && sender.isPlayer() && !ess.getUser(sender.getPlayer()).isAuthorized("essentials.enchantments.allowunsafe"))
			{
				allowUnsafe = false;
			}

			metaStack.parseStringMeta(sender, allowUnsafe, args, NumberUtil.isInt(args[3]) ? 4 : 3, ess);

			stack = metaStack.getItemStack();
		}

		if (stack.getType() == Material.AIR)
		{
			throw new Exception(_("cantSpawnItem", "Air"));
		}

		final String itemName = stack.getType().toString().toLowerCase(Locale.ENGLISH).replace('_', ' ');
		sender.sendMessage(_("giveSpawn", stack.getAmount(), itemName, giveTo.getDisplayName()));

		Map<Integer, ItemStack> leftovers;

		if (giveTo.isAuthorized("essentials.oversizedstacks"))
		{
			leftovers = InventoryWorkaround.addOversizedItems(giveTo.getInventory(), ess.getSettings().getOversizedStackSize(), stack);
		}
		else
		{
			leftovers = InventoryWorkaround.addItems(giveTo.getInventory(), stack);
		}

		for (ItemStack item : leftovers.values())
		{
			sender.sendMessage(_("giveSpawnFailure", item.getAmount(), itemName, giveTo.getDisplayName()));
		}

		giveTo.updateInventory();
	}
}
